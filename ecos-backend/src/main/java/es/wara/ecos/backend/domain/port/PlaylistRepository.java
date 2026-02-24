package es.wara.ecos.backend.domain.port;

import es.wara.ecos.backend.domain.model.Playlist;

import java.util.List;
import java.util.Optional;

public interface PlaylistRepository {

    Optional<Playlist> findById(Long id);

    List<Playlist> findByUserId(Long userId);

    Playlist save(Playlist playlist);

    void addSong(Long playlistId, Long songId, int position);

}