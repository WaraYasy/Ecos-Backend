package es.wara.ecos.backend.interfaces.rest.dto;

import es.wara.ecos.backend.domain.model.GraphNode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * DTO de un nodo del grafo neuronal para la respuesta REST.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Schema(description = "Nodo del grafo musical con su posición visual y score de similitud")
public record GraphNodeResponse(

        @Schema(description = "Identificador de la canción")
        UUID id,

        @Schema(description = "Título de la canción")
        String title,

        @Schema(description = "Nombre del artista principal")
        String artistName,

        @Schema(description = "URL de la carátula del álbum")
        String coverUrl,

        @Schema(description = "URL de preview de 30 segundos")
        String previewUrl,

        @Schema(description = "Identificador de Spotify")
        String spotifyId,

        @Schema(description = "Coordenada X en el layout del grafo")
        float x,

        @Schema(description = "Coordenada Y en el layout del grafo")
        float y,

        @Schema(description = "Similitud con el nodo raíz (0.0 a 1.0)")
        float similarity
) {

    public static GraphNodeResponse from(GraphNode node) {
        var song = node.getSong();
        String artist = (song.getArtists() != null && !song.getArtists().isEmpty())
                ? song.getArtists().get(0).getName()
                : null;
        String coverUrl = song.getAlbum() != null ? song.getAlbum().getCoverUrl() : null;

        return new GraphNodeResponse(
                song.getId(),
                song.getTitle(),
                artist,
                coverUrl,
                song.getPreviewUrl(),
                song.getSpotifyId(),
                node.getXPosition(),
                node.getYPosition(),
                node.getSimilarity()
        );
    }

}