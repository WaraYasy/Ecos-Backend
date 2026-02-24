package es.wara.ecos.backend.infrastructure.persistence.mapper;

import es.wara.ecos.backend.domain.model.Playlist;
import es.wara.ecos.backend.infrastructure.persistence.entity.PlaylistEntity;
import org.springframework.stereotype.Component;

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
        return entity;
    }

}