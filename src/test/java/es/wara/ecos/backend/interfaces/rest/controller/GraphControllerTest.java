package es.wara.ecos.backend.interfaces.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import es.wara.ecos.backend.application.usecase.graph.BuildGraphUseCase;
import es.wara.ecos.backend.application.usecase.graph.ExpandGraphUseCase;
import es.wara.ecos.backend.domain.model.*;
import es.wara.ecos.backend.infrastructure.security.JwtAuthFilter;
import es.wara.ecos.backend.infrastructure.security.RateLimitFilter;
import es.wara.ecos.backend.interfaces.rest.dto.ExpandGraphRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GraphController.class)
@AutoConfigureMockMvc(addFilters = false)
class GraphControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean JwtAuthFilter jwtAuthFilter;
    @MockitoBean RateLimitFilter rateLimitFilter;
    @MockitoBean BuildGraphUseCase buildGraphUseCase;
    @MockitoBean ExpandGraphUseCase expandGraphUseCase;

    private static final UUID ROOT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID CAND_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    // ─── GET /v1/graph ────────────────────────────────────────────────────────

    @Test
    void buildGraph_validSongId_returns200WithGraphStructure() throws Exception {
        when(buildGraphUseCase.execute(eq(ROOT_ID), anyInt())).thenReturn(singleNodeGraph());

        mockMvc.perform(get("/v1/graph").param("songId", ROOT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rootSongId").value(ROOT_ID.toString()))
                .andExpect(jsonPath("$.nodes").isArray())
                .andExpect(jsonPath("$.nodes[0].id").value(ROOT_ID.toString()))
                .andExpect(jsonPath("$.nodes[0].similarity").value(1.0))
                .andExpect(jsonPath("$.edges").isArray());
    }

    @Test
    void buildGraph_withNeighbor_returnsNodesAndEdges() throws Exception {
        when(buildGraphUseCase.execute(eq(ROOT_ID), anyInt())).thenReturn(twoNodeGraph());

        mockMvc.perform(get("/v1/graph").param("songId", ROOT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes").isArray())
                .andExpect(jsonPath("$.nodes.length()").value(2))
                .andExpect(jsonPath("$.edges.length()").value(1))
                .andExpect(jsonPath("$.edges[0].source").value(ROOT_ID.toString()))
                .andExpect(jsonPath("$.edges[0].target").value(CAND_ID.toString()))
                .andExpect(jsonPath("$.edges[0].type").isString());
    }

    @Test
    void buildGraph_songNotFound_returns404() throws Exception {
        when(buildGraphUseCase.execute(eq(ROOT_ID), anyInt()))
                .thenThrow(new IllegalArgumentException("Canción no encontrada: " + ROOT_ID));

        mockMvc.perform(get("/v1/graph").param("songId", ROOT_ID.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void buildGraph_missingSongId_returns400() throws Exception {
        mockMvc.perform(get("/v1/graph"))
                .andExpect(status().isBadRequest());
    }

    // ─── POST /v1/graph/expand ────────────────────────────────────────────────

    @Test
    void expandGraph_validRequest_returns200WithNewNodes() throws Exception {
        when(expandGraphUseCase.execute(eq(CAND_ID), anyList(), anyInt()))
                .thenReturn(singleNodeGraph(CAND_ID));

        ExpandGraphRequest request = new ExpandGraphRequest(CAND_ID, List.of(ROOT_ID), 5);

        mockMvc.perform(post("/v1/graph/expand")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes").isArray());
    }

    @Test
    void expandGraph_nullTargetSongId_returns400() throws Exception {
        ExpandGraphRequest request = new ExpandGraphRequest(null, List.of(), 5);

        mockMvc.perform(post("/v1/graph/expand")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void expandGraph_songNotFound_returns404() throws Exception {
        when(expandGraphUseCase.execute(eq(CAND_ID), anyList(), anyInt()))
                .thenThrow(new IllegalArgumentException("Canción no encontrada: " + CAND_ID));

        ExpandGraphRequest request = new ExpandGraphRequest(CAND_ID, List.of(ROOT_ID), 5);

        mockMvc.perform(post("/v1/graph/expand")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private GraphResponse singleNodeGraph() {
        return singleNodeGraph(ROOT_ID);
    }

    private GraphResponse singleNodeGraph(UUID id) {
        Song song = new Song();
        song.setId(id);
        song.setTitle("Test Song");
        song.setArtists(List.of());
        GraphNode rootNode = new GraphNode(song, 0f, 0f, 1f, List.of());
        return new GraphResponse(id, List.of(rootNode), List.of());
    }

    private GraphResponse twoNodeGraph() {
        Song root = new Song(); root.setId(ROOT_ID); root.setTitle("Root"); root.setArtists(List.of());
        Song cand = new Song(); cand.setId(CAND_ID); cand.setTitle("Cand"); cand.setArtists(List.of());

        GraphEdge edge = new GraphEdge(ROOT_ID, CAND_ID, 0.75f, GraphEdge.EdgeType.AUDIO);
        GraphNode rootNode = new GraphNode(root, 0f, 0f, 1f, List.of(edge));
        GraphNode candNode = new GraphNode(cand, 100f, 100f, 0.75f, List.of(edge));

        return new GraphResponse(ROOT_ID, List.of(rootNode, candNode), List.of(edge));
    }

}