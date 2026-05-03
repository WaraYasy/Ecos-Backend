package es.wara.ecos.backend.application.service;

import es.wara.ecos.backend.infrastructure.adapter.tmdb.TmdbClient;
import es.wara.ecos.backend.infrastructure.adapter.tmdb.dto.TMDBMultiResultDTO;
import es.wara.ecos.backend.infrastructure.adapter.tmdb.dto.TMDBMultiSearchResponseDTO;
import es.wara.ecos.backend.infrastructure.persistence.jpa.JpaArtistRepository;
import es.wara.ecos.backend.infrastructure.persistence.jpa.JpaSongTagRepository;
import es.wara.ecos.backend.interfaces.rest.dto.SuggestionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * Servicio de sugerencias de autocompletado para los campos de búsqueda.
 *
 * <p>Cinco tipos de sugerencias:
 * <ul>
 *   <li><b>movie</b> → consulta TMDb ({@code /search/multi}) en tiempo real</li>
 *   <li><b>aesthetic</b> → tags de tipo {@code aesthetic} en {@code song_tags}</li>
 *   <li><b>genre</b> → tags de tipo {@code genre} en {@code song_tags}</li>
 *   <li><b>artist</b> → artistas en la BD local por nombre</li>
 *   <li><b>country</b> → códigos ISO de país en la BD, filtrados por nombre en inglés</li>
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

    private final TmdbClient tmdbClient;
    private final JpaArtistRepository artistRepository;
    private final JpaSongTagRepository tagRepository;

    public SuggestionsService(TmdbClient tmdbClient,
                               JpaArtistRepository artistRepository,
                               JpaSongTagRepository tagRepository) {
        this.tmdbClient = tmdbClient;
        this.artistRepository = artistRepository;
        this.tagRepository = tagRepository;
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
     * Sugiere estéticas desde {@code song_tags} con {@code type='aesthetic'}.
     *
     * @param q texto introducido por el usuario
     * @return lista de estéticas que contienen {@code q} (case-insensitive); vacía si la BD no tiene datos
     */
    public List<SuggestionResponse> suggestAesthetics(String q) {
        String search = q == null ? "" : q.strip();
        return tagRepository.findDistinctTagsByTypeContaining("aesthetic", search).stream()
                .limit(MAX_SUGGESTIONS)
                .map(t -> new SuggestionResponse(t, formatLabel(t)))
                .toList();
    }

    /**
     * Sugiere géneros desde {@code song_tags} con {@code type='genre'}.
     *
     * @param q texto introducido por el usuario
     * @return lista de géneros que contienen {@code q} (case-insensitive); vacía si la BD no tiene datos
     */
    public List<SuggestionResponse> suggestGenres(String q) {
        String search = q == null ? "" : q.strip();
        return tagRepository.findDistinctTagsByTypeContaining("genre", search).stream()
                .limit(MAX_SUGGESTIONS)
                .map(t -> new SuggestionResponse(t, formatLabel(t)))
                .toList();
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
     * Filtra por nombre en inglés (ej. "uni" → "United States") y devuelve el código ISO como value.
     *
     * @param q texto introducido por el usuario
     * @return lista de sugerencias con código ISO como value y nombre en inglés como label
     */
    public List<SuggestionResponse> suggestCountries(String q) {
        String lower = (q == null || q.isBlank()) ? "" : q.strip().toLowerCase(Locale.ENGLISH);
        return artistRepository.findAllDistinctCountries().stream()
                .map(code -> new SuggestionResponse(code, new Locale("", code).getDisplayCountry(Locale.ENGLISH)))
                .filter(s -> lower.isEmpty() || s.label().toLowerCase(Locale.ENGLISH).contains(lower))
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────────────

    private SuggestionResponse toMultiSuggestion(TMDBMultiResultDTO result) {
        String displayTitle = result.getDisplayTitle();
        String year = result.getDisplayYear();
        String typeSuffix = "tv".equals(result.getMediaType()) ? " · Serie" : "";
        String label = displayTitle + (year != null ? " (" + year + ")" : "") + typeSuffix;
        return new SuggestionResponse(displayTitle, label);
    }

    /** Convierte camelCase en texto legible: "darkAcademia" → "Dark academia" */
    private String formatLabel(String term) {
        if (term == null) return "";
        String spaced = term.replaceAll("([A-Z])", " $1").trim();
        if (spaced.isEmpty()) return term;
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1).toLowerCase();
    }
}