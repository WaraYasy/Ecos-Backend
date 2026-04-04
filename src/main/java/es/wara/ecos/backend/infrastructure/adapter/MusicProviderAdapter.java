package es.wara.ecos.backend.infrastructure.adapter;

import es.wara.ecos.backend.domain.model.Album;
import es.wara.ecos.backend.domain.model.Artist;
import es.wara.ecos.backend.domain.model.AudioFeatures;
import es.wara.ecos.backend.domain.model.Movie;
import es.wara.ecos.backend.domain.model.Song;
import es.wara.ecos.backend.domain.model.Tag;
import es.wara.ecos.backend.domain.port.MusicProvider;
import es.wara.ecos.backend.infrastructure.adapter.lastfm.LastFmClient;
import es.wara.ecos.backend.infrastructure.adapter.lastfm.dto.LastFmSimilarTracksDTO;
import es.wara.ecos.backend.infrastructure.adapter.lastfm.dto.LastFmTagDTO;
import es.wara.ecos.backend.infrastructure.adapter.lastfm.dto.LastFmTagTrackDTO;
import es.wara.ecos.backend.infrastructure.adapter.lastfm.dto.LastFmTrackInfoDTO;
import es.wara.ecos.backend.infrastructure.adapter.musicbrainz.MusicBrainzClient;
import es.wara.ecos.backend.infrastructure.adapter.musicbrainz.dto.MusicBrainzArtistCreditDTO;
import es.wara.ecos.backend.infrastructure.adapter.musicbrainz.dto.MusicBrainzMediumDTO;
import es.wara.ecos.backend.infrastructure.adapter.musicbrainz.dto.MusicBrainzReleaseDTO;
import es.wara.ecos.backend.infrastructure.adapter.musicbrainz.dto.MusicBrainzReleaseGroupDTO;
import es.wara.ecos.backend.infrastructure.adapter.musicbrainz.dto.MusicBrainzTrackDTO;
import es.wara.ecos.backend.infrastructure.adapter.spotify.SpotifyClient;
import es.wara.ecos.backend.infrastructure.adapter.spotify.dto.SpotifyAudioFeaturesDTO;
import es.wara.ecos.backend.infrastructure.adapter.spotify.dto.SpotifySearchResponseDTO;
import es.wara.ecos.backend.infrastructure.adapter.spotify.dto.SpotifyTrackDTO;
import es.wara.ecos.backend.infrastructure.adapter.tmdb.TmdbClient;
import es.wara.ecos.backend.infrastructure.adapter.tmdb.dto.TMDBCreditDTO;
import es.wara.ecos.backend.infrastructure.adapter.tmdb.dto.TMDBMovieDetailDTO;
import es.wara.ecos.backend.infrastructure.adapter.tmdb.dto.TMDBMultiResultDTO;
import es.wara.ecos.backend.infrastructure.adapter.tmdb.dto.TMDBMultiSearchResponseDTO;
import es.wara.ecos.backend.config.CacheConfig;
import es.wara.ecos.backend.infrastructure.adapter.deezer.DeezerClient;
import es.wara.ecos.backend.infrastructure.adapter.itunes.ItunesClient;
import es.wara.ecos.backend.infrastructure.exception.ExternalApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adaptador que implementa el puerto {@link MusicProvider} combinando las cuatro
 * fuentes externas de ECOS en dos ejes:
 *
 * <p><b>Eje musical:</b>
 * <ul>
 *   <li><b>Spotify</b>: búsqueda de tracks, audio features, datos de álbum/artista</li>
 *   <li><b>Last.fm</b>: tags de género/mood/estética, tracks similares</li>
 * </ul>
 * </p>
 *
 * <p><b>Eje cinematográfico:</b>
 * <ul>
 *   <li><b>MusicBrainz</b>: puente bidireccional OST — tracklist de película (Flujo A)
 *       y películas que contienen una grabación (Flujo B)</li>
 *   <li><b>TMDb</b>: metadatos de películas — título, año, director, géneros</li>
 * </ul>
 * </p>
 *
 * <p>Ante errores de API externa, todos los métodos devuelven resultados vacíos
 * para garantizar degradación elegante del sistema.</p>
 *
 * <p><b>Caché:</b> los métodos de consulta están anotados con {@link org.springframework.cache.annotation.Cacheable}
 * usando los TTL definidos en {@link es.wara.ecos.backend.config.CacheConfig}:
 * audio features (7 días), tags (6 h), películas (24 h), soundtracks (24 h), búsquedas (15 min).</p>
 *
 * @author Wara Pacheco
 * @version 1.1
 * @since 1.0.0
 */
@Slf4j
@Component
public class MusicProviderAdapter implements MusicProvider {

