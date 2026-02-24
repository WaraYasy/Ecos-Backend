package es.wara.ecos.backend.application.usecase.search;

import es.wara.ecos.backend.domain.model.Song;
import es.wara.ecos.backend.domain.port.MusicProvider;
import es.wara.ecos.backend.domain.port.SongRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Caso de uso para buscar canciones por texto.
 * <p>
 * Consulta primero la BD local y, si no hay resultados, delega en el proveedor
 * externo y persiste las canciones recuperadas para futuras consultas.
 * </p>
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Service
public class SearchSongsUseCase {

    /** Número máximo de resultados a pedir al proveedor externo. */
    private static final int EXTERNAL_LIMIT = 10;

    private final SongRepository songRepository;
    private final MusicProvider musicProvider;

    public SearchSongsUseCase(SongRepository songRepository, MusicProvider musicProvider) {
        this.songRepository = songRepository;
        this.musicProvider = musicProvider;
    }

    /**
     * Busca canciones que coincidan con el texto indicado.
     *
     * @param query texto de búsqueda (título, artista o álbum)
     * @return lista de canciones encontradas, nunca null
     */
    public List<Song> execute(String query) {
        List<Song> local = songRepository.findByTitleContaining(query);
        if (!local.isEmpty()) {
            return local;
        }

        // Sin resultados locales: buscar externamente y persistir
        List<Song> external = musicProvider.searchSongs(query, EXTERNAL_LIMIT);
        external.forEach(songRepository::save);
        return external;
    }

}
