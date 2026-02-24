package es.wara.ecos.backend.infrastructure.adapter;

import es.wara.ecos.backend.domain.model.AudioFeatures;
import es.wara.ecos.backend.domain.model.Song;
import es.wara.ecos.backend.domain.model.Tag;
import es.wara.ecos.backend.domain.port.MusicProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Implementación provisional del puerto MusicProvider que devuelve resultados vacíos.
 * <p>
 * Sustituir por el adaptador real de Spotify + Last.fm en la tarea 2.9 y 2.11.
 * </p>
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Component
public class StubMusicProvider implements MusicProvider {

    @Override
    public List<Song> searchSongs(String query, int limit) {
        // TODO (tarea 2.9): implementar con SpotifyClient
        return List.of();
    }

    @Override
    public Optional<AudioFeatures> getAudioFeatures(String spotifyId) {
        // TODO (tarea 2.9): implementar con SpotifyClient
        return Optional.empty();
    }

    @Override
    public List<Song> getSimilarSongs(String artist, String title, int limit) {
        // TODO (tarea 2.11): implementar con LastFmClient
        return List.of();
    }

    @Override
    public List<Tag> getSongTags(String artist, String title) {
        // TODO (tarea 2.11): implementar con LastFmClient
        return List.of();
    }

}
