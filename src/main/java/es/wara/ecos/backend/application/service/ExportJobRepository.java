package es.wara.ecos.backend.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

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

    public void save(ExportJob job) {
        try {
            redis.opsForValue().set(KEY_PREFIX + job.getJobId(),
                    objectMapper.writeValueAsString(job), TTL);
        } catch (JsonProcessingException e) {
            log.error("Error serializando job {}: {}", job.getJobId(), e.getMessage());
        }
    }

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