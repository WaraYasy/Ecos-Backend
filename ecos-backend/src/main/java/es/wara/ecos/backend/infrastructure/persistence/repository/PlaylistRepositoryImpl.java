package es.wara.ecos.backend.infrastructure.persistence.repository;

import es.wara.ecos.backend.domain.model.Playlist;
import es.wara.ecos.backend.domain.port.PlaylistRepository;
import es.wara.ecos.backend.infrastructure.persistence.entity.PlaylistSongEntity;
import es.wara.ecos.backend.infrastructure.persistence.entity.PlaylistSongEntity.PlaylistSongId;
import es.wara.ecos.backend.infrastructure.persistence.entity.SongEntity;
import es.wara.ecos.backend.infrastructure.persistence.entity.PlaylistEntity;
import es.wara.ecos.backend.infrastructure.persistence.jpa.JpaPlaylistRepository;
import es.wara.ecos.backend.infrastructure.persistence.jpa.JpaPlaylistSongRepository;
import es.wara.ecos.backend.infrastructure.persistence.mapper.PlaylistMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Implementación del puerto de dominio PlaylistRepository usando JPA.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Repository
public class PlaylistRepositoryImpl implements PlaylistRepository {

    private final JpaPlaylistRepository jpa;
    private final JpaPlaylistSongRepository jpaPlaylistSong;
    private final PlaylistMapper mapper;

    public PlaylistRepositoryImpl(JpaPlaylistRepository jpa,
                                  JpaPlaylistSongRepository jpaPlaylistSong,
                                  PlaylistMapper mapper) {
        this.jpa = jpa;
        this.jpaPlaylistSong = jpaPlaylistSong;
        this.mapper = mapper;
    }

    @Override
    public Optional<Playlist> findById(Long id) {
        return jpa.findById(id).map(mapper::toModel);
    }

    @Override
    public List<Playlist> findByUserId(Long userId) {
        return jpa.findByUser_Id(userId).stream()
                .map(mapper::toModel)
                .toList();
    }

    @Override
    public Playlist save(Playlist playlist) {
        return mapper.toModel(jpa.save(mapper.toEntity(playlist)));
    }

    @Override
    public void addSong(Long playlistId, Long songId, int position) {
        PlaylistEntity playlist = new PlaylistEntity();
        playlist.setId(playlistId);
        SongEntity song = new SongEntity();
        song.setId(songId);

        PlaylistSongEntity entry = new PlaylistSongEntity();
        entry.setId(new PlaylistSongId(playlistId, songId));
        entry.setPlaylist(playlist);
        entry.setSong(song);
        entry.setPosition((short) position);
        jpaPlaylistSong.save(entry);
    }

    @Override
    public void delete(Long id) {
        jpa.deleteById(id);
    }

    @Override
    public void removeSong(Long playlistId, Long songId) {
        jpaPlaylistSong.deleteById(new PlaylistSongId(playlistId, songId));
    }

}
