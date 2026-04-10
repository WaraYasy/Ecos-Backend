package es.wara.ecos.backend.application.usecase.search;

import es.wara.ecos.backend.domain.model.Song;
import es.wara.ecos.backend.domain.model.SongEmbedding;
import es.wara.ecos.backend.domain.port.MusicProvider;
import es.wara.ecos.backend.domain.port.SongEmbeddingRepository;
import es.wara.ecos.backend.domain.port.SongRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Caso de uso para obtener canciones similares a una dada.
 *
 * <p><b>Estrategia primaria:</b> si la canción tiene embedding en {@code song_embeddings},
 * busca los vecinos más cercanos por similitud coseno (pgvector {@code <=>}).
 * <b>Fallback:</b> si no hay embedding, delega en Last.fm {@code track.getSimilar}.</p>
 *
 * @author Wara Pacheco
 * @version 2.0
 * @since 1.0.0
 */
@Slf4j
@Service
public class GetSimilarSongsUseCase {

    private static final int LIMIT = 8;

    private final SongRepository songRepository;
    private final SongEmbeddingRepository embeddingRepository;
    private final MusicProvider musicProvider;

    public GetSimilarSongsUseCase(SongRepository songRepository,
                                  SongEmbeddingRepository embeddingRepository,
                                  MusicProvider musicProvider) {
        this.songRepository = songRepository;
        this.embeddingRepository = embeddingRepository;
        this.musicProvider = musicProvider;
    }

    /**
     * Devuelve canciones similares a la indicada.
     * Usa embeddings si están disponibles; si no, fallback a Last.fm.
     *
     * @param songId identificador interno de la canción de referencia
     * @return lista de hasta {@value LIMIT} canciones similares
     * @throws IllegalArgumentException si la canción no existe
     */
    public List<Song> execute(UUID songId) {
        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new IllegalArgumentException("Canción no encontrada: " + songId));

        Optional<SongEmbedding> embedding = embeddingRepository.findBySongId(songId);

        if (embedding.isPresent()) {
            log.debug("related [embedding] song={}", songId);
            List<UUID> neighborIds = embeddingRepository.findNearestNeighbors(
                    embedding.get().getEmbedding(), LIMIT, List.of(songId));
            return neighborIds.stream()
                    .map(songRepository::findById)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList();
        }

        log.debug("related [lastfm fallback] song={}", songId);
        String artist = (song.getArtists() != null && !song.getArtists().isEmpty())
                ? song.getArtists().get(0).getName()
                : "";
        return musicProvider.getSimilarSongs(artist, song.getTitle(), LIMIT)
                .stream()
                .filter(s -> s.getTitle() != null)
                .map(s -> s.getId() != null ? s : songRepository.save(s))
                .toList();
    }

}
