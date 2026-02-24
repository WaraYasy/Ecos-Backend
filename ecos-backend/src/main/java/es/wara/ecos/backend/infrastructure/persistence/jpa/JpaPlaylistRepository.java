package es.wara.ecos.backend.infrastructure.persistence.jpa;

import es.wara.ecos.backend.infrastructure.persistence.entity.PlaylistEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaPlaylistRepository extends JpaRepository<PlaylistEntity, Long> {

    List<PlaylistEntity> findByUser_Id(Long userId);

}