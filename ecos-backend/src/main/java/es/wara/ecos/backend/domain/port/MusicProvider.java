package es.wara.ecos.backend.domain.port;

import es.wara.ecos.backend.domain.model.AudioFeatures;
import es.wara.ecos.backend.domain.model.Song;
import es.wara.ecos.backend.domain.model.Tag;

import java.util.List;
import java.util.Optional;

public interface MusicProvider {

    List<Song> searchSongs(String query, int limit);

    Optional<AudioFeatures> getAudioFeatures(String spotifyId);

    List<Song> getSimilarSongs(String artist, String title, int limit);

    List<Tag> getSongTags(String artist, String title);

}