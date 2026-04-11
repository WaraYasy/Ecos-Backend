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
        log.info("▶ [búsqueda] cuadrantes=[{}] página={}", ingestionService.summarize(params), params.page());

        // Lanzar búsqueda semántica en paralelo con la consulta SQL (solo cuando hay texto libre)
        boolean hasQuery = params.q() != null && !params.q().isBlank();
        CompletableFuture<List<UUID>> semanticFuture = hasQuery
                ? CompletableFuture.supplyAsync(() -> params.enhanced()
                        ? semanticSearchProvider.searchEnhanced(params.q(), params.pageSize() * 2)
                        : semanticSearchProvider.search(params.q(), params.pageSize() * 2))
                : CompletableFuture.completedFuture(List.of());

        Page<Song> result = songRepository.search(params);
        log.info("  BD → {} items", result.items().size());

        if (result.items().size() < MIN_RESULTS_THRESHOLD && isOnDemandQuadrant(params)) {
            log.info("  → bajo umbral ({} < {}), fetch on-demand", result.items().size(), MIN_RESULTS_THRESHOLD);
            ingestionService.ingest(params);
            result = songRepository.search(params);
            log.info("  ✓ re-query tras ingesta → {} items", result.items().size());
        }

        // Merge híbrido: semánticos primero (en orden de score), luego BD, sin duplicados
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
     * <p>Estrategia:
     * <ol>
     *   <li>Canciones que el micro devolvió → aparecen primero en orden de score.
     *       Las que no estaban en la página SQL se fetchean por ID (1 query IN).</li>
     *   <li>Canciones solo en BD (no en semántico) → al final, en su orden original.</li>
     *   <li>El resultado se trunca a {@code pageSize}.</li>
     * </ol>
     */
    private Page<Song> mergeWithSemantic(Page<Song> page, List<UUID> semanticIds, SearchParams params) {
        Map<UUID, Song> dbMap = page.items().stream()
                .filter(s -> s.getId() != null)
                .collect(java.util.stream.Collectors.toMap(Song::getId, s -> s));

        Set<UUID> dbIds = dbMap.keySet();

        // IDs semánticos que no están en la página SQL → fetch adicional (1 query IN)
        List<UUID> missingIds = semanticIds.stream()
                .filter(id -> !dbIds.contains(id))
                .collect(java.util.stream.Collectors.toList());

        if (!missingIds.isEmpty()) {
            missingIds.stream()
                    .map(songRepository::findById)
                    .filter(java.util.Optional::isPresent)
                    .map(java.util.Optional::get)
                    .forEach(s -> { if (s.getId() != null) dbMap.put(s.getId(), s); });
        }

        // Semánticos en orden de score del micro (incluye los recién fetcheados)
        List<Song> semantic = semanticIds.stream()
                .map(dbMap::get)
                .filter(s -> s != null)
                .collect(java.util.stream.Collectors.toList());

        Set<UUID> semanticIdSet = semantic.stream()
                .map(Song::getId)
                .collect(java.util.stream.Collectors.toSet());

        // BD-only al final
        List<Song> dbOnly = page.items().stream()
                .filter(s -> s.getId() == null || !semanticIdSet.contains(s.getId()))
                .collect(java.util.stream.Collectors.toList());

        List<Song> merged = new ArrayList<>(semantic);
        merged.addAll(dbOnly);

        // Truncar a pageSize
        if (merged.size() > params.pageSize()) {
            merged = merged.subList(0, params.pageSize());
        }

        return new Page<>(merged, page.page(), page.pageSize(), page.total());
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