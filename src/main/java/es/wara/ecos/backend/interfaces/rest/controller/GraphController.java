package es.wara.ecos.backend.interfaces.rest.controller;

import es.wara.ecos.backend.application.usecase.graph.BuildGraphUseCase;
import es.wara.ecos.backend.application.usecase.graph.ExpandGraphUseCase;
import es.wara.ecos.backend.interfaces.rest.dto.ExpandGraphRequest;
import es.wara.ecos.backend.interfaces.rest.dto.GraphResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controlador REST para el grafo neuronal de ECOS.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code GET /v1/graph} — construye el grafo inicial desde una canción raíz.</li>
 *   <li>{@code POST /v1/graph/expand} — expande el grafo desde un nodo existente.</li>
 * </ul>
 * Todos los endpoints requieren autenticación JWT.
 * El grafo inicial se cachea por songId + maxNodes (TTL configurado en CacheConfig).</p>
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@RestController
@RequestMapping("/v1/graph")
@Tag(name = "Graph", description = "Grafo neuronal de exploración musical")
public class GraphController {

    private static final int DEFAULT_MAX_NODES = 8;
    private static final int DEFAULT_MAX_NEW_NODES = 5;

    private final BuildGraphUseCase buildGraphUseCase;
    private final ExpandGraphUseCase expandGraphUseCase;

    public GraphController(BuildGraphUseCase buildGraphUseCase,
                           ExpandGraphUseCase expandGraphUseCase) {
        this.buildGraphUseCase = buildGraphUseCase;
        this.expandGraphUseCase = expandGraphUseCase;
    }

    /**
     * Genera el grafo inicial desde una canción raíz.
     *
     * @param songId   ID de la canción raíz
     * @param maxNodes número máximo de nodos vecinos (por defecto 8, máximo 8)
     * @return grafo con nodos posicionados y aristas tipificadas
     */
    @GetMapping
    @Cacheable(value = "graph", key = "#songId + '-' + #maxNodes")
    @Operation(summary = "Construir grafo inicial",
               description = "Genera el grafo de canciones similares partiendo de una canción raíz")
    public ResponseEntity<GraphResponseDto> buildGraph(
            @RequestParam UUID songId,
            @RequestParam(defaultValue = "8") int maxNodes
    ) {
        int limit = Math.min(maxNodes, BuildGraphUseCase.MAX_NODES);
        return ResponseEntity.ok(GraphResponseDto.from(buildGraphUseCase.execute(songId, limit)));
    }

    /**
     * Expande el grafo añadiendo nodos vecinos al nodo seleccionado por el usuario.
     * No se cachea porque depende del estado actual del grafo en el cliente.
     *
     * @param request cuerpo con targetSongId, existingSongIds y maxNewNodes
     * @return nodos y aristas nuevos a fusionar en el cliente
     */
    @PostMapping("/expand")
    @Operation(summary = "Expandir grafo",
               description = "Añade nodos relacionados desde un nodo existente del grafo")
    public ResponseEntity<GraphResponseDto> expandGraph(@Valid @RequestBody ExpandGraphRequest request) {
        int maxNew = request.maxNewNodes() != null ? request.maxNewNodes() : DEFAULT_MAX_NEW_NODES;
        List<UUID> existing = request.existingSongIds() != null ? request.existingSongIds() : List.of();
        return ResponseEntity.ok(GraphResponseDto.from(
                expandGraphUseCase.execute(request.targetSongId(), existing, maxNew)));
    }

}