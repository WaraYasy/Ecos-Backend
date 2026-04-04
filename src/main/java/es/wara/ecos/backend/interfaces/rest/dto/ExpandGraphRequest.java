package es.wara.ecos.backend.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Cuerpo de la petición para expandir el grafo desde un nodo existente.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Schema(description = "Petición para expandir el grafo desde un nodo seleccionado")
public record ExpandGraphRequest(

        @NotNull
        @Schema(description = "Canción desde la que se expande el grafo")
        UUID targetSongId,

        @NotNull
        @Schema(description = "IDs de canciones ya presentes en el grafo del cliente (se excluyen del resultado)")
        List<UUID> existingSongIds,

        @Schema(description = "Número máximo de nodos nuevos a añadir (máx. 5)", example = "5")
        Integer maxNewNodes
) {}