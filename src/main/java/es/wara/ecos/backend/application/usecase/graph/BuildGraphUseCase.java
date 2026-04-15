package es.wara.ecos.backend.application.usecase.graph;

import es.wara.ecos.backend.domain.model.*;
import es.wara.ecos.backend.domain.port.MusicProvider;
import es.wara.ecos.backend.domain.port.SongEmbeddingRepository;
import es.wara.ecos.backend.domain.port.SongRepository;
import es.wara.ecos.backend.domain.service.SimilarityService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Construye el grafo neuronal inicial a partir de una canción raíz.
 * <p>
 * Estrategia de candidatos (local-first):
 * <ol>
 *   <li>Vecinos semánticos por pgvector (<=> coseno) si existen embeddings.</li>
 *   <li>Fallback: {@link MusicProvider#getSimilarSongs} → canciones ya en nuestra BD por spotifyId.</li>
 * </ol>
 * Para cada candidato carga sus features completos de BD, calcula similitud multi-factor
 * y selecciona los top-{@value MAX_NODES}. Las posiciones se calculan en layout polar:
 * nodo raíz en (0,0), vecinos en círculo a distancia proporcional a (1 − similitud).
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Service
public class BuildGraphUseCase {

    public static final int MAX_NODES = 8;
    private static final float LAYOUT_RADIUS = 500f;
    /** Multiplicador de candidatos a evaluar antes de filtrar los top-K. */
    private static final int CANDIDATE_MULTIPLIER = 3;

    private final SongRepository songRepository;
    private final SongEmbeddingRepository embeddingRepository;
    private final SimilarityService similarityService;
    private final MusicProvider musicProvider;

    public BuildGraphUseCase(SongRepository songRepository,
                             SongEmbeddingRepository embeddingRepository,
                             SimilarityService similarityService,
                             MusicProvider musicProvider) {
        this.songRepository = songRepository;
        this.embeddingRepository = embeddingRepository;
        this.similarityService = similarityService;
        this.musicProvider = musicProvider;
    }

    /**
     * Genera el grafo desde la canción raíz.
n     * <p>
     * Los candidatos se recuperan en una sola query batch con
     * {@code findAllById} para evitar N+1. Las posiciones se calculan
     * en layout polar: raíz en (0,0), vecinos en círculo a distancia
     * proporcional a {@code (1 − similitud) × LAYOUT_RADIUS}.
     * </p>
     *
     * @param rootSongId identificador de la canción raíz
     * @param maxNodes   número máximo de nodos vecinos (tope: {@value MAX_NODES})
     * @return {@link GraphResponse} con nodos posicionados, aristas tipificadas
     *         y el ID de la canción raíz
     * @throws IllegalArgumentException si la canción raíz no existe en la BD
     */
    public GraphResponse execute(UUID rootSongId, int maxNodes) {
        int limit = Math.min(maxNodes, MAX_NODES);

        Song root = songRepository.findById(rootSongId)
                .orElseThrow(() -> new IllegalArgumentException("Canción no encontrada: " + rootSongId));

        float[] rootEmbedding = embeddingRepository.findBySongId(rootSongId)
                .map(SongEmbedding::getEmbedding)
                .orElse(null);

        List<UUID> candidateIds = findCandidates(root, rootEmbedding, limit * CANDIDATE_MULTIPLIER);

        List<ScoredSong> scored = score(root, rootEmbedding, candidateIds, limit);

        return buildResponse(root, rootSongId, rootEmbedding, scored);
    }

    // --- pasos internos ---

    private List<UUID> findCandidates(Song root, float[] rootEmbedding, int limit) {
        if (rootEmbedding != null) {
            List<UUID> semantic = embeddingRepository.findNearestNeighbors(
                    rootEmbedding, limit, List.of(root.getId()));
            if (!semantic.isEmpty()) return semantic;
        }
        // Fallback: API externa → filtrar a canciones ya persistidas en nuestra BD
        String artist = firstArtistName(root);
        return musicProvider.getSimilarSongs(artist, root.getTitle(), limit)
                .stream()
                .map(Song::getSpotifyId)
                .filter(Objects::nonNull)
                .map(songRepository::findBySpotifyId)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(Song::getId)
                .filter(id -> !id.equals(root.getId()))
                .toList();
    }

    private List<ScoredSong> score(Song root, float[] rootEmbedding,
                                   List<UUID> candidateIds, int limit) {
        Map<UUID, Song> byId = songRepository.findAllById(candidateIds).stream()
                .collect(Collectors.toMap(Song::getId, s -> s));
        return candidateIds.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .map(candidate -> {
                    float[] candEmb = embeddingRepository.findBySongId(candidate.getId())
                            .map(SongEmbedding::getEmbedding).orElse(null);
                    float sim = similarityService.compute(root, candidate, rootEmbedding, candEmb);
                    return new ScoredSong(candidate, candEmb, sim);
                })
                .sorted(Comparator.comparingDouble(ScoredSong::similarity).reversed())
                .limit(limit)
                .toList();
    }

    private GraphResponse buildResponse(Song root, UUID rootSongId,
                                        float[] rootEmbedding, List<ScoredSong> scored) {
        List<GraphNode> nodes = new ArrayList<>();
        List<GraphEdge> edges = new ArrayList<>();

        // Nodo raíz: centro del grafo
        GraphNode rootNode = new GraphNode(root, 0f, 0f, 1f, new ArrayList<>());
        nodes.add(rootNode);

        int total = scored.size();
        for (int i = 0; i < total; i++) {
            ScoredSong ss = scored.get(i);

            // Posición polar: radio ∝ (1 - similitud), ángulo distribuido uniformemente
            float angle    = (float) (2 * Math.PI * i / total);
            float distance = (1f - ss.similarity()) * LAYOUT_RADIUS;
            float x = (float) (Math.cos(angle) * distance);
            float y = (float) (Math.sin(angle) * distance);

            GraphEdge.EdgeType edgeType = resolveEdgeType(root, ss, rootEmbedding);
            GraphEdge edge = new GraphEdge(rootSongId, ss.song().getId(), ss.similarity(), edgeType);

            edges.add(edge);
            nodes.add(new GraphNode(ss.song(), x, y, ss.similarity(), List.of(edge)));
        }

        rootNode.setEdges(edges);
        return new GraphResponse(rootSongId, nodes, edges);
    }

    private GraphEdge.EdgeType resolveEdgeType(Song root, ScoredSong ss, float[] rootEmbedding) {
        Integer rootYear = root.getAlbum() != null ? root.getAlbum().getYear() : null;
        Integer candYear = ss.song().getAlbum() != null ? ss.song().getAlbum().getYear() : null;

        return similarityService.dominantEdgeType(
                similarityService.audioSimilarity(root.getAudioFeatures(), ss.song().getAudioFeatures()),
                (rootEmbedding != null && ss.embedding() != null)
                        ? similarityService.semanticSimilarity(rootEmbedding, ss.embedding()) : 0f,
                similarityService.tagSimilarity(root.getTags(), ss.song().getTags()),
                similarityService.eraProximity(rootYear, candYear),
                similarityService.movieOverlap(root.getMovies(), ss.song().getMovies())
        );
    }

    private String firstArtistName(Song song) {
        return (song.getArtists() != null && !song.getArtists().isEmpty())
                ? song.getArtists().get(0).getName()
                : "";
    }

    private record ScoredSong(Song song, float[] embedding, float similarity) {}

}