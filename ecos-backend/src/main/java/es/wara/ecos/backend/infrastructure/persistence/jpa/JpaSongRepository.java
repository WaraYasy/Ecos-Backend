package es.wara.ecos.backend.infrastructure.persistence.jpa;

import es.wara.ecos.backend.infrastructure.persistence.entity.SongEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaSongRepository extends JpaRepository<SongEntity, Long> {

    Optional<SongEntity> findBySpotifyId(String spotifyId);

    List<SongEntity> findByTitleContainingIgnoreCase(String title);

}