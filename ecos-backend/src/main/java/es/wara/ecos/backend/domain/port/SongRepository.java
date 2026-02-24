package es.wara.ecos.backend.domain.port;

import es.wara.ecos.backend.domain.model.Song;

import java.util.List;
import java.util.Optional;

public interface SongRepository {

    Optional<Song> findById(Long id);

    Optional<Song> findBySpotifyId(String spotifyId);

    Song save(Song song);

    List<Song> findByTitleContaining(String title);

}