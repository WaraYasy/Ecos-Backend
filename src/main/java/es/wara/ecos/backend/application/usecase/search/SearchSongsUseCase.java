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
                ? CompletableFuture.supplyAsync(
                        () -> semanticSearchProvider.search(params.q(), params.pageSize() * 2))
                : CompletableFuture.completedFuture(List.of());

        Page<Song> result = songRepository.search(params);
        log.info("  BD → {} items", result.items().size());

        if (result.items().size() < MIN_RESULTS_THRESHOLD && isOnDemandQuadrant(params)) {
            log.info("  → bajo umbral ({} < {}), fetch on-demand", result.items().size(), MIN_RESULTS_THRESHOLD);
            ingestionService.ingest(params);
            result = songRepository.search(params);
            log.info("  ✓ re-query tras ingesta → {} items", result.items().size());
        }

        // Boost semántico: canciones que coinciden con resultados del micro suben al principio
        List<UUID> semanticIds = semanticFuture.join();
        if (!semanticIds.isEmpty() && !result.items().isEmpty()) {
            result = boostSemanticMatches(result, semanticIds);
            log.debug("  semantic boost: {} ids del micro aplicados", semanticIds.size());
        }

        return result;
    }

    /**
     * Reordena la página priorizando las canciones del micro de IA en su orden de score.
     * Primero aparecen las que coinciden con los resultados semánticos (en el orden del micro),
     * después el resto de canciones SQL en su orden original.
     */
    private Page<Song> boostSemanticMatches(Page<Song> page, List<UUID> semanticIds) {
        // Índice rápido por ID para buscar canciones sin iterar la lista
        Map<UUID, Song> songMap = page.items().stream()
                .filter(s -> s.getId() != null)
                .collect(java.util.stream.Collectors.toMap(Song::getId, s -> s));

        // Las boosteadas siguen el orden de score del micro (no el orden SQL)
        List<Song> boosted = semanticIds.stream()
                .map(songMap::get)
                .filter(s -> s != null)
                .collect(java.util.stream.Collectors.toList());

        if (boosted.isEmpty()) return page;

        Set<UUID> boostedIds = boosted.stream()
                .map(Song::getId)
                .collect(java.util.stream.Collectors.toSet());

        List<Song> remaining = page.items().stream()
                .filter(s -> s.getId() == null || !boostedIds.contains(s.getId()))
                .collect(java.util.stream.Collectors.toList());

        boosted.addAll(remaining);
        return new Page<>(boosted, page.page(), page.pageSize(), page.total());
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