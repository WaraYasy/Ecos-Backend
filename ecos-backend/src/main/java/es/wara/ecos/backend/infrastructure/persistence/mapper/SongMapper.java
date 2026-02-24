package es.wara.ecos.backend.infrastructure.persistence.mapper;

import es.wara.ecos.backend.domain.model.Artist;
import es.wara.ecos.backend.domain.model.Album;
import es.wara.ecos.backend.domain.model.Song;
import es.wara.ecos.backend.infrastructure.persistence.entity.ArtistEntity;
import es.wara.ecos.backend.infrastructure.persistence.entity.AlbumEntity;
import es.wara.ecos.backend.infrastructure.persistence.entity.SongEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper que convierte entre SongEntity y el modelo de dominio Song, incluyendo Album, Artist, AudioFeatures y Tags.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Component
public class SongMapper {

    @Autowired
    private AudioFeaturesMapper audioFeaturesMapper;

    @Autowired
    private TagMapper tagMapper;

    public Song toModel(SongEntity entity) {
        if (entity == null) return null;
        Song song = new Song();
        song.setId(entity.getId());
        song.setSpotifyId(entity.getSpotifyId());
        song.setTitle(entity.getTitle());
        song.setDurationMs(entity.getDurationMs());
        song.setPreviewUrl(entity.getPreviewUrl());
        song.setCreatedAt(entity.getCreatedAt());
        if (entity.getAlbum() != null) {
            song.setAlbum(toAlbumModel(entity.getAlbum()));
        }
        if (entity.getArtists() != null) {
            song.setArtists(entity.getArtists().stream().map(this::toArtistModel).toList());
        }
        if (entity.getAudioFeatures() != null) {
            song.setAudioFeatures(audioFeaturesMapper.toModel(entity.getAudioFeatures()));
        }
        if (entity.getTags() != null) {
            song.setTags(entity.getTags().stream().map(tagMapper::toModel).toList());
        }
        return song;
    }

    public SongEntity toEntity(Song model) {
        if (model == null) return null;
        SongEntity entity = new SongEntity();
        entity.setId(model.getId());
        entity.setSpotifyId(model.getSpotifyId());
        entity.setTitle(model.getTitle());
        entity.setDurationMs(model.getDurationMs());
        entity.setPreviewUrl(model.getPreviewUrl());
        entity.setCreatedAt(model.getCreatedAt());
        return entity;
    }

    private Album toAlbumModel(AlbumEntity entity) {
        return new Album(
                entity.getId(),
                entity.getSpotifyId(),
                entity.getTitle(),
                entity.getYear() != null ? entity.getYear().intValue() : null,
                entity.getCoverUrl(),
                entity.getArtist() != null ? entity.getArtist().getId() : null
        );
    }

    private Artist toArtistModel(ArtistEntity entity) {
        return new Artist(
                entity.getId(),
                entity.getSpotifyId(),
                entity.getName(),
                entity.getCountry()
        );
    }

}
