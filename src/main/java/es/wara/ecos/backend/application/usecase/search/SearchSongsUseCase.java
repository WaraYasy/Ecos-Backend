package es.wara.ecos.backend.application.usecase.search;

import es.wara.ecos.backend.application.service.SongIngestionService;
import es.wara.ecos.backend.domain.model.Page;
import es.wara.ecos.backend.domain.model.Song;
import es.wara.ecos.backend.domain.port.SemanticSearchProvider;
import es.wara.ecos.backend.domain.port.SongRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Caso de uso para búsqueda multidimensional de canciones.
 *
 * <p><b>Estrategia BD-primero:</b> consulta siempre la base de datos local.
 * Para cuadrantes <b>seed</b> (aesthetic, genre, decade, country, texture)
 * la BD es la única fuente — el {@code DatabaseSeeder} la mantiene poblada.
 * Para cuadrantes <b>on-demand</b> (movie, q, artist), si la BD no tiene
 * resultados se hace un fetch puntual a APIs externas, se persiste y se
 * re-consulta.</p>
 *
 * @author Wara Pacheco
 * @version 4.0
 * @since 1.0.0
 */
@Slf4j
@Service
public class SearchSongsUseCase {

    private static final int MIN_RESULTS_THRESHOLD = 5;

    private final SongRepository songRepository;
    private final SongIngestionService ingestionService;
    private final SemanticSearchProvider semanticSearchProvider;

    public SearchSongsUseCase(SongRepository songRepository,
                               SongIngestionService ingestionService,
                               SemanticSearchProvider semanticSearchProvider) {
        this.songRepository = songRepository;
        this.ingestionService = ingestionService;
        this.semanticSearchProvider = semanticSearchProvider;
    }

    public Page<Song> execute(SearchParams params) {
        log.info("[búsqueda] cuadrantes=[{}] página={}", ingestionService.summarize(params), params.page());

        // Búsqueda semántica solo en modo enhanced y page=0.
        // enhanced=false → BD con relevanceScore es suficiente y más rápido.
        // page>0 → el micro no soporta offset; devolvería los mismos IDs en todas las páginas.
        boolean hasQuery = params.q() != null && !params.q().isBlank();
        CompletableFuture<List<UUID>> semanticFuture = hasQuery && params.enhanced() && params.page() == 0
                ? CompletableFuture.supplyAsync(() ->
                        semanticSearchProvider.searchEnhanced(params.q(), params.pageSize() * 2))
                : CompletableFuture.completedFuture(List.of());

        Page<Song> result = songRepository.search(params);
        log.info("  BD → {} items", result.items().size());

        if (result.items().size() < MIN_RESULTS_THRESHOLD && isOnDemandQuadrant(params) && shouldIngest(params)) {
            log.info("  → bajo umbral ({} < {}), fetch on-demand", result.items().size(), MIN_RESULTS_THRESHOLD);
            ingestionService.ingest(params);
            result = songRepository.search(params);
            log.info("  ✓ re-query tras ingesta → {} items", result.items().size());
        }

        // Merge híbrido: BD primero (matches exactos), semánticos como relleno
        List<UUID> semanticIds = semanticFuture.join();
        if (!semanticIds.isEmpty()) {
            result = mergeWithSemantic(result, semanticIds, params);
            log.debug("  hybrid merge: {} ids del micro aplicados", semanticIds.size());
        }

        return result;
    }

    /**
     * Merge híbrido de resultados SQL y semánticos.
     *
     * <p>Estrategia: BD primero (matches exactos, ya ordenados por relevancia),
     * semánticos solo como relleno para los huecos restantes hasta {@code pageSize}.
     * Los IDs semánticos no presentes en la página SQL se fetchean en un único IN.</p>
     */
    private Page<Song> mergeWithSemantic(Page<Song> page, List<UUID> semanticIds, SearchParams params) {
        Set<UUID> dbIds = page.items().stream()
                .filter(s -> s.getId() != null)
                .map(Song::getId)
                .collect(java.util.stream.Collectors.toSet());

        // Fetch semánticos que no están ya en la página SQL (1 query IN)
        List<UUID> missingIds = semanticIds.stream()
                .filter(id -> !dbIds.contains(id))
                .collect(java.util.stream.Collectors.toList());

        Map<UUID, Song> extraMap = new java.util.LinkedHashMap<>();
        if (!missingIds.isEmpty()) {
            songRepository.findAllById(missingIds)
                    .forEach(s -> { if (s.getId() != null) extraMap.put(s.getId(), s); });
        }

        // BD primero; semánticos-only a continuación hasta completar pageSize
        List<Song> merged = new ArrayList<>(page.items());
        for (UUID id : semanticIds) {
            if (merged.size() >= params.pageSize()) break;
            Song s = extraMap.get(id);
            if (s != null) merged.add(s);
        }

        long correctedTotal = Math.max(page.total(), (long) semanticIds.size());
        return new Page<>(merged, page.page(), page.pageSize(), correctedTotal);
    }

    /**
     * Cuando enhanced=true y el único cuadrante on-demand activo es q libre,
     * la ingesta no tiene sentido: Spotify no entiende queries evocadoras como
     * "canciones parecidas a X" o "música para una boda".
     * El micro semántico ya gestiona esas intenciones devolviendo UUIDs directamente.
     */
    private boolean shouldIngest(SearchParams params) {
        if (!params.enhanced()) return true;
        boolean hasConcreteOnDemand = (params.movie()  != null && !params.movie().isBlank())
                                   || (params.artist() != null && !params.artist().isBlank())
                                   || (params.year()   != null && !params.year().isBlank());
        return hasConcreteOnDemand;
    }

    /**
     * Cuadrantes on-demand: movie, q, artist.
     * El resto (aesthetic, genre, decade, country, texture) los gestiona el DatabaseSeeder.
     */
    private boolean isOnDemandQuadrant(SearchParams params) {
        return (params.movie()  != null && !params.movie().isBlank())
            || (params.q()      != null && !params.q().isBlank())
            || (params.artist() != null && !params.artist().isBlank())
            || (params.year()   != null && !params.year().isBlank());
    }

}