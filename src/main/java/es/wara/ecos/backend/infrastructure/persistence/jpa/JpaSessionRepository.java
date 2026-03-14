package es.wara.ecos.backend.infrastructure.persistence.jpa;

import es.wara.ecos.backend.infrastructure.persistence.entity.ExplorationSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio Spring Data JPA para ExplorationSessionEntity; permite obtener todas las sesiones de un usuario.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
public interface JpaSessionRepository extends JpaRepository<ExplorationSessionEntity, UUID> {

    List<ExplorationSessionEntity> findByUser_Id(UUID userId);

}
