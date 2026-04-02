package es.wara.ecos.backend.infrastructure.seed;

import es.wara.ecos.backend.application.usecase.search.SearchParams;
import es.wara.ecos.backend.application.usecase.search.SearchSongsUseCase;
import es.wara.ecos.backend.infrastructure.persistence.jpa.JpaSongRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Siembra y refresca la base de datos con canciones para todos los términos configurados.
 *
 * <h3>Dos modos de ejecución:</h3>
 * <ol>
 *   <li><b>Seed inicial</b> ({@link ApplicationRunner}): corre al arrancar si la BD tiene
 *       menos de {@code seed.minSongs} canciones. Útil para poblar un entorno vacío.</li>
 *   <li><b>Refresco de tendencias</b> ({@link Scheduled}): corre según {@code seed.cron}
 *       (por defecto domingos a las 3:00h). Siempre ejecuta para actualizar el catálogo
 *       con nuevas canciones; la deduplicación en {@code SongRepositoryImpl} lo hace
 *       idempotente.</li>
 * </ol>
 *
 * <h3>Configuración:</h3>
 * <p>Los términos (géneros, estéticas, películas) se leen de {@code seed-terms.yml}.
 * Para añadir términos sin recompilar, edita el fichero externo en
 * {@code /config/seed-terms.yml} y reinicia el servicio.</p>
 *
 * @author Wara Pacheco
 * @since 1.0.0
 */
@Slf4j
@Component
public class DatabaseSeeder implements ApplicationRunner {

    private final SearchSongsUseCase searchSongsUseCase;
    private final JpaSongRepository jpaSongRepository;
    private final SeedTermsProperties terms;

    public DatabaseSeeder(SearchSongsUseCase searchSongsUseCase,
                          JpaSongRepository jpaSongRepository,
                          SeedTermsProperties terms) {
        this.searchSongsUseCase = searchSongsUseCase;
        this.jpaSongRepository = jpaSongRepository;
        this.terms = terms;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SEED INICIAL — solo si la BD está (casi) vacía
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void run(ApplicationArguments args) {
        if (!terms.isEnabled()) return;

        long total = jpaSongRepository.count();
        if (total >= terms.getMinSongs()) {
            log.info("[Seed] BD ya tiene {} canciones — omitiendo seed inicial", total);
            return;
        }

        log.info("[Seed] BD tiene {} canciones (< {}). Iniciando seed inicial en background",
                total, terms.getMinSongs());

        Thread.ofVirtual().name("db-seeder-init").start(this::runSeed);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REFRESCO DE TENDENCIAS — cron configurable
    // ─────────────────────────────────────────────────────────────────────────

    @Scheduled(cron = "${seed.cron:0 0 3 * * SUN}")
    public void refreshTrends() {
        if (!terms.isEnabled()) {
            log.debug("[Seed] Refresco omitido — seed.enabled=false");
            return;
        }

        log.info("[Seed] Iniciando refresco de tendencias (cron: {})", terms.getCron());
        Thread.ofVirtual().name("db-seeder-refresh").start(this::runSeed);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LÓGICA COMÚN
    // ─────────────────────────────────────────────────────────────────────────

    private void runSeed() {
        long start = System.currentTimeMillis();
        int total = 0;

        total += seedTerms("género",    terms.getGenres(),     this::searchByGenre);
        total += seedTerms("estética",  terms.getAesthetics(), this::searchByAesthetic);
        total += seedTerms("película",  terms.getMovies(),     this::searchByMovie);

        long elapsed = (System.currentTimeMillis() - start) / 1000;
        log.info("[Seed] Completado en {}s — {} términos procesados. Total canciones en BD: {}",
                elapsed, total, jpaSongRepository.count());
    }

    /**
     * Itera una lista de términos, llama al buscador y aplica el delay entre llamadas.
     * @return número de términos procesados con éxito
     */
    private int seedTerms(String type, List<String> termList, TermSearcher searcher) {
        if (termList == null || termList.isEmpty()) {
            log.debug("[Seed] No hay términos de {} configurados", type);
            return 0;
        }

        int processed = 0;
        for (String term : termList) {
            try {
                int found = searcher.search(term);
                log.info("[Seed] {} '{}' → {} canciones", type, term, found);
                processed++;
                Thread.sleep(terms.getDelayMs());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("[Seed] Seed interrumpido en {} '{}'", type, term);
                return processed;
            } catch (Exception e) {
                log.warn("[Seed] Error sembrando {} '{}': {}", type, term, e.getMessage());
            }
        }
        return processed;
    }

    private int searchByGenre(String genre) {
        SearchParams params = new SearchParams(
                null, null, null, genre, null, null, null,
                null, null, null,
                null, null, null, null, null, null,
                terms.getSongsPerTerm(), 0
        );
        return searchSongsUseCase.execute(params).items().size();
    }

    private int searchByAesthetic(String aesthetic) {
        SearchParams params = new SearchParams(
                null, null, aesthetic, null, null, null, null,
                null, null, null,
                null, null, null, null, null, null,
                terms.getSongsPerTerm(), 0
        );
        return searchSongsUseCase.execute(params).items().size();
    }

    private int searchByMovie(String movie) {
        SearchParams params = new SearchParams(
                null, null, null, null, null, movie, null,
                null, null, null,
                null, null, null, null, null, null,
                terms.getSongsPerTerm(), 0
        );
        return searchSongsUseCase.execute(params).items().size();
    }

    @FunctionalInterface
    private interface TermSearcher {
        int search(String term) throws Exception;
    }
}
