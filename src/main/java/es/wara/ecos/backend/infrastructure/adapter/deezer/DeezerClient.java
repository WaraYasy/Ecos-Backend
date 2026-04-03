package es.wara.ecos.backend.infrastructure.adapter.deezer;

import es.wara.ecos.backend.domain.model.Album;
import es.wara.ecos.backend.domain.model.Artist;
import es.wara.ecos.backend.domain.model.Song;
import es.wara.ecos.backend.infrastructure.adapter.deezer.dto.DeezerSearchResponseDTO;
import es.wara.ecos.backend.infrastructure.adapter.deezer.dto.DeezerTrackDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Optional;

/**
 * Cliente HTTP para la Deezer Search API (sin autenticación).
 *
 * <p>Deezer devuelve JSON con {@code Content-Type: application/json}, por lo que
 * no necesita ningún workaround de deserialización. Las URLs de preview son MP3
 * directos de 30 segundos, siempre presentes en los resultados de búsqueda.</p>
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Slf4j
@Component
public class DeezerClient {

    private static final String BASE_URL = "https://api.deezer.com";

    private final RestTemplate restTemplate;

    public DeezerClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Busca un track en Deezer por título y artista.
     *
     * @param title  título de la canción
     * @param artist nombre del artista (puede ser vacío)
     * @return el primer resultado mapeado a {@link Song}, o vacío si no se encontró
     */
    public Optional<Song> searchTrack(String title, String artist) {
        try {
            StringBuilder q = new StringBuilder("track:\"").append(title).append("\"");
            if (artist != null && !artist.isBlank()) {
                q.append(" artist:\"").append(artist).append("\"");
            }

            String url = UriComponentsBuilder.fromUriString(BASE_URL + "/search")
                    .queryParam("q", q.toString())
                    .queryParam("limit", 1)
                    .build().toUriString();

            DeezerSearchResponseDTO response = restTemplate.getForObject(url, DeezerSearchResponseDTO.class);

            if (response == null || response.getData() == null || response.getData().isEmpty()) {
                log.debug("Deezer: sin resultados para '{} - {}'", artist, title);
                return Optional.empty();
            }

            return Optional.of(mapToSong(response.getData().get(0)));

        } catch (Exception e) {
            log.debug("Deezer: error buscando '{} - {}': {}", artist, title, e.getMessage());
            return Optional.empty();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    private Song mapToSong(DeezerTrackDTO track) {
        Song song = new Song();
        song.setTitle(track.getTitle());
        song.setDurationMs(track.getDuration() != null ? track.getDuration() * 1000 : null);
        // previewUrl no se asigna desde Deezer — el CDN de Deezer bloquea AVFoundation (iOS).
        // MusicAggregationService obtiene el preview de iTunes como fallback.
        song.setIsrc(track.getIsrc());

        if (track.getArtist() != null && track.getArtist().getName() != null) {
            Artist artist = new Artist();
            artist.setName(track.getArtist().getName());
            song.setArtists(List.of(artist));
        }

        if (track.getAlbum() != null && track.getAlbum().getTitle() != null) {
            Album album = new Album();
            album.setTitle(track.getAlbum().getTitle());
            album.setCoverUrl(track.getAlbum().getCoverMedium());
            song.setAlbum(album);
        }

        return song;
    }
}
