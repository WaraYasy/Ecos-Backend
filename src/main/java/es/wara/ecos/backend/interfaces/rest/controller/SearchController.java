package es.wara.ecos.backend.interfaces.rest.controller;

import es.wara.ecos.backend.application.usecase.search.SearchParams;
import es.wara.ecos.backend.application.usecase.search.SearchSongsUseCase;
import es.wara.ecos.backend.config.SearchSuggestionsProperties;
import es.wara.ecos.backend.interfaces.rest.dto.PageResponse;
import es.wara.ecos.backend.interfaces.rest.dto.SearchSuggestionResponse;
import es.wara.ecos.backend.interfaces.rest.dto.SongResponse;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
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
@Validated
@RestController
@RequestMapping("/v1/search")
public class SearchController {

    private final SearchSongsUseCase searchSongsUseCase;
    private final SearchSuggestionsProperties suggestionsProperties;

    public SearchController(SearchSongsUseCase searchSongsUseCase,
                            SearchSuggestionsProperties suggestionsProperties) {
        this.searchSongsUseCase = searchSongsUseCase;
        this.suggestionsProperties = suggestionsProperties;
    }

    /**
     * Devuelve las sugerencias de búsqueda predefinidas.
     * La lista se configura en {@code suggestions.yml} y puede actualizarse
     * sin redesplegar montando un fichero externo en {@code /config/suggestions.yml}.
     */
    @GetMapping("/suggestions")
    public ResponseEntity<List<SearchSuggestionResponse>> suggestions() {
        List<SearchSuggestionResponse> items = suggestionsProperties.getItems()
                .stream()
                .map(SearchSuggestionResponse::from)
                .toList();
        return ResponseEntity.ok(items);
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
     * @param genre        género musical (p.ej. "rock", "jazz", "indie pop")
     * @param decade       década musical en formato "1980s" (p.ej. "1980s", "2000s") — cuadrante seed
     * @param year         año exacto o rango (p.ej. "1985", "1980-1990") — on-demand
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
     * @param pageSize     número de resultados por página (por defecto 20, máximo 50)
     * @param page         número de página 0-indexed (por defecto 0)
     * @return {@code 200 OK} con la página de canciones que coinciden con los parámetros
     */
    @GetMapping
    public ResponseEntity<PageResponse<SongResponse>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String artist,
            @RequestParam(required = false) String aesthetic,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) @Pattern(regexp = "\\d{4}s", message = "La década debe tener el formato '1980s'") String decade,
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String movie,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) Boolean instrumental,
            @RequestParam(required = false) String texture,
            @RequestParam(required = false) String mode,
            @RequestParam(required = false) @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal energyMin,
            @RequestParam(required = false) @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal energyMax,
            @RequestParam(required = false) @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal valenceMin,
            @RequestParam(required = false) @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal valenceMax,
            @RequestParam(required = false) @Min(0) BigDecimal bpmMin,
            @RequestParam(required = false) @Min(0) BigDecimal bpmMax,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "false") boolean enhanced
    ) {
        SearchParams params = new SearchParams(
                q, artist, aesthetic, genre, decade, year, movie, country,
                instrumental, texture, mode,
                energyMin, energyMax, valenceMin, valenceMax, bpmMin, bpmMax,
                pageSize, page, enhanced
        );

        PageResponse<SongResponse> response = PageResponse.from(
                searchSongsUseCase.execute(params).map(SongResponse::from)
        );

        if (response.items().isEmpty()) {
            throw new IllegalArgumentException("No se encontraron canciones para los parámetros indicados");
        }

        return ResponseEntity.ok(response);
    }

}
