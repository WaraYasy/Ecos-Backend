package es.wara.ecos.backend.application.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class ExportJobService {

    private final ExportJobRepository jobRepository;
    private final ExportWorker exportWorker;

    public ExportJobService(ExportJobRepository jobRepository, ExportWorker exportWorker) {
        this.jobRepository = jobRepository;
        this.exportWorker  = exportWorker;
    }

    public ExportJob submit(UUID playlistId, UUID userId) {
        ExportJob job = new ExportJob(
                UUID.randomUUID().toString(),
                playlistId,
                userId,
                ExportJob.Status.PENDING,
                null,
                null,
                LocalDateTime.now()
        );
        jobRepository.save(job);
        exportWorker.process(job.getJobId(), playlistId, userId);
        return job;
    }

    public Optional<ExportJob> findById(String jobId) {
        return jobRepository.findById(jobId);
    }
}