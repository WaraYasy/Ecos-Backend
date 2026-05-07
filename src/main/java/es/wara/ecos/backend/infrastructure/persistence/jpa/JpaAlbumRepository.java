package es.wara.ecos.backend.infrastructure.persistence.jpa;

import es.wara.ecos.backend.infrastructure.persistence.entity.AlbumEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio Spring Data JPA para AlbumEntity.
 *
 * @author Wara Pacheco
 * @version 1.1
 * @since 1.0.0
 */
public interface JpaAlbumRepository extends JpaRepository<AlbumEntity, UUID> {

    Optional<AlbumEntity> findBySpotifyId(String spotifyId);

    Optional<AlbumEntity> findByCoverUrl(String coverUrl);

    List<AlbumEntity> findByArtistIsNull();

    Optional<AlbumEntity> findFirstByTitleIgnoreCase(String title);

    /** Devuelve álbumes sin ninguna canción asociada; usados por el job de limpieza de BD. */
    @Query("SELECT a FROM AlbumEntity a WHERE NOT EXISTS (SELECT s FROM SongEntity s WHERE s.album = a)")
    List<AlbumEntity> findEmpty();

}