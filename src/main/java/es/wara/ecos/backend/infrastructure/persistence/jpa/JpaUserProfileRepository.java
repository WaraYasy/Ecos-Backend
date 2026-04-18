package es.wara.ecos.backend.infrastructure.persistence.jpa;

import es.wara.ecos.backend.infrastructure.persistence.entity.UserProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repositorio JPA para la tabla {@code user_profiles}.
 * <p>
 * Relación 1-1 con {@code users}: la PK de {@code user_profiles} es el mismo UUID del usuario
 * (columna {@code user_id}, mapeada con {@code @MapsId} en {@link UserProfileEntity}).
 * </p>
 *
 * @author Wara Pacheco
 * @since 1.0.0
 */
public interface JpaUserProfileRepository extends JpaRepository<UserProfileEntity, UUID> {
}