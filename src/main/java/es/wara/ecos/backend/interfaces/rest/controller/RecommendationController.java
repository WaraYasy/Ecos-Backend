package es.wara.ecos.backend.interfaces.rest.controller;

import es.wara.ecos.backend.domain.port.SongRepository;
import es.wara.ecos.backend.infrastructure.adapter.ai.RecommendationClient;
import es.wara.ecos.backend.infrastructure.adapter.ai.RecommendationClient.CrossoverResult;
import es.wara.ecos.backend.infrastructure.adapter.ai.RecommendationClient.ScoredSongId;
import es.wara.ecos.backend.infrastructure.security.SecurityUtils;
import es.wara.ecos.backend.interfaces.rest.dto.CrossoverResponse;
import es.wara.ecos.backend.interfaces.rest.dto.SongResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints de recomendaciones musicales personalizadas.
 *
 * <pre>
 * GET /v1/recommendations                     → recomendaciones personalizadas para el usuario autenticado
 * GET /v1/recommendations/crossover?userBId=  → recomendaciones cruzadas con otro usuario
 * </pre>
 *
 * <p>Java actúa como intermediario: llama al microservicio de IA, obtiene UUIDs de canciones
 * ordenados por score, y los enriquece con los datos completos de la BD antes de responder.</p>
 *
 * @author Wara Pacheco
 * @since 1.0.0
 */
@RestController
@RequestMapping("/v1/recommendations")
@Tag(name = "Recommendations", description = "Recomendaciones personalizadas y cruzadas")
public class RecommendationController {

    private final RecommendationClient recommendationClient;
    private final SongRepository songRepository;

    public RecommendationController(RecommendationClient recommendationClient,
                                    SongRepository songRepository) {
        this.recommendationClient = recommendationClient;
        this.songRepository = songRepository;
    }

    /**
     * Devuelve recomendaciones personalizadas para el usuario autenticado.
     *
     * @return lista de canciones ordenadas por score de afinidad
     */
    @GetMapping
    @Operation(summary = "Recomendaciones personalizadas",
               description = "Canciones recomendadas según el historial de interacciones del usuario")
    public ResponseEntity<List<SongResponse>> getRecommendations() {
        UUID userId = SecurityUtils.getCurrentUserId();
        List<ScoredSongId> scored = recommendationClient.getRecommendations(userId);
        List<SongResponse> songs = enrichSongs(scored);
        return ResponseEntity.ok(songs);
    }

    /**
     * Devuelve recomendaciones cruzadas entre el usuario autenticado y otro usuario.
     *
     * @param userBId UUID del segundo usuario
     * @return canciones recomendadas para ambos y número de canciones en común
     */
    @GetMapping("/crossover")
    @Operation(summary = "Recomendaciones cruzadas",
               description = "Canciones que gustarían a dos usuarios combinando sus perfiles musicales")
    public ResponseEntity<CrossoverResponse> getCrossover(@RequestParam UUID userBId) {
        UUID userAId = SecurityUtils.getCurrentUserId();
        CrossoverResult result = recommendationClient
                .getCrossoverRecommendations(userAId, userBId)
                .orElse(new CrossoverResult(List.of(), 0));

        List<SongResponse> songs = enrichSongs(result.recommendations());
        return ResponseEntity.ok(new CrossoverResponse(result.overlapCount(), songs));
    }

    /**
     * Convierte una lista de (songId, score) en {@link SongResponse} completos,
     * descartando silenciosamente los IDs que no existan en BD.
     */
    private List<SongResponse> enrichSongs(List<ScoredSongId> scored) {
        return scored.stream()
                .map(s -> {
                    try {
                        UUID id = UUID.fromString(s.songId());
                        return songRepository.findById(id).map(SongResponse::from).orElse(null);
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(r -> r != null)
                .toList();
    }
}