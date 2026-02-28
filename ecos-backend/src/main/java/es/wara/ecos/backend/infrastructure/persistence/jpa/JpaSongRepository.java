package es.wara.ecos.backend.infrastructure.persistence.jpa;

import es.wara.ecos.backend.infrastructure.persistence.entity.SongEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para SongEntity; permite buscar canciones por spotifyId,
 * por título y mediante especificaciones JPA para búsqueda multidimensional.
 *
 * @author Wara Pacheco
 * @version 1.1
 * @since 1.0.0
 */
public interface JpaSongRepository extends JpaRepository<SongEntity, Long>,
        JpaSpecificationExecutor<SongEntity> {

    Optional<SongEntity> findBySpotifyId(String spotifyId);

    List<SongEntity> findByTitleContainingIgnoreCase(String title);

}
