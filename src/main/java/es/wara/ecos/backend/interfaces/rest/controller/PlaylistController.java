package es.wara.ecos.backend.interfaces.rest.controller;

import es.wara.ecos.backend.application.service.ExportJob;
import es.wara.ecos.backend.application.service.ExportJobService;
import es.wara.ecos.backend.application.usecase.playlist.AddSongToPlaylistUseCase;
import es.wara.ecos.backend.application.usecase.playlist.CreatePlaylistUseCase;
import es.wara.ecos.backend.application.usecase.playlist.DeletePlaylistUseCase;
import es.wara.ecos.backend.application.usecase.playlist.GetPlaylistByIdUseCase;
import es.wara.ecos.backend.application.usecase.playlist.GetUserPlaylistsUseCase;
import es.wara.ecos.backend.application.usecase.playlist.RemoveSongFromPlaylistUseCase;
import es.wara.ecos.backend.domain.exception.ForbiddenException;
import es.wara.ecos.backend.domain.model.Playlist;
import es.wara.ecos.backend.domain.port.PlaylistRepository;
import es.wara.ecos.backend.infrastructure.security.SecurityUtils;
import es.wara.ecos.backend.interfaces.rest.dto.AddSongRequest;
import es.wara.ecos.backend.interfaces.rest.dto.ExportJobResponse;
import es.wara.ecos.backend.interfaces.rest.dto.PlaylistRequest;
import es.wara.ecos.backend.interfaces.rest.dto.PlaylistResponse;
import es.wara.ecos.backend.interfaces.rest.dto.UpdatePlaylistRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

