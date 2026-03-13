package es.wara.ecos.backend.infrastructure.persistence.repository;

import es.wara.ecos.backend.domain.model.ExplorationSession;
import es.wara.ecos.backend.domain.port.SessionRepository;
import es.wara.ecos.backend.infrastructure.persistence.jpa.JpaSessionRepository;
import es.wara.ecos.backend.infrastructure.persistence.mapper.SessionMapper;
import org.springframework.stereotype.Repository;

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
@Repository
public class SessionRepositoryImpl implements SessionRepository {

    private final JpaSessionRepository jpa;
    private final SessionMapper mapper;

    public SessionRepositoryImpl(JpaSessionRepository jpa, SessionMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public Optional<ExplorationSession> findById(UUID id) {
        return jpa.findById(id).map(mapper::toModel);
    }

    @Override
    public List<ExplorationSession> findByUserId(UUID userId) {
        return jpa.findByUser_Id(userId).stream()
                .map(mapper::toModel)
                .toList();
    }

    @Override
    public ExplorationSession save(ExplorationSession session) {
        return mapper.toModel(jpa.save(mapper.toEntity(session)));
    }

}
