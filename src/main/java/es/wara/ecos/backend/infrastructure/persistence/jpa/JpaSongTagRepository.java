package es.wara.ecos.backend.infrastructure.persistence.jpa;

import es.wara.ecos.backend.infrastructure.persistence.entity.SongTagEntity;
import es.wara.ecos.backend.infrastructure.persistence.entity.SongTagId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio Spring Data JPA para SongTagEntity con clave compuesta SongTagId; permite buscar tags por canción.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
public interface JpaSongTagRepository extends JpaRepository<SongTagEntity, SongTagId> {

    List<SongTagEntity> findById_SongId(UUID songId);

    @Query("SELECT DISTINCT t.id.tag FROM SongTagEntity t WHERE t.type = :type AND LOWER(t.id.tag) LIKE LOWER(CONCAT('%', :q, '%')) ORDER BY t.id.tag")
    List<String> findDistinctTagsByTypeContaining(@Param("type") String type, @Param("q") String q);

}
