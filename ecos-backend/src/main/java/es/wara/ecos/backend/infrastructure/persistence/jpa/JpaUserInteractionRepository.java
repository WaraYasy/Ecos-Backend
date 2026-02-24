package es.wara.ecos.backend.infrastructure.persistence.jpa;

import es.wara.ecos.backend.infrastructure.persistence.entity.UserInteractionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaUserInteractionRepository extends JpaRepository<UserInteractionEntity, Long> {
}