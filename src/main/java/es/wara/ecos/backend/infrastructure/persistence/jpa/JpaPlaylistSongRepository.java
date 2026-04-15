package es.wara.ecos.backend.infrastructure.persistence.jpa;

import es.wara.ecos.backend.infrastructure.persistence.entity.PlaylistSongEntity;
import es.wara.ecos.backend.infrastructure.persistence.entity.PlaylistSongEntity.PlaylistSongId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio Spring Data JPA para PlaylistSongEntity; permite obtener las canciones de una playlist ordenadas por posición.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
public interface JpaPlaylistSongRepository extends JpaRepository<PlaylistSongEntity, PlaylistSongId> {

    @Query("SELECT ps FROM PlaylistSongEntity ps LEFT JOIN FETCH ps.song WHERE ps.id.playlistId = :playlistId ORDER BY ps.position ASC")
    List<PlaylistSongEntity> findById_PlaylistIdOrderByPosition(@Param("playlistId") UUID playlistId);

    @Query("SELECT ps FROM PlaylistSongEntity ps LEFT JOIN FETCH ps.song WHERE ps.id.playlistId IN :playlistIds ORDER BY ps.position ASC")
    List<PlaylistSongEntity> findByPlaylistIdIn(@Param("playlistIds") List<UUID> playlistIds);

    int countById_PlaylistId(UUID playlistId);

}
