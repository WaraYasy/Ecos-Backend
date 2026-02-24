package es.wara.ecos.backend.infrastructure.persistence.jpa;

import es.wara.ecos.backend.infrastructure.persistence.entity.SongTagEntity;
import es.wara.ecos.backend.infrastructure.persistence.entity.SongTagId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaSongTagRepository extends JpaRepository<SongTagEntity, SongTagId> {

    List<SongTagEntity> findById_SongId(Long songId);

}