package es.wara.ecos.backend.application.usecase.search;

import es.wara.ecos.backend.domain.model.Song;
import es.wara.ecos.backend.domain.port.MusicProvider;
import es.wara.ecos.backend.domain.port.SongRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Caso de uso para obtener canciones similares a una dada, usando el proveedor externo.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Service
public class GetSimilarSongsUseCase {

    /** Número máximo de canciones similares a devolver. */
    private static final int LIMIT = 8;

    private final SongRepository songRepository;
    private final MusicProvider musicProvider;

    public GetSimilarSongsUseCase(SongRepository songRepository, MusicProvider musicProvider) {
        this.songRepository = songRepository;
        this.musicProvider = musicProvider;
    }

    /**
     * Devuelve canciones similares a la indicada.
     *
     * @param songId identificador interno de la canción de referencia
     * @return lista de hasta {@value LIMIT} canciones similares
     * @throws IllegalArgumentException si la canción no existe
     */
    public List<Song> execute(Long songId) {
        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new IllegalArgumentException("Canción no encontrada: " + songId));

        String artist = (song.getArtists() != null && !song.getArtists().isEmpty())
                ? song.getArtists().get(0).getName()
                : "";

        return musicProvider.getSimilarSongs(artist, song.getTitle(), LIMIT);
    }

}
