package es.wara.ecos.backend.interfaces.rest.controller;

import es.wara.ecos.backend.application.usecase.playlist.AddSongToPlaylistUseCase;
import es.wara.ecos.backend.application.usecase.playlist.CreatePlaylistUseCase;
import es.wara.ecos.backend.application.usecase.playlist.DeletePlaylistUseCase;
import es.wara.ecos.backend.application.usecase.playlist.ExportPlaylistToSpotifyUseCase;
import es.wara.ecos.backend.application.usecase.playlist.GetPlaylistByIdUseCase;
import es.wara.ecos.backend.application.usecase.playlist.GetUserPlaylistsUseCase;
import es.wara.ecos.backend.application.usecase.playlist.RemoveSongFromPlaylistUseCase;
import es.wara.ecos.backend.domain.model.Playlist;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @GetMapping
    public ResponseEntity<List<PlaylistResponse>> getByUser(@RequestParam Long userId) {
        List<PlaylistResponse> playlists = getUserPlaylistsUseCase.execute(userId)
                .stream().map(PlaylistResponse::from).toList();
        return ResponseEntity.ok(playlists);
    }

    @PostMapping
    public ResponseEntity<PlaylistResponse> create(@Valid @RequestBody PlaylistRequest request) {
        Playlist playlist = createPlaylistUseCase.execute(
                request.userId(), request.name(), request.description());
        return ResponseEntity.status(HttpStatus.CREATED).body(PlaylistResponse.from(playlist));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlaylistResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(PlaylistResponse.from(getPlaylistByIdUseCase.execute(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        deletePlaylistUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/songs")
    public ResponseEntity<Void> addSong(@PathVariable Long id,
                                        @Valid @RequestBody AddSongRequest request) {
        addSongToPlaylistUseCase.execute(id, request.songId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/songs/{songId}")
    public ResponseEntity<Void> removeSong(@PathVariable Long id, @PathVariable Long songId) {
        removeSongFromPlaylistUseCase.execute(id, songId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/export/spotify")
    public ResponseEntity<PlaylistResponse> exportToSpotify(@PathVariable Long id,
                                                            @RequestParam Long userId) {
        Playlist playlist = exportPlaylistToSpotifyUseCase.execute(id, userId);
        return ResponseEntity.ok(PlaylistResponse.from(playlist));
    }

}
