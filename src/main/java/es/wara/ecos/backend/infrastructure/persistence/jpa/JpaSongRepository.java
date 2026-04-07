package es.wara.ecos.backend.infrastructure.persistence.jpa;

import es.wara.ecos.backend.infrastructure.persistence.entity.SongEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio Spring Data JPA para SongEntity; permite buscar canciones por spotifyId,
 * por título y mediante especificaciones JPA para búsqueda multidimensional.
 *
 * @author Wara Pacheco
 * @version 1.1
 * @since 1.0.0
 */
public interface JpaSongRepository extends JpaRepository<SongEntity, UUID>,
        JpaSpecificationExecutor<SongEntity> {

    Optional<SongEntity> findBySpotifyId(String spotifyId);

    Optional<SongEntity> findByIsrc(String isrc);

    List<SongEntity> findByTitleContainingIgnoreCase(String title);

    @Query("SELECT s FROM SongEntity s LEFT JOIN FETCH s.artists WHERE s.previewUrl IS NULL")
    List<SongEntity> findWithoutPreview(Pageable pageable);

    @Query("SELECT DISTINCT s FROM SongEntity s LEFT JOIN FETCH s.artists WHERE s.tags IS EMPTY")
    List<SongEntity> findWithoutTags(Pageable pageable);

    @Query("SELECT s FROM SongEntity s LEFT JOIN FETCH s.album a WHERE s.spotifyId IS NOT NULL AND (a IS NULL OR a.coverUrl IS NULL)")
    List<SongEntity> findForCoverEnrichment(Pageable pageable);

}
