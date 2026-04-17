package es.wara.ecos.backend.infrastructure.persistence.jpa;

import es.wara.ecos.backend.infrastructure.persistence.entity.SongEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
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
 * @version 1.2
 * @since 1.0.0
 */
public interface JpaSongRepository extends JpaRepository<SongEntity, UUID>,
        JpaSpecificationExecutor<SongEntity> {

    Optional<SongEntity> findBySpotifyId(String spotifyId);

    Optional<SongEntity> findByIsrc(String isrc);

    List<SongEntity> findByTitleContainingIgnoreCase(String title);

    // DISTINCT evita duplicados cuando una canción tiene varios artistas (relación N:M)
    @Query("SELECT DISTINCT s FROM SongEntity s LEFT JOIN FETCH s.artists WHERE s.album IS NULL")
    List<SongEntity> findWithoutAlbum();

    // @EntityGraph en vez de FETCH JOIN: permite paginación correcta en colecciones @ManyToMany
    @EntityGraph(attributePaths = "artists")
    @Query("SELECT s FROM SongEntity s WHERE s.previewUrl IS NULL")
    Page<SongEntity> findWithoutPreview(Pageable pageable);

    @EntityGraph(attributePaths = "artists")
    @Query("SELECT s FROM SongEntity s WHERE s.tags IS EMPTY")
    Page<SongEntity> findWithoutTags(Pageable pageable);

    // album es @ManyToOne → FETCH JOIN sí es compatible con paginación
    @Query("SELECT s FROM SongEntity s LEFT JOIN FETCH s.album a WHERE s.spotifyId IS NOT NULL AND (a IS NULL OR a.coverUrl IS NULL)")
    Page<SongEntity> findForCoverEnrichment(Pageable pageable);

}
