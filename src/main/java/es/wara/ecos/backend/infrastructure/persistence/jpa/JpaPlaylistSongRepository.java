package es.wara.ecos.backend.infrastructure.persistence.jpa;

import es.wara.ecos.backend.infrastructure.persistence.entity.PlaylistSongEntity;
import es.wara.ecos.backend.infrastructure.persistence.entity.PlaylistSongEntity.PlaylistSongId;
import org.springframework.data.jpa.repository.JpaRepository;

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

    List<PlaylistSongEntity> findById_PlaylistIdOrderByPosition(UUID playlistId);

    int countById_PlaylistId(UUID playlistId);

}
