package es.wara.ecos.backend.infrastructure.persistence.jpa;

import es.wara.ecos.backend.infrastructure.persistence.entity.AudioFeaturesEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaAudioFeaturesRepository extends JpaRepository<AudioFeaturesEntity, Long> {
}