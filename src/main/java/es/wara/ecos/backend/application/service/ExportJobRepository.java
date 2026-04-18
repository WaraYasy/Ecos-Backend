package es.wara.ecos.backend.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

/**
 * Repositorio Redis para {@link ExportJob}.
 * <p>
 * Cada job se guarda como JSON bajo la clave {@code export:job:{jobId}} con TTL de 1 hora.
 * No se usa Spring Data Redis para mantener control explícito sobre la serialización.
 * </p>
 *
 * @author Wara Pacheco
 * @since 1.0.0
 */
@Slf4j
@Repository
public class ExportJobRepository {

    private static final String KEY_PREFIX = "export:job:";
    private static final Duration TTL = Duration.ofHours(1);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public ExportJobRepository(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis        = redis;
        this.objectMapper = objectMapper;
    }

    /**
     * Persiste o sobreescribe el job en Redis reiniciando el TTL.
     *
     * @param job job a guardar
     */
    public void save(ExportJob job) {
        try {
            redis.opsForValue().set(KEY_PREFIX + job.getJobId(),
                    objectMapper.writeValueAsString(job), TTL);
        } catch (JsonProcessingException e) {
            log.error("Error serializando job {}: {}", job.getJobId(), e.getMessage());
        }
    }

    /**
     * Recupera un job por su ID.
     *
     * @param jobId identificador del job
     * @return el job si existe y no ha expirado; {@code Optional.empty()} en caso contrario
     */
    public Optional<ExportJob> findById(String jobId) {
        String json = redis.opsForValue().get(KEY_PREFIX + jobId);
        if (json == null) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(json, ExportJob.class));
        } catch (JsonProcessingException e) {
            log.error("Error deserializando job {}: {}", jobId, e.getMessage());
            return Optional.empty();
        }
    }
}