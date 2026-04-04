package es.wara.ecos.backend.interfaces.rest.dto;

import es.wara.ecos.backend.domain.model.GraphResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

/**
 * DTO de respuesta del grafo neuronal (formato plano: nodos + aristas separados).
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Schema(description = "Grafo musical con nodos (canciones posicionadas) y aristas (relaciones)")
public record GraphResponseDto(

        @Schema(description = "ID de la canción que originó el grafo")
        UUID rootSongId,

        @Schema(description = "Lista de nodos del grafo (incluye el nodo raíz)")
        List<GraphNodeResponse> nodes,

        @Schema(description = "Lista de aristas del grafo")
        List<GraphEdgeResponse> edges
) {

    public static GraphResponseDto from(GraphResponse domain) {
        List<GraphNodeResponse> nodes = domain.nodes().stream()
                .map(GraphNodeResponse::from)
                .toList();
        List<GraphEdgeResponse> edges = domain.edges().stream()
                .map(GraphEdgeResponse::from)
                .toList();
        return new GraphResponseDto(domain.rootSongId(), nodes, edges);
    }

}