package es.wara.ecos.backend.interfaces.rest.controller;

import es.wara.ecos.backend.application.usecase.search.GetSimilarSongsUseCase;
import es.wara.ecos.backend.application.usecase.song.GetSongByIdUseCase;
import es.wara.ecos.backend.domain.model.Song;
import es.wara.ecos.backend.interfaces.rest.dto.SongResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Controlador REST para operaciones sobre canciones individuales.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code GET /v1/songs/{id}} — detalle completo de una canción (SongResponse)</li>
 *   <li>{@code GET /v1/songs/{id}/related} — canciones similares vía Last.fm</li>
 * </ul>
 * Todos los endpoints requieren autenticación JWT.</p>
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@RestController
@RequestMapping("/v1/songs")
public class SongController {

    private final GetSongByIdUseCase getSongByIdUseCase;
    private final GetSimilarSongsUseCase getSimilarSongsUseCase;

    /**
     * Inyecta los casos de uso necesarios para consultar canciones.
     *
     * @param getSongByIdUseCase     consulta de una canción por su ID
     * @param getSimilarSongsUseCase búsqueda de canciones similares a una dada
     */
    public SongController(GetSongByIdUseCase getSongByIdUseCase,
                          GetSimilarSongsUseCase getSimilarSongsUseCase) {
        this.getSongByIdUseCase = getSongByIdUseCase;
        this.getSimilarSongsUseCase = getSimilarSongsUseCase;
    }

    /**
     * Devuelve los detalles completos de una canción por su ID.
     *
     * @param id identificador de la canción
     * @return {@code 200 OK} con la canción, o {@code 404} si no existe
     */
    @GetMapping("/{id}")
    public ResponseEntity<SongResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(SongResponse.from(getSongByIdUseCase.execute(id)));
    }

    /**
     * Devuelve canciones similares a la indicada según el algoritmo de similitud de ECOS.
     *
     * @param id identificador de la canción de referencia
     * @return {@code 200 OK} con la lista de canciones relacionadas
     */
    @GetMapping("/{id}/related")
    public ResponseEntity<List<SongResponse>> getRelated(@PathVariable UUID id) {
        List<SongResponse> related = getSimilarSongsUseCase.execute(id)
                .stream()
                .map(SongResponse::from)
                .toList();
        return ResponseEntity.ok(related);
    }

}
