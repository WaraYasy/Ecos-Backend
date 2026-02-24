package es.wara.ecos.backend.infrastructure.persistence.jpa;

import es.wara.ecos.backend.infrastructure.persistence.entity.AudioFeaturesEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data JPA para AudioFeaturesEntity.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
public interface JpaAudioFeaturesRepository extends JpaRepository<AudioFeaturesEntity, Long> {
}
