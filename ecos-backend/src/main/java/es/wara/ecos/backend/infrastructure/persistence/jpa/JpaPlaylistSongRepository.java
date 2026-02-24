package es.wara.ecos.backend.infrastructure.persistence.jpa;

import es.wara.ecos.backend.infrastructure.persistence.entity.PlaylistSongEntity;
import es.wara.ecos.backend.infrastructure.persistence.entity.PlaylistSongEntity.PlaylistSongId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaPlaylistSongRepository extends JpaRepository<PlaylistSongEntity, PlaylistSongId> {

    List<PlaylistSongEntity> findById_PlaylistIdOrderByPosition(Long playlistId);

}