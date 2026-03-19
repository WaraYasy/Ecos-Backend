package es.wara.ecos.backend.infrastructure.persistence.jpa;

import es.wara.ecos.backend.infrastructure.persistence.entity.AlbumEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio Spring Data JPA para AlbumEntity.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
public interface JpaAlbumRepository extends JpaRepository<AlbumEntity, UUID> {

    Optional<AlbumEntity> findBySpotifyId(String spotifyId);

}