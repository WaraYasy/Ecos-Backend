package es.wara.ecos.backend.infrastructure.persistence.jpa;

import es.wara.ecos.backend.infrastructure.persistence.entity.ArtistEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JpaArtistRepository extends JpaRepository<ArtistEntity, UUID> {

    Optional<ArtistEntity> findBySpotifyId(String spotifyId);

    Optional<ArtistEntity> findByNameIgnoreCase(String name);

}