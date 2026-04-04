package es.wara.ecos.backend.application.usecase.graph;

import es.wara.ecos.backend.domain.model.*;
import es.wara.ecos.backend.domain.port.MusicProvider;
import es.wara.ecos.backend.domain.port.SongEmbeddingRepository;
import es.wara.ecos.backend.domain.port.SongRepository;
import es.wara.ecos.backend.domain.service.SimilarityService;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Expande el grafo añadiendo nuevos nodos a partir de un nodo existente.
 * Recibe los IDs ya presentes en el grafo del cliente para no repetirlos.
 * Devuelve solo los nodos y aristas nuevos a fusionar en el cliente.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Service
public class ExpandGraphUseCase {

    private static final int MAX_NEW_NODES = 5;
    private static final float LAYOUT_RADIUS = 500f;

    private final SongRepository songRepository;
    private final SongEmbeddingRepository embeddingRepository;
    private final SimilarityService similarityService;
    private final MusicProvider musicProvider;

    public ExpandGraphUseCase(SongRepository songRepository,
                              SongEmbeddingRepository embeddingRepository,
                              SimilarityService similarityService,
                              MusicProvider musicProvider) {
        this.songRepository = songRepository;
        this.embeddingRepository = embeddingRepository;
        this.similarityService = similarityService;
        this.musicProvider = musicProvider;
    }

    /**
     * Expande el grafo desde el nodo indicado.
     *
     * @param targetSongId   canción desde la que se expande
     * @param existingIds    IDs ya presentes en el grafo (se excluyen de los resultados)
     * @param maxNewNodes    máximo de nodos nuevos a añadir (tope: {@value MAX_NEW_NODES})
     * @return grafo parcial con los nodos y aristas nuevos
     * @throws IllegalArgumentException si la canción no existe
     */
    public GraphResponse execute(UUID targetSongId, List<UUID> existingIds, int maxNewNodes) {
        int limit = Math.min(maxNewNodes, MAX_NEW_NODES);
        List<UUID> excluded = new ArrayList<>(existingIds);
        if (!excluded.contains(targetSongId)) excluded.add(targetSongId);

        Song target = songRepository.findById(targetSongId)
                .orElseThrow(() -> new IllegalArgumentException("Canción no encontrada: " + targetSongId));

        float[] targetEmbedding = embeddingRepository.findBySongId(targetSongId)
                .map(SongEmbedding::getEmbedding)
                .orElse(null);

        List<UUID> candidateIds = findCandidates(target, targetEmbedding, excluded, limit * 3);

        List<ScoredSong> scored = candidateIds.stream()
                .map(id -> songRepository.findById(id).orElse(null))
                .filter(Objects::nonNull)
                .map(candidate -> {
                    float[] candEmb = embeddingRepository.findBySongId(candidate.getId())
                            .map(SongEmbedding::getEmbedding).orElse(null);
                    float sim = similarityService.compute(target, candidate, targetEmbedding, candEmb);
                    return new ScoredSong(candidate, candEmb, sim);
                })
                .sorted(Comparator.comparingDouble(ScoredSong::similarity).reversed())
                .limit(limit)
                .toList();

        return buildResponse(target, targetSongId, targetEmbedding, scored);
    }

    private List<UUID> findCandidates(Song target, float[] embedding,
                                      List<UUID> excluded, int limit) {
        if (embedding != null) {
            List<UUID> semantic = embeddingRepository.findNearestNeighbors(embedding, limit, excluded);
            if (!semantic.isEmpty()) return semantic;
        }
        String artist = (target.getArtists() != null && !target.getArtists().isEmpty())
                ? target.getArtists().get(0).getName() : "";
        return musicProvider.getSimilarSongs(artist, target.getTitle(), limit)
                .stream()
                .map(Song::getSpotifyId)
                .filter(Objects::nonNull)
                .map(songRepository::findBySpotifyId)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(Song::getId)
                .filter(id -> !excluded.contains(id))
                .toList();
    }

    private GraphResponse buildResponse(Song target, UUID targetSongId,
                                        float[] targetEmbedding, List<ScoredSong> scored) {
        List<GraphNode> nodes = new ArrayList<>();
        List<GraphEdge> edges = new ArrayList<>();

        int total = scored.size();
        for (int i = 0; i < total; i++) {
            ScoredSong ss = scored.get(i);

            float angle    = (float) (2 * Math.PI * i / total);
            float distance = (1f - ss.similarity()) * LAYOUT_RADIUS;
            float x = (float) (Math.cos(angle) * distance);
            float y = (float) (Math.sin(angle) * distance);

            Integer targetYear = target.getAlbum() != null ? target.getAlbum().getYear() : null;
            Integer candYear   = ss.song().getAlbum() != null ? ss.song().getAlbum().getYear() : null;

            GraphEdge.EdgeType edgeType = similarityService.dominantEdgeType(
                    similarityService.audioSimilarity(target.getAudioFeatures(), ss.song().getAudioFeatures()),
                    (targetEmbedding != null && ss.embedding() != null)
                            ? similarityService.semanticSimilarity(targetEmbedding, ss.embedding()) : 0f,
                    similarityService.tagSimilarity(target.getTags(), ss.song().getTags()),
                    similarityService.eraProximity(targetYear, candYear),
                    similarityService.movieOverlap(target.getMovies(), ss.song().getMovies())
            );

            GraphEdge edge = new GraphEdge(targetSongId, ss.song().getId(), ss.similarity(), edgeType);
            edges.add(edge);
            nodes.add(new GraphNode(ss.song(), x, y, ss.similarity(), List.of(edge)));
        }

        return new GraphResponse(targetSongId, nodes, edges);
    }

    private record ScoredSong(Song song, float[] embedding, float similarity) {}

}