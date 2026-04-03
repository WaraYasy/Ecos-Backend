package es.wara.ecos.backend.infrastructure.seed;

import es.wara.ecos.backend.application.service.MusicAggregationService;
import es.wara.ecos.backend.domain.model.Song;
import es.wara.ecos.backend.domain.port.SongRepository;
import es.wara.ecos.backend.infrastructure.adapter.itunes.ItunesClient;
import es.wara.ecos.backend.infrastructure.persistence.entity.ArtistEntity;
import es.wara.ecos.backend.infrastructure.persistence.entity.SongEntity;
import es.wara.ecos.backend.infrastructure.persistence.jpa.JpaSongRepository;
import es.wara.ecos.backend.infrastructure.persistence.mapper.SongMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Job de mantenimiento que repara canciones existentes con datos incompletos.
 *
 * <p>Corre en background sin bloquear el arranque de la aplicación. Opera en lotes
 * para no saturar las APIs externas. Cada campo se procesa de forma independiente,
 * por lo que un fallo en uno no afecta al resto.</p>
 *
 * <h3>Qué repara:</h3>
 * <ul>
 *   <li><b>preview_url NULL</b> — rellena desde iTunes Search API</li>
 *   <li><b>Sin tags</b> — obtiene géneros y estéticas desde Last.fm vía enrich()</li>
 * </ul>
 *
 * <p>Los crons son configurables vía variables de entorno:
 * {@code ENRICHMENT_PREVIEW_CRON} y {@code ENRICHMENT_TAGS_CRON}.</p>
 *
 * @author Wara Pacheco
 * @since 1.0.0
 */
@Slf4j
@Component
public class EnrichmentMaintenanceJob {

    private static final int BATCH_SIZE = 30;
    private static final long DELAY_MS = 500;

    private final JpaSongRepository jpaSongRepository;
    private final SongRepository songRepository;
    private final SongMapper songMapper;
    private final ItunesClient itunesClient;
    private final MusicAggregationService musicAggregationService;

    public EnrichmentMaintenanceJob(JpaSongRepository jpaSongRepository,
                                    SongRepository songRepository,
                                    SongMapper songMapper,
                                    ItunesClient itunesClient,
                                    MusicAggregationService musicAggregationService) {
        this.jpaSongRepository = jpaSongRepository;
        this.songRepository = songRepository;
        this.songMapper = songMapper;
        this.itunesClient = itunesClient;
        this.musicAggregationService = musicAggregationService;
    }

    /**
     * Rellena preview URLs nulas usando iTunes. Corre cada día a las 2:00h.
     * Se ejecuta antes que el seeder (3:00h) para no competir por las APIs.
     */
    @Scheduled(cron = "${enrichment.preview.cron:0 0 2 * * *}")
    public void fillMissingPreviews() {
        Thread.ofVirtual().name("enrichment-preview").start(() -> {
            List<SongEntity> songs = jpaSongRepository.findWithoutPreview(PageRequest.of(0, BATCH_SIZE));
            if (songs.isEmpty()) {
                log.debug("[Enrichment] Sin canciones con preview_url nulo");
                return;
            }
            log.info("[Enrichment] Rellenando preview_url para {} canciones", songs.size());
            int fixed = 0;
            for (SongEntity song : songs) {
                String title  = song.getTitle();
                String artist = primaryArtist(song);
                if (title == null || title.isBlank()) continue;
                try {
                    itunesClient.findPreviewUrl(title, artist).ifPresent(url -> {
                        song.setPreviewUrl(url);
                        jpaSongRepository.save(song);
                        log.debug("[Enrichment] Preview añadido: '{} - {}'", artist, title);
                    });
                    fixed++;
                    Thread.sleep(DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("[Enrichment] Preview interrumpido en '{}'", title);
                    break;
                } catch (Exception e) {
                    log.debug("[Enrichment] Error preview '{}': {}", title, e.getMessage());
                }
            }
            log.info("[Enrichment] Preview completado — {}/{} canciones procesadas", fixed, songs.size());
        });
    }

    /**
     * Rellena tags vacíos usando Last.fm. Corre cada día a las 2:30h.
     * Usa el pipeline enrich() existente para reutilizar toda la lógica de guardado.
     */
    @Scheduled(cron = "${enrichment.tags.cron:0 30 2 * * *}")
    public void fillMissingTags() {
        Thread.ofVirtual().name("enrichment-tags").start(() -> {
            List<SongEntity> songs = jpaSongRepository.findWithoutTags(PageRequest.of(0, BATCH_SIZE));
            if (songs.isEmpty()) {
                log.debug("[Enrichment] Sin canciones sin tags");
                return;
            }
            log.info("[Enrichment] Rellenando tags para {} canciones", songs.size());
            int fixed = 0;
            for (SongEntity entity : songs) {
                String title  = entity.getTitle();
                String artist = primaryArtist(entity);
                if (title == null || title.isBlank() || artist == null || artist.isBlank()) continue;
                try {
                    Song song = songMapper.toModel(entity);
                    Song enriched = musicAggregationService.enrich(song);
                    if (enriched.getTags() != null && !enriched.getTags().isEmpty()) {
                        songRepository.save(enriched);
                        log.debug("[Enrichment] Tags añadidos a '{}': {}", title, enriched.getTags().size());
                        fixed++;
                    }
                    Thread.sleep(DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("[Enrichment] Tags interrumpido en '{}'", title);
                    break;
                } catch (Exception e) {
                    log.debug("[Enrichment] Error tags '{}': {}", title, e.getMessage());
                }
            }
            log.info("[Enrichment] Tags completado — {}/{} canciones con tags nuevos", fixed, songs.size());
        });
    }

    // ─────────────────────────────────────────────────────────────────────────

    private String primaryArtist(SongEntity song) {
        if (song.getArtists() == null || song.getArtists().isEmpty()) return null;
        ArtistEntity first = song.getArtists().get(0);
        return first != null ? first.getName() : null;
    }
}