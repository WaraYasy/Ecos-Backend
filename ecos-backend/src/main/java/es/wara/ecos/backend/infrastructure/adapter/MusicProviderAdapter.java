package es.wara.ecos.backend.infrastructure.adapter;

import es.wara.ecos.backend.domain.model.Album;
import es.wara.ecos.backend.domain.model.Artist;
import es.wara.ecos.backend.domain.model.AudioFeatures;
import es.wara.ecos.backend.domain.model.Song;
import es.wara.ecos.backend.domain.model.Tag;
import es.wara.ecos.backend.domain.port.MusicProvider;
import es.wara.ecos.backend.infrastructure.adapter.lastfm.LastFmClient;
import es.wara.ecos.backend.infrastructure.adapter.lastfm.dto.LastFmSimilarTracksDTO;
import es.wara.ecos.backend.infrastructure.adapter.lastfm.dto.LastFmTagDTO;
import es.wara.ecos.backend.infrastructure.adapter.lastfm.dto.LastFmTrackInfoDTO;
import es.wara.ecos.backend.infrastructure.adapter.spotify.SpotifyClient;
import es.wara.ecos.backend.infrastructure.adapter.spotify.dto.SpotifyAudioFeaturesDTO;
import es.wara.ecos.backend.infrastructure.adapter.spotify.dto.SpotifySearchResponseDTO;
import es.wara.ecos.backend.infrastructure.adapter.spotify.dto.SpotifyTrackDTO;
import es.wara.ecos.backend.infrastructure.exception.ExternalApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adaptador que implementa el puerto {@link MusicProvider} combinando Spotify y Last.fm.
 * <p>
 * Responsabilidades por fuente:
 * <ul>
 *   <li><b>Spotify</b>: búsqueda de tracks, audio features, datos de álbum/artista</li>
 *   <li><b>Last.fm</b>: tags de género/mood/estética, tracks similares</li>
 * </ul>
 * </p>
 * <p>
 * Ante errores de API externa, los métodos devuelven resultados vacíos para
 * garantizar degradación elegante del sistema.
 * </p>
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Slf4j
@Component
public class MusicProviderAdapter implements MusicProvider {

    private final SpotifyClient spotifyClient;
    private final LastFmClient lastFmClient;

    public MusicProviderAdapter(SpotifyClient spotifyClient, LastFmClient lastFmClient) {
        this.spotifyClient = spotifyClient;
        this.lastFmClient = lastFmClient;
    }

    /**
     * Busca canciones en Spotify que coincidan con la query.
     * <p>
     * Mapea los tracks de Spotify al modelo de dominio {@link Song}.
     * Si Spotify no devuelve resultados o falla, retorna lista vacía.
     * </p>
     *
     * @param query texto de búsqueda (título, artista, álbum o combinación)
     * @param limit número máximo de resultados
     * @return lista de canciones del dominio; vacía si no hay resultados o hay error
     */
    @Override
    public List<Song> searchSongs(String query, int limit) {
        try {
            SpotifySearchResponseDTO response = spotifyClient.searchTracks(query, limit);

            if (response == null || response.getTracks() == null
                    || response.getTracks().getItems() == null) {
                return List.of();
            }

            return response.getTracks().getItems().stream()
                    .map(this::mapTrackToSong)
                    .collect(Collectors.toList());

        } catch (ExternalApiException e) {
            log.warn("Error al buscar canciones en Spotify (query='{}'), devolviendo vacío: {}", query, e.getMessage());
            return List.of();
        }
    }

