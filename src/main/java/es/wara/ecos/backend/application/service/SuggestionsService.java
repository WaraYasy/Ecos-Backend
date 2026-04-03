package es.wara.ecos.backend.application.service;

import es.wara.ecos.backend.infrastructure.adapter.tmdb.TmdbClient;
import es.wara.ecos.backend.infrastructure.adapter.tmdb.dto.TMDBMultiResultDTO;
import es.wara.ecos.backend.infrastructure.adapter.tmdb.dto.TMDBMultiSearchResponseDTO;
import es.wara.ecos.backend.infrastructure.persistence.jpa.JpaArtistRepository;
import es.wara.ecos.backend.infrastructure.seed.SeedTermsProperties;
import es.wara.ecos.backend.interfaces.rest.dto.SuggestionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio de sugerencias de autocompletado para los campos de búsqueda.
 *
 * <p>Cinco tipos de sugerencias:
 * <ul>
 *   <li><b>movie</b> → consulta TMDb ({@code /search/multi}) en tiempo real y devuelve películas y series relevantes</li>
 *   <li><b>aesthetic</b> → filtra la lista curada de estéticas de {@code seed-terms.yml}</li>
 *   <li><b>genre</b> → filtra la lista curada de géneros de {@code seed-terms.yml}</li>
 *   <li><b>artist</b> → busca artistas en la base de datos local por nombre</li>
 *   <li><b>country</b> → busca países de origen de artistas en la base de datos local</li>
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
    private final JpaArtistRepository artistRepository;

    public SuggestionsService(TmdbClient tmdbClient, SeedTermsProperties terms,
                               JpaArtistRepository artistRepository) {
        this.tmdbClient = tmdbClient;
        this.terms = terms;
        this.artistRepository = artistRepository;
    }

    /**
     * Sugiere películas y series buscando en TMDb con {@code /search/multi}.
     * Requiere al menos 2 caracteres. Filtra resultados de tipo {@code "movie"} y {@code "tv"}.
     *
     * @param q texto introducido por el usuario
     * @return lista de sugerencias con título y año; vacía si {@code q} es corto o hay error
     */
    public List<SuggestionResponse> suggestMovies(String q) {
        if (q == null || q.strip().length() < 2) return List.of();

        try {
            TMDBMultiSearchResponseDTO results = tmdbClient.searchMulti(q.strip(), 1);
            if (results == null || results.getResults() == null) return List.of();

            return results.getResults().stream()
                    // Solo películas y series — excluir personas u otros tipos
                    .filter(r -> "movie".equals(r.getMediaType()) || "tv".equals(r.getMediaType()))
                    .filter(r -> r.getDisplayTitle() != null && !r.getDisplayTitle().isBlank())
                    .limit(MAX_SUGGESTIONS)
                    .map(this::toMultiSuggestion)
                    .toList();
        } catch (Exception e) {
            log.warn("Error buscando sugerencias de película/serie para '{}': {}", q, e.getMessage());
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

    /**
     * Sugiere artistas buscando en la base de datos local por nombre.
     *
     * @param q texto introducido por el usuario
     * @return lista de artistas cuyo nombre contiene {@code q}; vacía si {@code q} es corto
     */
    public List<SuggestionResponse> suggestArtists(String q) {
        if (q == null || q.strip().length() < 2) return List.of();
        return artistRepository.findTop6ByNameContainingIgnoreCase(q.strip()).stream()
                .map(a -> new SuggestionResponse(a.getName(), a.getName()))
                .toList();
    }

    /**
     * Sugiere países de origen de artistas desde la base de datos local.
     *
     * @param q texto introducido por el usuario
     * @return lista de países que contienen {@code q}; vacía si la BD no tiene datos
     */
    public List<SuggestionResponse> suggestCountries(String q) {
        String search = (q == null || q.isBlank()) ? "" : q.strip();
        return artistRepository.findDistinctCountriesContaining(search).stream()
                .limit(MAX_SUGGESTIONS)
                .map(c -> new SuggestionResponse(c, c))
                .toList();
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

    private SuggestionResponse toMultiSuggestion(TMDBMultiResultDTO result) {
        String displayTitle = result.getDisplayTitle();
        String year = result.getDisplayYear();
        // El label incluye el año y, para series, "(Serie)" para distinguirlas de películas
        String typeSuffix = "tv".equals(result.getMediaType()) ? " · Serie" : "";
        String label = displayTitle + (year != null ? " (" + year + ")" : "") + typeSuffix;
        return new SuggestionResponse(displayTitle, label);
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
