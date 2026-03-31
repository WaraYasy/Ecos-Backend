package es.wara.ecos.backend.interfaces.rest.dto;

import es.wara.ecos.backend.domain.model.Song;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

/**
 * DTO de respuesta para una canción del catálogo de ECOS.
 *
 * @author Wara Pacheco
 * @version 1.1
 * @since 1.0.0
 */
@Schema(description = "Canción del catálogo de ECOS con metadatos enriquecidos de múltiples fuentes")
public record SongResponse(

        @Schema(description = "Identificador único interno (UUID v7 time-ordered)")
        UUID id,

        @Schema(description = "Título de la canción")
        String title,

        @Schema(description = "Nombres de los artistas, en orden (principal primero)")
        List<String> artists,

        @Schema(description = "Título del álbum")
        String album,

        @Schema(description = "Año de publicación del álbum")
        Integer year,

        @Schema(description = "URL de la carátula del álbum (fuente: Spotify)")
        String imageUrl,

        @Schema(description = "Identificador de la pista en Spotify")
        String spotifyId,

        @Schema(description = "Código ISRC de la grabación (International Standard Recording Code)")
        String isrc,

        @Schema(description = "Duración en milisegundos")
        Integer durationMs,

        @Schema(description = "URL de preview de 30 segundos. " +
                "Fuente primaria: Spotify. " +
                "Fuente de respaldo: iTunes Search API (Apple) — usado desde noviembre 2024 " +
                "porque Spotify dejó de devolver preview_url en Development Mode. " +
                "Puede ser null si ninguna fuente tiene preview disponible.")
        String previewUrl,

        @Schema(description = "Etiquetas de género y estética (fuente: Last.fm)")
        List<String> tags,

        @Schema(description = "Características de audio extraídas de Spotify (tempo, energía, valencia, etc.)")
        AudioFeaturesResponse audioFeatures,

        @Schema(description = "Películas con las que esta canción está relacionada (fuente: MusicBrainz + TMDb)")
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
                song.getAlbum() != null ? song.getAlbum().getCoverUrl() : null,
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
