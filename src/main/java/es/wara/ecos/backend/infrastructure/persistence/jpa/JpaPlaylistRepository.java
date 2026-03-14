package es.wara.ecos.backend.infrastructure.persistence.jpa;

import es.wara.ecos.backend.infrastructure.persistence.entity.PlaylistEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio Spring Data JPA para PlaylistEntity; permite obtener todas las playlists de un usuario.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
public interface JpaPlaylistRepository extends JpaRepository<PlaylistEntity, UUID> {

    List<PlaylistEntity> findByUser_Id(UUID userId);

}
