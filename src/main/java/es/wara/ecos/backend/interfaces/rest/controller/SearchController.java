package es.wara.ecos.backend.interfaces.rest.controller;

import es.wara.ecos.backend.application.usecase.search.SearchParams;
import es.wara.ecos.backend.application.usecase.search.SearchSongsUseCase;
import es.wara.ecos.backend.interfaces.rest.dto.SongResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * Controlador REST para la búsqueda multidimensional de canciones (5 cuadrantes de ECOS).
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@RestController
@RequestMapping("/v1/search")
public class SearchController {

    private final SearchSongsUseCase searchSongsUseCase;
    /**
     * Inyecta el caso de uso que ejecuta la búsqueda multidimensional.
     *
     * @param searchSongsUseCase caso de uso de búsqueda de canciones por 5 cuadrantes
     */
    public SearchController(SearchSongsUseCase searchSongsUseCase) {
        this.searchSongsUseCase = searchSongsUseCase;
    }

    /**
     * Búsqueda multidimensional de canciones según el modelo de 5 cuadrantes de ECOS.
     * <p>
     * Todos los parámetros son opcionales; los activos determinan los cuadrantes activados:
     * <ul>
     *   <li><b>Cuadrante 1 — Texto libre:</b> {@code q}</li>
     *   <li><b>Cuadrante 2 — Estética/época:</b> {@code aesthetic}, {@code decade}</li>
     *   <li><b>Cuadrante 3 — Contexto cinematográfico:</b> {@code movie}</li>
     *   <li><b>Cuadrante 4 — Textura sonora:</b> {@code instrumental}, {@code texture}, {@code mode}</li>
     *   <li><b>Cuadrante 5 — Parámetros de audio:</b> {@code energyMin/Max}, {@code valenceMin/Max}, {@code bpmMin/Max}</li>
     * </ul>
     * </p>
     *
     * @param q            texto libre para búsqueda por título, artista o letra
     * @param aesthetic    estilo o movimiento estético (p.ej. "dark academia", "cottagecore")
     * @param decade       década musical (p.ej. "1980s", "2000s")
     * @param movie        película o serie asociada
     * @param instrumental {@code true} para canciones sin letra
     * @param texture      textura sonora (p.ej. "acoustic", "distorted")
     * @param mode         modo musical: {@code "major"} o {@code "minor"}
     * @param energyMin    energía mínima [0.0 – 1.0]
     * @param energyMax    energía máxima [0.0 – 1.0]
     * @param valenceMin   valencia mínima (positividad) [0.0 – 1.0]
     * @param valenceMax   valencia máxima [0.0 – 1.0]
     * @param bpmMin       BPM mínimo
     * @param bpmMax       BPM máximo
     * @param limit        número máximo de resultados (por defecto 20)
     * @param offset       desplazamiento para paginación (por defecto 0)
     * @return {@code 200 OK} con la lista de canciones que coinciden con los parámetros
     */
    @GetMapping
    public ResponseEntity<List<SongResponse>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String aesthetic,
            @RequestParam(required = false) String decade,
            @RequestParam(required = false) String movie,
            @RequestParam(required = false) Boolean instrumental,
            @RequestParam(required = false) String texture,
            @RequestParam(required = false) String mode,
            @RequestParam(required = false) BigDecimal energyMin,
            @RequestParam(required = false) BigDecimal energyMax,
            @RequestParam(required = false) BigDecimal valenceMin,
            @RequestParam(required = false) BigDecimal valenceMax,
            @RequestParam(required = false) BigDecimal bpmMin,
            @RequestParam(required = false) BigDecimal bpmMax,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        // Empaquetar todos los filtros en un objeto de parámetros para el use case
        SearchParams params = new SearchParams(
                q, aesthetic, decade, movie, instrumental, texture, mode,
                energyMin, energyMax, valenceMin, valenceMax, bpmMin, bpmMax,
                limit, offset
        );

        List<SongResponse> results = searchSongsUseCase.execute(params)
                .stream()
                .map(SongResponse::from)
                .toList();

        return ResponseEntity.ok(results);
    }

}
