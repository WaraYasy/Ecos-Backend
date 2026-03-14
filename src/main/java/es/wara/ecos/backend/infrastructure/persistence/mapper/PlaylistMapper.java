package es.wara.ecos.backend.infrastructure.persistence.mapper;

import es.wara.ecos.backend.domain.model.Playlist;
import es.wara.ecos.backend.infrastructure.persistence.entity.PlaylistEntity;
import es.wara.ecos.backend.infrastructure.persistence.entity.UserEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper que convierte entre PlaylistEntity y el modelo de dominio Playlist.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Component
public class PlaylistMapper {

    public Playlist toModel(PlaylistEntity entity) {
        if (entity == null) return null;
        Playlist playlist = new Playlist();
        playlist.setId(entity.getId());
        playlist.setUserId(entity.getUser() != null ? entity.getUser().getId() : null);
        playlist.setName(entity.getName());
        playlist.setDescription(entity.getDescription());
        playlist.setSpotifyExportId(entity.getSpotifyExportId());
        playlist.setCreatedAt(entity.getCreatedAt());
        return playlist;
    }

    public PlaylistEntity toEntity(Playlist model) {
        if (model == null) return null;
        PlaylistEntity entity = new PlaylistEntity();
        entity.setId(model.getId());
        entity.setName(model.getName());
        entity.setDescription(model.getDescription());
        entity.setSpotifyExportId(model.getSpotifyExportId());
        entity.setCreatedAt(model.getCreatedAt());
        if (model.getUserId() != null) {
            UserEntity user = new UserEntity();
            user.setId(model.getUserId());
            entity.setUser(user);
        }
        return entity;
    }

}
