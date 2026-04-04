package es.wara.ecos.backend.interfaces.rest.dto;

import es.wara.ecos.backend.domain.model.GraphEdge;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * DTO de una arista del grafo neuronal para la respuesta REST.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Schema(description = "Arista del grafo que conecta dos canciones con su score y tipo dominante")
public record GraphEdgeResponse(

        @Schema(description = "ID de la canción origen")
        UUID source,

        @Schema(description = "ID de la canción destino")
        UUID target,

        @Schema(description = "Peso de similitud entre los dos nodos (0.0 a 1.0)")
        float weight,

        @Schema(description = "Componente dominante que origina la conexión",
                allowableValues = {"AUDIO", "SEMANTIC", "TAG", "ERA", "MOVIE"})
        String type
) {

    public static GraphEdgeResponse from(GraphEdge edge) {
        return new GraphEdgeResponse(
                edge.getSourceId(),
                edge.getTargetId(),
                edge.getWeight(),
                edge.getType().name()
        );
    }

}