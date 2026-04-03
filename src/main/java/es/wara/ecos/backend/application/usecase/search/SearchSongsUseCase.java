package es.wara.ecos.backend.application.usecase.search;

import es.wara.ecos.backend.application.service.SongIngestionService;
import es.wara.ecos.backend.domain.model.Page;
import es.wara.ecos.backend.domain.model.Song;
import es.wara.ecos.backend.domain.port.SongRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

    public SearchSongsUseCase(SongRepository songRepository,
                               SongIngestionService ingestionService) {
        this.songRepository = songRepository;
        this.ingestionService = ingestionService;
    }

    public Page<Song> execute(SearchParams params) {
        log.info("▶ [búsqueda] cuadrantes=[{}] página={}", ingestionService.summarize(params), params.page());

        Page<Song> result = songRepository.search(params);
        log.info("  BD → {} items", result.items().size());

        if (result.items().size() >= MIN_RESULTS_THRESHOLD) {
            log.info("  ✓ umbral alcanzado, devolviendo desde BD");
            return result;
        }

        // Solo hacer fetch on-demand para cuadrantes que no se pueden pre-seed
        if (isOnDemandQuadrant(params)) {
            log.info("  → bajo umbral ({} < {}), fetch on-demand", result.items().size(), MIN_RESULTS_THRESHOLD);
            ingestionService.ingest(params);
            result = songRepository.search(params);
            log.info("  ✓ re-query tras ingesta → {} items", result.items().size());
        } else {
            log.info("  → cuadrante seed sin resultados suficientes en BD (seeder pendiente)");
        }

        return result;
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