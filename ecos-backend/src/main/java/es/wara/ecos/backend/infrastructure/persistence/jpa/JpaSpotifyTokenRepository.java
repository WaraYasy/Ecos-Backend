package es.wara.ecos.backend.infrastructure.persistence.jpa;

import es.wara.ecos.backend.infrastructure.persistence.entity.SpotifyTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaSpotifyTokenRepository extends JpaRepository<SpotifyTokenEntity, Long> {
}
