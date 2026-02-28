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

    public SearchController(SearchSongsUseCase searchSongsUseCase) {
        this.searchSongsUseCase = searchSongsUseCase;
    }

    /**
     * Búsqueda multidimensional de canciones.
     * Todos los parámetros son opcionales; los activos determinan los cuadrantes de búsqueda.
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