    private final SpotifyClient spotifyClient;
    private final LastFmClient lastFmClient;
    private final TmdbClient tmdbClient;
    private final MusicBrainzClient musicBrainzClient;
    private final DeezerClient deezerClient;
    private final ItunesClient itunesClient;

    public MusicProviderAdapter(SpotifyClient spotifyClient, LastFmClient lastFmClient,
                                TmdbClient tmdbClient, MusicBrainzClient musicBrainzClient,
                                DeezerClient deezerClient, ItunesClient itunesClient) {
        this.spotifyClient = spotifyClient;
        this.lastFmClient = lastFmClient;
        this.tmdbClient = tmdbClient;
        this.musicBrainzClient = musicBrainzClient;
        this.deezerClient = deezerClient;
        this.itunesClient = itunesClient;
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
    @Cacheable(cacheNames = CacheConfig.CACHE_SEARCH_SONGS, key = "#query + '-' + #limit")
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
    @Cacheable(cacheNames = CacheConfig.CACHE_AUDIO_FEATURES, key = "#spotifyId")
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
                    log.debug("Spotify no disponible para '{}', intentando Deezer", trackName);
                    deezerClient.searchTrack(trackName, artistName).ifPresent(result::add);
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
    @Cacheable(cacheNames = CacheConfig.CACHE_SONG_TAGS, key = "#artist + '-' + #title")
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

    /**
     * Obtiene las canciones del soundtrack de una película combinando MusicBrainz y Spotify (Flujo A).
     * <p>
     * MusicBrainz proporciona el tracklist del OST; Spotify enriquece cada pista con
     * spotifyId, previewUrl y audio features. Si Spotify no encuentra una pista, se
     * añade una canción básica con los datos de MusicBrainz.
     * </p>
     *
     * @param movieTitle título de la película
     * @return lista de canciones del soundtrack; vacía si no se encontró OST
     */
    @Cacheable(cacheNames = CacheConfig.CACHE_SOUNDTRACK_SONGS, key = "#movieTitle")
    @Override
    public List<Song> getSoundtrackSongs(String movieTitle) {
        try {
            List<MusicBrainzReleaseGroupDTO> groups = musicBrainzClient.searchSoundtrackByMovie(movieTitle);

            if (groups.isEmpty()) {
                log.info("No se encontró OST en MusicBrainz para '{}', intentando fallback por tag", movieTitle);
                return List.of();
            }

            List<MusicBrainzReleaseDTO> releases = musicBrainzClient.getReleasesByGroup(groups.get(0).getId());
            if (releases.isEmpty()) return List.of();

            // Preferir edición "Official" si existe
            MusicBrainzReleaseDTO chosenRelease = releases.stream()
                    .filter(r -> "Official".equalsIgnoreCase(r.getStatus()))
                    .findFirst()
                    .orElse(releases.get(0));

            MusicBrainzReleaseDTO release = musicBrainzClient.getRelease(chosenRelease.getId());
            if (release == null || release.getMedia() == null) return List.of();

            List<Song> songs = new ArrayList<>();
            for (MusicBrainzMediumDTO medium : release.getMedia()) {
                if (medium.getTracks() == null) continue;
                for (MusicBrainzTrackDTO track : medium.getTracks()) {
                    String trackTitle = resolveTrackTitle(track);
                    if (trackTitle == null) continue;

                    String artistName = extractArtistFromTrack(track);

                    try {
                        String query = trackTitle + (artistName != null ? " " + artistName : "");
                        SpotifySearchResponseDTO spotifyResult = spotifyClient.searchTracks(query, 1);
                        if (spotifyResult != null && spotifyResult.getTracks() != null
                                && spotifyResult.getTracks().getItems() != null
                                && !spotifyResult.getTracks().getItems().isEmpty()) {
                            songs.add(mapTrackToSong(spotifyResult.getTracks().getItems().get(0)));
                        }
                    } catch (ExternalApiException e) {
                        log.info("Spotify rate-limited para '{}' ({}), intentando Deezer", trackTitle, e.getMessage());
                        deezerClient.searchTrack(trackTitle, artistName).ifPresent(t -> {
                            log.info("  Deezer encontró '{}'", trackTitle);
                            songs.add(t);
                        });
                    }
                }
            }

            // Adjuntar la película a todas las canciones para que la búsqueda por movie funcione
            if (!songs.isEmpty()) {
                Movie movie = fetchMovieFromTmdb(movieTitle);
                if (movie != null) {
                    songs.forEach(s -> s.setMovies(List.of(movie)));
                }
            }

            return songs;

        } catch (ExternalApiException e) {
            log.warn("Error al obtener soundtrack de '{}', devolviendo vacío: {}", movieTitle, e.getMessage());
            return List.of();
        }
    }

    /**
     * Obtiene las películas a las que pertenece una canción combinando MusicBrainz y TMDb (Flujo B).
     * <p>
     * MusicBrainz localiza los soundtracks que contienen la grabación;
     * TMDb enriquece cada película con metadatos completos (director, géneros, año).
     * </p>
     *
     * @param artist nombre del artista
     * @param title  título de la canción
     * @return lista de películas en las que aparece la canción; vacía si no se encontró ninguna
     */
    @Cacheable(cacheNames = CacheConfig.CACHE_MOVIES_FOR_SONG, key = "#artist + '-' + #title")
    @Override
    public List<Movie> getMoviesForSong(String artist, String title) {
        try {
            Optional<String> mbid = musicBrainzClient.searchRecordingByArtistAndTitle(artist, title);
            if (mbid.isEmpty()) return List.of();

            List<MusicBrainzReleaseGroupDTO> soundtracks = musicBrainzClient.getMoviesForRecording(mbid.get());
            if (soundtracks.isEmpty()) return List.of();

            List<Movie> movies = new ArrayList<>();
            for (MusicBrainzReleaseGroupDTO rg : soundtracks) {
                try {
                    TMDBMultiSearchResponseDTO search = tmdbClient.searchMulti(rg.getTitle(), 1);
                    if (search == null || search.getResults() == null || search.getResults().isEmpty()) continue;

                    TMDBMultiResultDTO result = search.getResults().stream()
                            .filter(r -> "movie".equals(r.getMediaType()) || "tv".equals(r.getMediaType()))
                            .findFirst()
                            .orElse(null);
                    if (result == null) continue;

                    // Géneros y director: solo disponibles para películas en TMDb
                    List<String> genres = List.of();
                    String director = null;
                    if ("movie".equals(result.getMediaType())) {
                        try {
                            TMDBMovieDetailDTO details = tmdbClient.getMovieDetails(result.getId());
                            if (details != null && details.getGenres() != null) {
                                genres = details.getGenres().stream()
                                        .map(TMDBMovieDetailDTO.GenreDTO::getName)
                                        .toList();
                            }
                        } catch (ExternalApiException ignored) {}

                        try {
                            TMDBCreditDTO credits = tmdbClient.getMovieCredits(result.getId());
                            if (credits != null && credits.getCrew() != null) {
                                director = credits.getCrew().stream()
                                        .filter(c -> "Director".equals(c.getJob()))
                                        .findFirst()
                                        .map(TMDBCreditDTO.CrewMember::getName)
                                        .orElse(null);
                            }
                        } catch (ExternalApiException ignored) {}
                    }

                    Integer year = null;
                    String displayYear = result.getDisplayYear();
                    if (displayYear != null) {
                        try { year = Integer.parseInt(displayYear); }
                        catch (NumberFormatException ignored) {}
                    }

                    Movie movie = new Movie();
                    movie.setTmdbId(result.getId().intValue());
                    movie.setTitle(result.getDisplayTitle());
                    movie.setYear(year);
                    movie.setDirector(director);
                    movie.setGenres(genres);
                    movie.setMediaType(result.getMediaType());
                    movies.add(movie);

                } catch (ExternalApiException e) {
                    log.debug("No se encontró en TMDb el soundtrack '{}': {}", rg.getTitle(), e.getMessage());
                }
            }
            return movies;

        } catch (ExternalApiException e) {
            log.warn("Error al obtener películas para '{} - {}', devolviendo vacío: {}", artist, title, e.getMessage());
            return List.of();
        }
    }

    /**
     * Busca canciones por tag estético en Last.fm y las enriquece con Spotify.
     * <p>
     * Flujo: {@code tag.getTopTracks} en Last.fm → por cada track, buscar en Spotify
     * con "{@code title artist}" para obtener spotifyId, previewUrl y datos de álbum.
     * Los tracks que Spotify no encuentra se omiten silenciosamente.
     * </p>
     *
     * @param tag   nombre del tag / estética
     * @param limit número máximo de canciones a devolver
     * @return lista de canciones enriquecidas; vacía si Last.fm no tiene el tag o hay error
     */
    @Override
    public List<Song> searchSongsByTag(String tag, int limit) {
        try {
            LastFmTagTrackDTO tagTrackDTO = lastFmClient.getTopTracksByTag(tag, limit);

            if (tagTrackDTO == null || tagTrackDTO.getTracks() == null
                    || tagTrackDTO.getTracks().getTrack() == null) {
                return List.of();
            }

            List<Song> result = new ArrayList<>();

            for (LastFmTagTrackDTO.TagTrack tagTrack : tagTrackDTO.getTracks().getTrack()) {
                if (result.size() >= limit) break;

                String trackName = tagTrack.getName();
                String artistName = tagTrack.getArtist() != null ? tagTrack.getArtist().getName() : null;

                if (trackName == null || trackName.isBlank()) continue;

                try {
                    String query = trackName + (artistName != null ? " " + artistName : "");
                    SpotifySearchResponseDTO spotifyResult = spotifyClient.searchTracks(query, 1);

                    if (spotifyResult != null && spotifyResult.getTracks() != null
                            && spotifyResult.getTracks().getItems() != null
                            && !spotifyResult.getTracks().getItems().isEmpty()) {
                        result.add(mapTrackToSong(spotifyResult.getTracks().getItems().get(0)));
                    }
                } catch (ExternalApiException e) {
                    log.debug("Spotify no disponible para '{}' (tag '{}'), intentando Deezer", trackName, tag);
                    deezerClient.searchTrack(trackName, artistName).ifPresent(result::add);
                }
            }

            return result;

        } catch (ExternalApiException e) {
            log.warn("Error al buscar por tag '{}' en Last.fm, devolviendo vacío: {}", tag, e.getMessage());
            return List.of();
        }
    }

    @Cacheable(cacheNames = CacheConfig.CACHE_SEARCH_SONGS, key = "'preview-' + #title + '-' + #artist")
    @Override
    public Optional<String> findPreviewUrl(String title, String artist) {
        return itunesClient.findPreviewUrl(title, artist);
    }

    /**
     * Devuelve los nombres de artistas cuyo país de origen coincide con el indicado.
     * <p>
     * Delega en {@link MusicBrainzClient#searchArtistsByCountry(String, int)} usando la búsqueda
     * Lucene {@code area:"country" AND type:person}. Los nombres se usan después en
     * {@code SearchSongsUseCase} para buscar sus top tracks en Spotify y poblar la BD con
     * canciones que tengan {@code artist.country} ya informado.
     * </p>
     *
     * @param country nombre del país en inglés (ej. "Spain", "Japan")
     * @param limit   número máximo de artistas a devolver
     * @return lista de nombres de artistas; vacía si MusicBrainz no devuelve resultados o hay error
     */
    @Override
    public List<String> getArtistNamesByCountry(String country, int limit) {
        try {
            return musicBrainzClient.searchArtistsByCountry(country, limit).stream()
                    .map(a -> a.getName())
                    .filter(name -> name != null && !name.isBlank())
                    .toList();
        } catch (Exception e) {
            log.warn("Error buscando artistas de '{}' en MusicBrainz: {}", country, e.getMessage());
            return List.of();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS INTERNOS
    // ─────────────────────────────────────────────────────────────────────────

    /** Busca una película o serie en TMDb por título y devuelve un Movie con sus datos básicos. */
    private Movie fetchMovieFromTmdb(String movieTitle) {
        try {
            TMDBMultiSearchResponseDTO search = tmdbClient.searchMulti(movieTitle, 1);
            if (search == null || search.getResults() == null || search.getResults().isEmpty()) {
                return null;
            }
            TMDBMultiResultDTO result = search.getResults().stream()
                    .filter(r -> "movie".equals(r.getMediaType()) || "tv".equals(r.getMediaType()))
                    .findFirst()
                    .orElse(null);
            if (result == null) return null;

            Movie movie = new Movie();
            movie.setTmdbId(result.getId().intValue());
            movie.setTitle(result.getDisplayTitle());
            movie.setMediaType(result.getMediaType());
            String displayYear = result.getDisplayYear();
            if (displayYear != null) {
                try { movie.setYear(Integer.parseInt(displayYear)); }
                catch (NumberFormatException ignored) {}
            }
            return movie;
        } catch (ExternalApiException e) {
            log.debug("No se pudo obtener info de '{}' en TMDb: {}", movieTitle, e.getMessage());
            return null;
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
        song.setIsrc(dto.getExternalIds() != null ? dto.getExternalIds().getIsrc() : null);
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

    /** Resuelve el título de una pista: usa el título de la pista o, si es null, el de la grabación. */
    private String resolveTrackTitle(MusicBrainzTrackDTO track) {
        if (track.getTitle() != null && !track.getTitle().isBlank()) return track.getTitle();
        if (track.getRecording() != null && track.getRecording().getTitle() != null) {
            return track.getRecording().getTitle();
        }
        return null;
    }

    /** Extrae el nombre del artista principal de una pista de MusicBrainz. */
    private String extractArtistFromTrack(MusicBrainzTrackDTO track) {
        if (track.getRecording() != null && track.getRecording().getArtistCredit() != null
                && !track.getRecording().getArtistCredit().isEmpty()) {
            MusicBrainzArtistCreditDTO credit = track.getRecording().getArtistCredit().get(0);
            if (credit.getArtist() != null) return credit.getArtist().getName();
            if (credit.getName() != null) return credit.getName();
        }
        return null;
    }
}