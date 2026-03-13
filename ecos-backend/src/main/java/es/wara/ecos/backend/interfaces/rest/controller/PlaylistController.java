package es.wara.ecos.backend.interfaces.rest.controller;

import es.wara.ecos.backend.application.usecase.playlist.AddSongToPlaylistUseCase;
import es.wara.ecos.backend.application.usecase.playlist.CreatePlaylistUseCase;
import es.wara.ecos.backend.application.usecase.playlist.DeletePlaylistUseCase;
import es.wara.ecos.backend.application.usecase.playlist.ExportPlaylistToSpotifyUseCase;
import es.wara.ecos.backend.application.usecase.playlist.GetPlaylistByIdUseCase;
import es.wara.ecos.backend.application.usecase.playlist.GetUserPlaylistsUseCase;
import es.wara.ecos.backend.application.usecase.playlist.RemoveSongFromPlaylistUseCase;
import es.wara.ecos.backend.domain.model.Playlist;
import es.wara.ecos.backend.infrastructure.security.SecurityUtils;
import es.wara.ecos.backend.interfaces.rest.dto.AddSongRequest;
import es.wara.ecos.backend.interfaces.rest.dto.PlaylistRequest;
import es.wara.ecos.backend.interfaces.rest.dto.PlaylistResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

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
    private final ExportPlaylistToSpotifyUseCase exportPlaylistToSpotifyUseCase;

    /**
     * Inyecta los casos de uso necesarios para las operaciones sobre playlists.
     *
     * @param getPlaylistByIdUseCase        consulta de una playlist por ID
     * @param getUserPlaylistsUseCase       listado de playlists del usuario autenticado
     * @param createPlaylistUseCase         creación de una nueva playlist
     * @param deletePlaylistUseCase         eliminación de una playlist (solo el propietario)
     * @param addSongToPlaylistUseCase      adición de una canción a una playlist
     * @param removeSongFromPlaylistUseCase eliminación de una canción de una playlist
     * @param exportPlaylistToSpotifyUseCase exportación de una playlist a Spotify
     */
    public PlaylistController(
            GetPlaylistByIdUseCase getPlaylistByIdUseCase,
            GetUserPlaylistsUseCase getUserPlaylistsUseCase,
            CreatePlaylistUseCase createPlaylistUseCase,
            DeletePlaylistUseCase deletePlaylistUseCase,
            AddSongToPlaylistUseCase addSongToPlaylistUseCase,
            RemoveSongFromPlaylistUseCase removeSongFromPlaylistUseCase,
            ExportPlaylistToSpotifyUseCase exportPlaylistToSpotifyUseCase
    ) {
        this.getPlaylistByIdUseCase = getPlaylistByIdUseCase;
        this.getUserPlaylistsUseCase = getUserPlaylistsUseCase;
        this.createPlaylistUseCase = createPlaylistUseCase;
        this.deletePlaylistUseCase = deletePlaylistUseCase;
        this.addSongToPlaylistUseCase = addSongToPlaylistUseCase;
        this.removeSongFromPlaylistUseCase = removeSongFromPlaylistUseCase;
        this.exportPlaylistToSpotifyUseCase = exportPlaylistToSpotifyUseCase;
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
     *
     * @param id identificador de la playlist
     * @return {@code 200 OK} con la playlist, o {@code 404} si no existe
     */
    @GetMapping("/{id}")
    public ResponseEntity<PlaylistResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(PlaylistResponse.from(getPlaylistByIdUseCase.execute(id)));
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
     * Exporta una playlist de ECOS a la cuenta de Spotify del usuario.
     * <p>
     * Requiere que el usuario haya vinculado previamente su cuenta de Spotify
     * mediante el flujo OAuth en {@code /v1/auth/spotify/login}.
     * </p>
     *
     * @param id identificador de la playlist a exportar
     * @return {@code 200 OK} con la playlist actualizada (incluyendo el ID de Spotify)
     */
    @PostMapping("/{id}/export/spotify")
    public ResponseEntity<PlaylistResponse> exportToSpotify(@PathVariable UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Playlist playlist = exportPlaylistToSpotifyUseCase.execute(id, userId);
        return ResponseEntity.ok(PlaylistResponse.from(playlist));
    }

}
