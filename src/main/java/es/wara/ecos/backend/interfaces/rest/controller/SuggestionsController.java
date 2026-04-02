package es.wara.ecos.backend.interfaces.rest.controller;

import es.wara.ecos.backend.application.service.SuggestionsService;
import es.wara.ecos.backend.interfaces.rest.dto.SuggestionResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoint de sugerencias de autocompletado para los campos de búsqueda.
 *
 * <pre>
 * GET /v1/suggestions?type=movie&q=avengers
 * GET /v1/suggestions?type=aesthetic&q=dark
 * GET /v1/suggestions?type=genre&q=rock
 * </pre>
 *
 * <p>Devuelve hasta 6 sugerencias con {@code value} (lo que se envía al backend)
 * y {@code label} (lo que se muestra al usuario).</p>
 *
 * @author Wara Pacheco
 * @since 1.0.0
 */
@RestController
@RequestMapping("/v1/suggestions")
public class SuggestionsController {

    private final SuggestionsService suggestionsService;

    public SuggestionsController(SuggestionsService suggestionsService) {
        this.suggestionsService = suggestionsService;
    }

    /**
     * Devuelve sugerencias de autocompletado según el tipo de campo.
     *
     * @param type tipo de sugerencia: {@code movie}, {@code aesthetic} o {@code genre}
     * @param q    texto introducido por el usuario (puede estar vacío)
     * @return lista de sugerencias ordenadas por relevancia
     */
    @GetMapping
    public ResponseEntity<List<SuggestionResponse>> suggest(
            @RequestParam String type,
            @RequestParam(required = false, defaultValue = "") String q
    ) {
        List<SuggestionResponse> suggestions = switch (type.toLowerCase()) {
            case "movie"     -> suggestionsService.suggestMovies(q);
            case "aesthetic" -> suggestionsService.suggestAesthetics(q);
            case "genre"     -> suggestionsService.suggestGenres(q);
            case "artist"    -> suggestionsService.suggestArtists(q);
            case "country"   -> suggestionsService.suggestCountries(q);
            default          -> List.of();
        };
        return ResponseEntity.ok(suggestions);
    }
}
