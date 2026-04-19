package es.wara.ecos.backend.interfaces.rest.controller;

import es.wara.ecos.backend.application.usecase.playlist.CreatePlaylistUseCase;
import es.wara.ecos.backend.domain.model.Playlist;
import es.wara.ecos.backend.domain.model.User;
import es.wara.ecos.backend.domain.port.PlaylistRepository;
import es.wara.ecos.backend.domain.port.SongRepository;
import es.wara.ecos.backend.domain.port.UserRepository;
import es.wara.ecos.backend.infrastructure.adapter.ai.RecommendationClient;
import es.wara.ecos.backend.infrastructure.adapter.ai.RecommendationClient.CrossoverResult;
import es.wara.ecos.backend.infrastructure.adapter.ai.RecommendationClient.ScoredSongId;
import es.wara.ecos.backend.infrastructure.security.SecurityUtils;
import es.wara.ecos.backend.interfaces.rest.dto.CrossoverResponse;
import es.wara.ecos.backend.interfaces.rest.dto.CrossoverSaveResponse;
import es.wara.ecos.backend.interfaces.rest.dto.SongResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
@Slf4j
@RestController
@RequestMapping("/v1/recommendations")
@Tag(name = "Recommendations", description = "Recomendaciones personalizadas y cruzadas")
public class RecommendationController {

    private final RecommendationClient recommendationClient;
    private final SongRepository songRepository;
    private final UserRepository userRepository;
    private final PlaylistRepository playlistRepository;
    private final CreatePlaylistUseCase createPlaylistUseCase;

    public RecommendationController(RecommendationClient recommendationClient,
                                    SongRepository songRepository,
                                    UserRepository userRepository,
                                    PlaylistRepository playlistRepository,
                                    CreatePlaylistUseCase createPlaylistUseCase) {
        this.recommendationClient = recommendationClient;
        this.songRepository = songRepository;
        this.userRepository = userRepository;
        this.playlistRepository = playlistRepository;
        this.createPlaylistUseCase = createPlaylistUseCase;
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
     * Genera el crossover entre el usuario autenticado y el usuario identificado por su código,
     * y guarda las canciones resultantes como una nueva playlist.
     *
     * @param userCode código del segundo usuario en formato ECO-XXXX-XXXX
     * @return {@code 201 Created} con el id, nombre y número de canciones de la playlist creada
     */
    @PostMapping("/crossover/save")
    @Operation(summary = "Guardar crossover como playlist",
               description = "Resuelve el código de usuario, obtiene el crossover y crea una playlist con las canciones")
    public ResponseEntity<CrossoverSaveResponse> saveCrossover(@RequestParam String userCode) {
        UUID userAId = SecurityUtils.getCurrentUserId();

        User userB = userRepository.findByUserCode(userCode.toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("No existe ningún usuario con el código: " + userCode));

        if (userAId.equals(userB.getId())) {
            throw new IllegalArgumentException("No puedes hacer crossover contigo mismo");
        }

        CrossoverResult result = recommendationClient
                .getCrossoverRecommendations(userAId, userB.getId())
                .orElse(new CrossoverResult(List.of(), 0));

        if (result.recommendations().isEmpty()) {
            throw new IllegalArgumentException("El usuario no tiene perfil musical suficiente para el crossover");
        }

        String playlistName = "Crossover con " + userB.getUsername();
        Playlist playlist = createPlaylistUseCase.execute(userAId, playlistName, null);

        int added = addSongsWithCompensation(playlist.getId(), result.recommendations());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CrossoverSaveResponse(playlist.getId(), playlistName, added));
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

    /**
     * Añade canciones a la playlist. Si falla por error inesperado, elimina la playlist
     * para evitar dejar estado inconsistente (acción compensatoria).
     * Si ninguna canción existe en BD, también elimina la playlist.
     */
    private int addSongsWithCompensation(UUID playlistId, List<ScoredSongId> songs) {
        int added = 0;
        try {
            for (ScoredSongId scored : songs) {
                UUID songId;
                try {
                    songId = UUID.fromString(scored.songId());
                } catch (IllegalArgumentException ignored) {
                    continue;
                }
                if (songRepository.findById(songId).isPresent()) {
                    playlistRepository.addSong(playlistId, songId);
                    added++;
                }
            }
        } catch (Exception e) {
            log.error("Error al añadir canciones al crossover, eliminando playlist {}", playlistId, e);
            playlistRepository.delete(playlistId);
            throw e;
        }
        if (added == 0) {
            playlistRepository.delete(playlistId);
            throw new IllegalArgumentException("El crossover no encontró canciones compatibles en la biblioteca");
        }
        return added;
    }
}