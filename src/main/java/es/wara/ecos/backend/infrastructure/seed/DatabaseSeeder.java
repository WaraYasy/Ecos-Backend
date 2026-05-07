package es.wara.ecos.backend.infrastructure.seed;

import es.wara.ecos.backend.application.service.SongIngestionService;
import es.wara.ecos.backend.application.usecase.search.SearchParams;
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
 *       menos de {@code seed.minSongs} canciones.</li>
 *   <li><b>Refresco periódico</b> ({@link Scheduled}): cron configurable (por defecto
 *       domingos a las 3:00h). Siempre idempotente gracias a la deduplicación de BD.</li>
 * </ol>
 *
 * <h3>Cuadrantes sembrados:</h3>
 * <ul>
 *   <li>Géneros, estéticas, texturas — términos curados en {@code seed-terms.yml}</li>
 *   <li>Décadas (ej. "1990s") y países (ej. "Spain") — ídem</li>
 *   <li>Películas — soundtracks via MusicBrainz + Spotify</li>
 * </ul>
 *
 * @author Wara Pacheco
 * @since 1.0.0
 */
@Slf4j
@Component
public class DatabaseSeeder implements ApplicationRunner {

    private final SongIngestionService ingestionService;
    private final JpaSongRepository jpaSongRepository;
    private final SeedTermsProperties terms;

    public DatabaseSeeder(SongIngestionService ingestionService,
                          JpaSongRepository jpaSongRepository,
                          SeedTermsProperties terms) {
        this.ingestionService = ingestionService;
        this.jpaSongRepository = jpaSongRepository;
        this.terms = terms;
    }

    /**
     * Seed inicial al arrancar: si la BD tiene menos de {@code seed.minSongs} canciones,
     * lanza el proceso de siembra en un hilo virtual para no bloquear el arranque.
     */
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

    /**
     * Refresco semanal de tendencias (por defecto domingos a las 3:00h).
     * Resiembra todos los cuadrantes de forma idempotente — las canciones ya existentes no se duplican.
     */
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

    private void runSeed() {
        long start = System.currentTimeMillis();
        int total = 0;

        total += seedTerms("género",    terms.getGenres(),      this::ingestByGenre);
        total += seedTerms("estética",  terms.getAesthetics(),  this::ingestByAesthetic);
        total += seedTerms("década",    terms.getDecades(),     this::ingestByDecade);
        total += seedTerms("país",      terms.getCountries(),   this::ingestByCountry);
        total += seedTerms("película",  terms.getMovies(),      this::ingestByMovie);
        total += seedTerms("artista",   terms.getArtists(),     this::ingestByArtist);

        long elapsed = (System.currentTimeMillis() - start) / 1000;
        log.info("[Seed] Completado en {}s — {} términos procesados. Total canciones en BD: {}",
                elapsed, total, jpaSongRepository.count());
    }

    private int seedTerms(String type, List<String> termList, TermIngester ingester) {
        if (termList == null || termList.isEmpty()) {
            log.debug("[Seed] No hay términos de {} configurados", type);
            return 0;
        }
        int processed = 0;
        for (String term : termList) {
            try {
                int saved = ingester.ingest(term);
                log.info("[Seed] {} '{}' → {} canciones guardadas", type, term, saved);
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

    // ─────────────────────────────────────────────────────────────────────────
    // INGESTA POR CUADRANTE
    // ─────────────────────────────────────────────────────────────────────────

    private int ingestByGenre(String genre) {
        return ingestionService.ingest(params().genre(genre).build());
    }

    private int ingestByAesthetic(String aesthetic) {
        return ingestionService.ingest(params().aesthetic(aesthetic).build());
    }

    private int ingestByDecade(String decade) {
        return ingestionService.ingest(params().decade(decade).build());
    }

    private int ingestByCountry(String country) {
        return ingestionService.ingest(params().country(country).build());
    }

    private int ingestByMovie(String movie) {
        return ingestionService.ingest(params().movie(movie).build());
    }

    private int ingestByArtist(String artist) {
        return ingestionService.ingest(params().artist(artist).build());
    }

    private SearchParams.Builder params() {
        return SearchParams.builder().pageSize(terms.getSongsPerTerm());
    }

    @FunctionalInterface
    private interface TermIngester {
        int ingest(String term) throws Exception;
    }
}