    /**
     * Obtiene las características de audio de una canción por su Spotify ID.
     * <p>
     * Mapea el {@link SpotifyAudioFeaturesDTO} al modelo de dominio {@link AudioFeatures}.
     * </p>
     *
     * @param spotifyId ID del track en Spotify
     * @return audio features si existen; {@link Optional#empty()} si no hay datos o hay error
     */
    @Override
    public Optional<AudioFeatures> getAudioFeatures(String spotifyId) {
        try {
            SpotifyAudioFeaturesDTO dto = spotifyClient.getTrackAudioFeatures(spotifyId);

            if (dto == null) {
                return Optional.empty();
            }

            return Optional.of(mapToAudioFeatures(dto));

        } catch (ExternalApiException e) {
            log.warn("Error al obtener audio features de Spotify (id='{}'), devolviendo vacío: {}", spotifyId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Obtiene canciones similares usando Last.fm y enriquece cada una con datos de Spotify.
     * <p>
     * Flujo: Last.fm devuelve la lista de tracks similares → para cada uno se busca
     * en Spotify para obtener el Spotify ID, preview URL y audio features.
     * Si Last.fm no tiene datos para la canción, devuelve lista vacía.
     * </p>
     *
     * @param artist nombre del artista de referencia
     * @param title  título de la canción de referencia
     * @param limit  número máximo de similares a devolver
     * @return lista de canciones similares; vacía si no hay datos o hay error
     */
    @Override
    public List<Song> getSimilarSongs(String artist, String title, int limit) {
        try {
            LastFmSimilarTracksDTO similarDto = lastFmClient.getSimilarTracks(artist, title, limit);

            if (similarDto == null || similarDto.getSimilartracks() == null
                    || similarDto.getSimilartracks().getTrack() == null) {
                return List.of();
            }

            List<Song> result = new ArrayList<>();

            for (LastFmSimilarTracksDTO.SimilarTrack similar : similarDto.getSimilartracks().getTrack()) {
                if (result.size() >= limit) break;

                String trackName = similar.getName();
                String artistName = similar.getArtist() != null ? similar.getArtist().getName() : "";

                // Buscar en Spotify para enriquecer con spotifyId, previewUrl, etc.
                try {
                    String query = trackName + " " + artistName;
                    SpotifySearchResponseDTO spotifyResult = spotifyClient.searchTracks(query, 1);

                    if (spotifyResult != null && spotifyResult.getTracks() != null
                            && spotifyResult.getTracks().getItems() != null
                            && !spotifyResult.getTracks().getItems().isEmpty()) {
                        result.add(mapTrackToSong(spotifyResult.getTracks().getItems().get(0)));
                    }
                } catch (ExternalApiException spotifyEx) {
                    // Si Spotify falla para este track, crear song básica con datos de Last.fm
                    log.debug("No se encontró '{}' en Spotify, creando canción básica", trackName);
                    Song basicSong = new Song();
                    basicSong.setTitle(trackName);
                    Artist basicArtist = new Artist();
                    basicArtist.setName(artistName);
                    basicSong.setArtists(List.of(basicArtist));
                    result.add(basicSong);
                }
            }

            return result;

        } catch (ExternalApiException e) {
            log.warn("Error al obtener similares de Last.fm ('{} - {}'), devolviendo vacío: {}", artist, title, e.getMessage());
            return List.of();
        }
    }

    /**
     * Obtiene los tags (géneros, moods, estéticas) de una canción desde Last.fm.
     * <p>
     * Los tags se normalizan: nombre en minúsculas y sin espacios al margen.
     * El peso se normaliza de la escala 0-100 de Last.fm a 0.0-1.0.
     * </p>
     *
     * @param artist nombre del artista
     * @param title  título de la canción
     * @return lista de tags normalizados; vacía si no hay datos o hay error
     */
    @Override
    public List<Tag> getSongTags(String artist, String title) {
        try {
            LastFmTrackInfoDTO trackInfo = lastFmClient.getTrackTags(artist, title);

            if (trackInfo == null || trackInfo.getToptags() == null
                    || trackInfo.getToptags().getTag() == null) {
                return List.of();
            }

            return trackInfo.getToptags().getTag().stream()
                    .map(this::mapToTag)
                    .collect(Collectors.toList());

        } catch (ExternalApiException e) {
            log.warn("Error al obtener tags de Last.fm ('{} - {}'), devolviendo vacío: {}", artist, title, e.getMessage());
            return List.of();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MAPPERS INTERNOS
    // ─────────────────────────────────────────────────────────────────────────

    /** Convierte un {@link SpotifyTrackDTO} al modelo de dominio {@link Song}. */
    private Song mapTrackToSong(SpotifyTrackDTO dto) {
        // Artistas
        List<Artist> artists = List.of();
        if (dto.getArtists() != null) {
            artists = dto.getArtists().stream()
                    .map(a -> {
                        Artist artist = new Artist();
                        artist.setSpotifyId(a.getId());
                        artist.setName(a.getName());
                        return artist;
                    })
                    .collect(Collectors.toList());
        }

        // Álbum
        Album album = null;
        if (dto.getAlbum() != null) {
            album = new Album();
            album.setSpotifyId(dto.getAlbum().getId());
            album.setTitle(dto.getAlbum().getName());

            // Extraer año del release_date (puede ser "2023-01-15", "2023-01" o "2023")
            String releaseDate = dto.getAlbum().getReleaseDate();
            if (releaseDate != null && releaseDate.length() >= 4) {
                try {
                    album.setYear(Integer.parseInt(releaseDate.substring(0, 4)));
                } catch (NumberFormatException ignored) {
                    // Si el año no es parseable, dejarlo en null
                }
            }

            // Portada: primera imagen (mayor resolución)
            if (dto.getAlbum().getImages() != null && !dto.getAlbum().getImages().isEmpty()) {
                album.setCoverUrl(dto.getAlbum().getImages().get(0).getUrl());
            }
        }

        Song song = new Song();
        song.setSpotifyId(dto.getId());
        song.setTitle(dto.getName());
        song.setDurationMs(dto.getDurationMs());
        song.setPreviewUrl(dto.getPreviewUrl());
        song.setAlbum(album);
        song.setArtists(artists);

        return song;
    }

    /** Convierte un {@link SpotifyAudioFeaturesDTO} al modelo de dominio {@link AudioFeatures}. */
    private AudioFeatures mapToAudioFeatures(SpotifyAudioFeaturesDTO dto) {
        AudioFeatures af = new AudioFeatures();
        af.setEnergy(toBigDecimal(dto.getEnergy()));
        af.setTempo(toBigDecimal(dto.getTempo()));
        af.setValence(toBigDecimal(dto.getValence()));
        af.setDanceability(toBigDecimal(dto.getDanceability()));
        af.setAcousticness(toBigDecimal(dto.getAcousticness()));
        af.setInstrumentalness(toBigDecimal(dto.getInstrumentalness()));
        af.setSpeechiness(toBigDecimal(dto.getSpeechiness()));
        af.setLoudness(toBigDecimal(dto.getLoudness()));
        af.setMusicalKey(dto.getKey());
        af.setMode(dto.getMode());
        af.setTimeSignature(dto.getTimeSignature());
        return af;
    }

    /** Convierte un {@link LastFmTagDTO} al modelo de dominio {@link Tag}. */
    private Tag mapToTag(LastFmTagDTO dto) {
        Tag tag = new Tag();
        // Normalizar nombre: minúsculas y sin espacios al margen
        tag.setName(dto.getName() != null ? dto.getName().toLowerCase().trim() : "");
        // Normalizar peso de escala 0-100 a 0.0-1.0
        tag.setWeight(dto.getCount() != null
                ? BigDecimal.valueOf(dto.getCount() / 100.0)
                : BigDecimal.ZERO);
        return tag;
    }

    /** Convierte Double a BigDecimal de forma segura. */
    private BigDecimal toBigDecimal(Double value) {
        return value != null ? BigDecimal.valueOf(value) : null;
    }
}