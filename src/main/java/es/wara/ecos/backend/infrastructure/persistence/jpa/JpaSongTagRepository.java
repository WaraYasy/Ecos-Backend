package es.wara.ecos.backend.infrastructure.persistence.jpa;

import es.wara.ecos.backend.infrastructure.persistence.entity.SongTagEntity;
import es.wara.ecos.backend.infrastructure.persistence.entity.SongTagId;
import org.springframework.data.jpa.repository.JpaRepository;

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

}
