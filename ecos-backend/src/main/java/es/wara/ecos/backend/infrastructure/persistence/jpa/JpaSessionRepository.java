package es.wara.ecos.backend.infrastructure.persistence.jpa;

import es.wara.ecos.backend.infrastructure.persistence.entity.ExplorationSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaSessionRepository extends JpaRepository<ExplorationSessionEntity, Long> {

    List<ExplorationSessionEntity> findByUser_Id(Long userId);

}