/**
 * Controlador REST para gestión de playlists de ECOS.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@RestController
@RequestMapping("/v1/playlists")
public class PlaylistController {

    private final GetPlaylistByIdUseCase getPlaylistByIdUseCase;
    private final GetUserPlaylistsUseCase getUserPlaylistsUseCase;
    private final CreatePlaylistUseCase createPlaylistUseCase;
    private final DeletePlaylistUseCase deletePlaylistUseCase;
    private final AddSongToPlaylistUseCase addSongToPlaylistUseCase;
    private final RemoveSongFromPlaylistUseCase removeSongFromPlaylistUseCase;
    private final ExportJobService exportJobService;
    private final PlaylistRepository playlistRepository;

    public PlaylistController(
            GetPlaylistByIdUseCase getPlaylistByIdUseCase,
            GetUserPlaylistsUseCase getUserPlaylistsUseCase,
            CreatePlaylistUseCase createPlaylistUseCase,
            DeletePlaylistUseCase deletePlaylistUseCase,
            AddSongToPlaylistUseCase addSongToPlaylistUseCase,
            RemoveSongFromPlaylistUseCase removeSongFromPlaylistUseCase,
            ExportJobService exportJobService,
            PlaylistRepository playlistRepository
    ) {
        this.getPlaylistByIdUseCase = getPlaylistByIdUseCase;
        this.getUserPlaylistsUseCase = getUserPlaylistsUseCase;
        this.createPlaylistUseCase = createPlaylistUseCase;
        this.deletePlaylistUseCase = deletePlaylistUseCase;
        this.addSongToPlaylistUseCase = addSongToPlaylistUseCase;
        this.removeSongFromPlaylistUseCase = removeSongFromPlaylistUseCase;
        this.exportJobService = exportJobService;
        this.playlistRepository = playlistRepository;
    }

    /**
     * Devuelve todas las playlists del usuario autenticado.
     *
     * @return {@code 200 OK} con la lista de playlists (puede ser vacía)
     */
    @GetMapping
    public ResponseEntity<List<PlaylistResponse>> getByUser() {
        UUID userId = SecurityUtils.getCurrentUserId();
        List<PlaylistResponse> playlists = getUserPlaylistsUseCase.execute(userId)
                .stream().map(PlaylistResponse::from).toList();
        return ResponseEntity.ok(playlists);
    }

    /**
     * Crea una nueva playlist para el usuario autenticado.
     *
     * @param request datos de la playlist (nombre y descripción opcional)
     * @return {@code 201 Created} con la playlist recién creada
     */
    @PostMapping
    public ResponseEntity<PlaylistResponse> create(@Valid @RequestBody PlaylistRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Playlist playlist = createPlaylistUseCase.execute(userId, request.name(), request.description());
        return ResponseEntity.status(HttpStatus.CREATED).body(PlaylistResponse.from(playlist));
    }

    /**
     * Devuelve los detalles de una playlist por su ID, incluyendo sus canciones.
     * Solo el propietario puede acceder a ella.
     *
     * @param id identificador de la playlist
     * @return {@code 200 OK} con la playlist, {@code 403} si no es el propietario,
     *         o {@code 404} si no existe
     */
    @GetMapping("/{id}")
    public ResponseEntity<PlaylistResponse> getById(@PathVariable UUID id) {
        Playlist playlist = getPlaylistByIdUseCase.execute(id);
        if (!playlist.getUserId().equals(SecurityUtils.getCurrentUserId())) {
            throw new ForbiddenException("No tienes permiso para acceder a esta playlist");
        }
        return ResponseEntity.ok(PlaylistResponse.from(playlist));
    }

    /**
     * Actualiza el nombre y/o descripción de una playlist. Solo el propietario puede modificarla.
     * Semántica PATCH: solo se actualizan los campos con valor no nulo.
     *
     * @param id      identificador de la playlist
     * @param request campos a actualizar (ambos opcionales)
     * @return {@code 200 OK} con la playlist actualizada
     */
    @PatchMapping("/{id}")
    public ResponseEntity<PlaylistResponse> update(@PathVariable UUID id,
                                                   @RequestBody UpdatePlaylistRequest request) {
        Playlist playlist = getPlaylistByIdUseCase.execute(id);
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (!playlist.getUserId().equals(currentUserId)) {
            throw new ForbiddenException("No tienes permiso para modificar esta playlist");
        }
        if (request.name() != null && !request.name().isBlank()) {
            playlist.setName(request.name());
        }
        if (request.description() != null) {
            playlist.setDescription(request.description());
        }
        return ResponseEntity.ok(PlaylistResponse.from(playlistRepository.save(playlist)));
    }

    /**
     * Elimina una playlist. Solo el propietario puede eliminarla.
     * <p>
     * Devuelve {@code 403} si el usuario autenticado no es el dueño de la playlist.
     * </p>
     *
     * @param id identificador de la playlist a eliminar
     * @return {@code 204 No Content} si se eliminó correctamente
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        deletePlaylistUseCase.execute(id, SecurityUtils.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Añade una canción a una playlist. Solo el propietario puede modificarla.
     *
     * @param id      identificador de la playlist
     * @param request cuerpo con el {@code songId} de la canción a añadir
     * @return {@code 204 No Content} si se añadió correctamente
     */
    @PostMapping("/{id}/songs")
    public ResponseEntity<Void> addSong(@PathVariable UUID id,
                                        @Valid @RequestBody AddSongRequest request) {
        addSongToPlaylistUseCase.execute(id, request.songId(), SecurityUtils.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Elimina una canción de una playlist. Solo el propietario puede modificarla.
     *
     * @param id     identificador de la playlist
     * @param songId identificador de la canción a eliminar
     * @return {@code 204 No Content} si se eliminó correctamente
     */
    @DeleteMapping("/{id}/songs/{songId}")
    public ResponseEntity<Void> removeSong(@PathVariable UUID id, @PathVariable UUID songId) {
        removeSongFromPlaylistUseCase.execute(id, songId, SecurityUtils.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Inicia la exportación asíncrona de una playlist a Spotify.
     * <p>
     * Devuelve inmediatamente un {@code jobId} para consultar el estado en
     * {@code GET /{id}/export/spotify/{jobId}}.
     * Requiere que el usuario haya vinculado previamente su cuenta de Spotify.
     * </p>
     *
     * @param id identificador de la playlist a exportar
     * @return {@code 202 Accepted} con el jobId del proceso en background
     */
    @PostMapping("/{id}/export/spotify")
    public ResponseEntity<ExportJobResponse> exportToSpotify(@PathVariable UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        ExportJob job = exportJobService.submit(id, userId);
        return ResponseEntity.accepted().body(ExportJobResponse.from(job));
    }

    /**
     * Consulta el estado de un job de exportación a Spotify.
     *
     * @param id    identificador de la playlist
     * @param jobId identificador del job devuelto por {@code POST /{id}/export/spotify}
     * @return {@code 200 OK} con el estado actual, {@code 404} si el job no existe o expiró
     */
    @GetMapping("/{id}/export/spotify/{jobId}")
    public ResponseEntity<ExportJobResponse> exportStatus(@PathVariable UUID id,
                                                          @PathVariable String jobId) {
        Optional<ExportJob> job = exportJobService.findById(jobId);
        return job.map(j -> ResponseEntity.ok(ExportJobResponse.from(j)))
                  .orElseGet(() -> ResponseEntity.notFound().build());
    }

}
