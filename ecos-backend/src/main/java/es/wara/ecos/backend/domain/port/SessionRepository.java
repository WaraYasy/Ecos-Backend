package es.wara.ecos.backend.domain.port;

import es.wara.ecos.backend.domain.model.ExplorationSession;

import java.util.List;
import java.util.Optional;

public interface SessionRepository {

    Optional<ExplorationSession> findById(Long id);

    List<ExplorationSession> findByUserId(Long userId);

    ExplorationSession save(ExplorationSession session);

}