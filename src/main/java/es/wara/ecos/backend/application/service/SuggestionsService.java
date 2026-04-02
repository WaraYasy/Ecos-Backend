package es.wara.ecos.backend.application.service;

import es.wara.ecos.backend.infrastructure.adapter.tmdb.TmdbClient;
import es.wara.ecos.backend.infrastructure.adapter.tmdb.dto.TMDBMovieDTO;
import es.wara.ecos.backend.infrastructure.adapter.tmdb.dto.TMDBSearchResponseDTO;
import es.wara.ecos.backend.infrastructure.seed.SeedTermsProperties;
import es.wara.ecos.backend.interfaces.rest.dto.SuggestionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio de sugerencias de autocompletado para los campos de búsqueda.
 *
 * <p>Tres tipos de sugerencias:
 * <ul>
 *   <li><b>movie</b> → consulta TMDb en tiempo real y devuelve las películas más relevantes</li>
 *   <li><b>aesthetic</b> → filtra la lista curada de estéticas de {@code seed-terms.yml}</li>
 *   <li><b>genre</b> → filtra la lista curada de géneros de {@code seed-terms.yml}</li>
 * </ul>
 * </p>
 *
 * @author Wara Pacheco
 * @since 1.0.0
 */
@Slf4j
@Service
public class SuggestionsService {

    private static final int MAX_SUGGESTIONS = 6;
    private static final String TMDB_IMAGE_BASE = "https://image.tmdb.org/t/p/w92";

    private final TmdbClient tmdbClient;
    private final SeedTermsProperties terms;

    public SuggestionsService(TmdbClient tmdbClient, SeedTermsProperties terms) {
        this.tmdbClient = tmdbClient;
        this.terms = terms;
    }

    /**
     * Sugiere películas buscando en TMDb. Requiere al menos 2 caracteres.
     *
     * @param q texto introducido por el usuario
     * @return lista de sugerencias con título y año; vacía si {@code q} es corto
     */
    public List<SuggestionResponse> suggestMovies(String q) {
        if (q == null || q.strip().length() < 2) return List.of();

        try {
            TMDBSearchResponseDTO results = tmdbClient.searchMovies(q.strip(), 1);
            if (results == null || results.getResults() == null) return List.of();

            return results.getResults().stream()
                    .limit(MAX_SUGGESTIONS)
                    .map(this::toMovieSuggestion)
                    .toList();
        } catch (Exception e) {
            log.warn("Error buscando sugerencias de película para '{}': {}", q, e.getMessage());
            return List.of();
        }
    }

    /**
     * Sugiere estéticas filtrando la lista curada de {@code seed-terms.yml}.
     *
     * @param q texto introducido por el usuario
     * @return lista de estéticas que contienen {@code q} (case-insensitive)
     */
    public List<SuggestionResponse> suggestAesthetics(String q) {
        return suggestFromList(terms.getAesthetics(), q);
    }

    /**
     * Sugiere géneros filtrando la lista curada de {@code seed-terms.yml}.
     *
     * @param q texto introducido por el usuario
     * @return lista de géneros que contienen {@code q} (case-insensitive)
     */
    public List<SuggestionResponse> suggestGenres(String q) {
        return suggestFromList(terms.getGenres(), q);
    }

    // ─────────────────────────────────────────────────────────────────────────

    private List<SuggestionResponse> suggestFromList(List<String> source, String q) {
        if (q == null || q.isBlank()) {
            return source.stream()
                    .limit(MAX_SUGGESTIONS)
                    .map(t -> new SuggestionResponse(t, formatLabel(t)))
                    .toList();
        }
        String lower = q.strip().toLowerCase();
        return source.stream()
                .filter(t -> t.toLowerCase().contains(lower))
                .limit(MAX_SUGGESTIONS)
                .map(t -> new SuggestionResponse(t, formatLabel(t)))
                .toList();
    }

    private SuggestionResponse toMovieSuggestion(TMDBMovieDTO movie) {
        String year = movie.getReleaseDate() != null && movie.getReleaseDate().length() >= 4
                ? " (" + movie.getReleaseDate().substring(0, 4) + ")"
                : "";
        return new SuggestionResponse(movie.getTitle(), movie.getTitle() + year);
    }

    /** Convierte camelCase o snake_case en texto legible: "darkAcademia" → "Dark Academia" */
    private String formatLabel(String term) {
        if (term == null) return "";
        // Insertar espacio antes de cada mayúscula en camelCase
        String spaced = term.replaceAll("([A-Z])", " $1").trim();
        // Capitalizar primera letra
        if (spaced.isEmpty()) return term;
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1).toLowerCase();
    }
}
