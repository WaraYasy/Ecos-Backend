package es.wara.ecos.backend.application.service;

import es.wara.ecos.backend.application.usecase.playlist.ExportPlaylistToSpotifyUseCase;
import es.wara.ecos.backend.domain.model.Playlist;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Worker asíncrono que ejecuta la exportación de una playlist a Spotify.
 * <p>
 * Vive en un bean separado de {@link ExportJobService} para que el proxy de Spring AOP
 * intercepte correctamente la anotación {@link org.springframework.scheduling.annotation.Async}.
 * Si estuviera en el mismo bean, la llamada interna saltaría el proxy y se ejecutaría en el
 * hilo del llamador.
 * </p>
 *
 * @author Wara Pacheco
 * @since 1.0.0
 */
@Slf4j
@Service
public class ExportWorker {

    private final ExportPlaylistToSpotifyUseCase exportUseCase;
    private final ExportJobRepository jobRepository;

    public ExportWorker(ExportPlaylistToSpotifyUseCase exportUseCase,
                        ExportJobRepository jobRepository) {
        this.exportUseCase  = exportUseCase;
        this.jobRepository  = jobRepository;
    }

    /**
     * Ejecuta la exportación en el pool {@code exportExecutor}.
     * Actualiza el estado del job a {@code PROCESSING} → {@code DONE} o {@code FAILED}.
     *
     * @param jobId      identificador del job en Redis
     * @param playlistId playlist a exportar
     * @param userId     propietario con token Spotify vinculado
     */
    @Async("exportExecutor")
    public void process(String jobId, UUID playlistId, UUID userId) {
        jobRepository.findById(jobId).ifPresent(job -> {
            job.setStatus(ExportJob.Status.PROCESSING);
            jobRepository.save(job);
        });

        jobRepository.findById(jobId).ifPresent(job -> {
            try {
                Playlist result = exportUseCase.execute(playlistId, userId);
                job.setStatus(ExportJob.Status.DONE);
                job.setSpotifyPlaylistId(result.getSpotifyExportId());
                log.info("Export job {} completado: spotifyId={}", jobId, result.getSpotifyExportId());
            } catch (Exception e) {
                job.setStatus(ExportJob.Status.FAILED);
                job.setError(e.getMessage());
                log.error("Export job {} fallido: {}", jobId, e.getMessage());
            }
            jobRepository.save(job);
        });
    }
}