package es.wara.ecos.backend.application.usecase.song;

import es.wara.ecos.backend.domain.model.Song;
import es.wara.ecos.backend.domain.port.SongRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Caso de uso para obtener una canción por su identificador.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Service
public class GetSongByIdUseCase {

    private final SongRepository songRepository;

    public GetSongByIdUseCase(SongRepository songRepository) {
        this.songRepository = songRepository;
    }

    /**
     * Devuelve la canción con el identificador dado.
     *
     * @param id identificador de la canción
     * @return la canción encontrada
     * @throws IllegalArgumentException si la canción no existe
     */
    public Song execute(UUID id) {
        return songRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Canción no encontrada: " + id));
    }

}
