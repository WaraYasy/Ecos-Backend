package es.wara.ecos.backend.application.service;

import es.wara.ecos.backend.application.usecase.playlist.ExportPlaylistToSpotifyUseCase;
import es.wara.ecos.backend.domain.model.Playlist;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

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