package es.wara.ecos.backend.infrastructure.persistence.repository;

import es.wara.ecos.backend.domain.model.ExplorationSession;
import es.wara.ecos.backend.domain.port.SessionRepository;
import es.wara.ecos.backend.infrastructure.persistence.jpa.JpaSessionRepository;
import es.wara.ecos.backend.infrastructure.persistence.mapper.SessionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementación del puerto de dominio SessionRepository usando JPA.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Slf4j
@Repository
public class SessionRepositoryImpl implements SessionRepository {

    private final JpaSessionRepository jpa;
    private final SessionMapper mapper;

    public SessionRepositoryImpl(JpaSessionRepository jpa, SessionMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ExplorationSession> findById(UUID id) {
        return jpa.findByIdWithNodes(id).map(mapper::toModel);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExplorationSession> findByUserId(UUID userId) {
        StopWatch sw = new StopWatch();

        sw.start();
        // JOIN FETCH carga sesiones + nodos + canciones + álbumes en 1 query.
        // Las colecciones de canción (artistas, tags, películas, audioFeatures)
        // se resuelven en batch gracias a @BatchSize, sin N+1.
        var entities = jpa.findByUser_IdWithNodes(userId);
        sw.stop();
        log.debug("SessionRepository.findByUserId: {} sesiones con nodos (JOIN FETCH) → {}ms (userId={})",
                entities.size(), sw.getTotalTimeMillis(), userId);

        sw.start();
        List<ExplorationSession> result = entities.stream()
                .map(mapper::toModel)
                .toList();
        sw.stop();
        log.debug("SessionRepository.findByUserId: mapping completado → {}ms",
                sw.getLastTaskTimeMillis());

        return result;
    }

    @Override
    @Transactional
    public ExplorationSession save(ExplorationSession session) {
        var saved = jpa.save(mapper.toEntity(session));
        jpa.flush();
        // Recargamos desde BD para que las relaciones lazy se resuelvan en la misma transacción
        return mapper.toModel(jpa.findById(saved.getId()).orElseThrow());
    }

    @Override
    @Transactional(readOnly = true)
    public long countByUserId(UUID userId) {
        return jpa.countByUser_Id(userId);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        jpa.deleteById(id);
    }

}