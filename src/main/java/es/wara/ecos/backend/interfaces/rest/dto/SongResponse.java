package es.wara.ecos.backend.interfaces.rest.dto;

import es.wara.ecos.backend.domain.model.Song;

import java.util.List;
import java.util.UUID;

/**
 * DTO de respuesta para una canción del catálogo de ECOS.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
public record SongResponse(
        UUID id,
        String title,
        List<String> artists,
        String album,
        Integer year,
        String spotifyId,
        String isrc,
        Integer durationMs,
        String previewUrl,
        List<String> tags,
        AudioFeaturesResponse audioFeatures,
        List<MovieResponse> movies
) {

    public static SongResponse from(Song song) {
        return new SongResponse(
                song.getId(),
                song.getTitle(),
                song.getArtists() != null
                        ? song.getArtists().stream().map(a -> a.getName()).toList()
                        : List.of(),
                song.getAlbum() != null ? song.getAlbum().getTitle() : null,
                song.getAlbum() != null ? song.getAlbum().getYear() : null,
                song.getSpotifyId(),
                song.getIsrc(),
                song.getDurationMs(),
                song.getPreviewUrl(),
                song.getTags() != null
                        ? song.getTags().stream().map(t -> t.getName()).toList()
                        : List.of(),
                AudioFeaturesResponse.from(song.getAudioFeatures()),
                song.getMovies() != null
                        ? song.getMovies().stream().map(MovieResponse::from).toList()
                        : List.of()
        );
    }

}
