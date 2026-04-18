package es.wara.ecos.backend.application.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio de orquestación para jobs de exportación de playlists a Spotify.
 * <p>
 * Crea el job en Redis, lo persiste en estado {@code PENDING} y delega
 * la ejecución asíncrona a {@link ExportWorker}.
 * </p>
 *
 * @author Wara Pacheco
 * @since 1.0.0
 */
@Service
public class ExportJobService {

    private final ExportJobRepository jobRepository;
    private final ExportWorker exportWorker;

    public ExportJobService(ExportJobRepository jobRepository, ExportWorker exportWorker) {
        this.jobRepository = jobRepository;
        this.exportWorker  = exportWorker;
    }

    /**
     * Crea un nuevo job en estado {@code PENDING}, lo persiste y lanza el worker async.
     *
     * @param playlistId playlist a exportar
     * @param userId     propietario de la playlist (debe tener token Spotify vinculado)
     * @return el job recién creado con su {@code jobId} para que el front pueda hacer polling
     */
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

    /**
     * Consulta el estado actual de un job por su ID.
     *
     * @param jobId identificador del job
     * @return el job si existe y no ha expirado en Redis; {@code Optional.empty()} si no
     */
    public Optional<ExportJob> findById(String jobId) {
        return jobRepository.findById(jobId);
    }
}