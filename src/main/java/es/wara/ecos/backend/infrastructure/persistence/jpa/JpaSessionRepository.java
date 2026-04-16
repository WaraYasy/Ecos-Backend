package es.wara.ecos.backend.infrastructure.persistence.jpa;

import es.wara.ecos.backend.infrastructure.persistence.entity.ExplorationSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio Spring Data JPA para ExplorationSessionEntity.
 *
 * <p>La consulta {@link #findByUser_IdWithNodes} usa {@code JOIN FETCH} para cargar
 * en una sola query la sesión, sus nodos y las canciones con su álbum.
 * Las colecciones de la canción (artistas, etiquetas, películas, audioFeatures)
 * se resuelven después mediante {@code @BatchSize}, evitando N+1 sin necesidad
 * de JOIN FETCH adicionales (que provocarían {@code MultipleBagFetchException}).</p>
 *
 * @author Wara Pacheco
 * @version 1.1
 * @since 1.0.0
 */
public interface JpaSessionRepository extends JpaRepository<ExplorationSessionEntity, UUID> {

    /**
     * Carga sesiones de un usuario con nodos, canciones y álbumes en una sola query.
     * Usar en lugar de {@code findByUser_Id} para evitar lazy loading secuencial.
     */
    @Query("""
            SELECT DISTINCT s FROM ExplorationSessionEntity s
            LEFT JOIN FETCH s.nodes n
            LEFT JOIN FETCH n.song song
            LEFT JOIN FETCH song.album
            WHERE s.user.id = :userId
            """)
    List<ExplorationSessionEntity> findByUser_IdWithNodes(@Param("userId") UUID userId);

    /**
     * Carga una sesión por id con sus nodos, canciones y álbum en una sola query.
     * Usar en lugar de {@code findById} cuando se necesitan los nodos de la sesión.
     */
    @Query("""
            SELECT DISTINCT s FROM ExplorationSessionEntity s
            LEFT JOIN FETCH s.nodes n
            LEFT JOIN FETCH n.song song
            LEFT JOIN FETCH song.album
            WHERE s.id = :id
            """)
    Optional<ExplorationSessionEntity> findByIdWithNodes(@Param("id") UUID id);

    long countByUser_Id(UUID userId);

}
