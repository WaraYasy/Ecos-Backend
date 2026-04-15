package es.wara.ecos.backend.interfaces.rest.controller;

import es.wara.ecos.backend.application.usecase.playlist.AddSongToPlaylistUseCase;
import es.wara.ecos.backend.application.usecase.playlist.CreatePlaylistUseCase;
import es.wara.ecos.backend.application.usecase.playlist.DeletePlaylistUseCase;
import es.wara.ecos.backend.application.usecase.playlist.ExportPlaylistToSpotifyUseCase;
import es.wara.ecos.backend.application.usecase.playlist.GetPlaylistByIdUseCase;
import es.wara.ecos.backend.application.usecase.playlist.GetUserPlaylistsUseCase;
import es.wara.ecos.backend.application.usecase.playlist.RemoveSongFromPlaylistUseCase;
import es.wara.ecos.backend.domain.exception.ForbiddenException;
import es.wara.ecos.backend.domain.model.Playlist;
import es.wara.ecos.backend.domain.port.PlaylistRepository;
import es.wara.ecos.backend.infrastructure.security.JwtAuthFilter;
import es.wara.ecos.backend.infrastructure.security.RateLimitFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PlaylistController.class)
@AutoConfigureMockMvc(addFilters = false)
class PlaylistControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    RateLimitFilter rateLimitFilter;

    @MockitoBean
    GetPlaylistByIdUseCase getPlaylistByIdUseCase;

    @MockitoBean
    GetUserPlaylistsUseCase getUserPlaylistsUseCase;

    @MockitoBean
    CreatePlaylistUseCase createPlaylistUseCase;

    @MockitoBean
    DeletePlaylistUseCase deletePlaylistUseCase;

    @MockitoBean
    AddSongToPlaylistUseCase addSongToPlaylistUseCase;

    @MockitoBean
    RemoveSongFromPlaylistUseCase removeSongFromPlaylistUseCase;

    @MockitoBean
    ExportPlaylistToSpotifyUseCase exportPlaylistToSpotifyUseCase;

    @MockitoBean
    PlaylistRepository playlistRepository;

    private static final UUID USER_ID     = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID PLAYLIST_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SONG_ID     = UUID.fromString("00000000-0000-0000-0000-000000000020");
    private static final UUID SONG_10     = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID UNKNOWN_ID  = UUID.fromString("00000000-0000-0000-0000-000000000099");

    @BeforeEach
    void setUpSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USER_ID.toString(), null, List.of()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private Playlist buildPlaylist(String spotifyExportId) {
        return new Playlist(PLAYLIST_ID, USER_ID, "Mi playlist", "desc", spotifyExportId, LocalDateTime.now(), List.of());
    }

    @Test
    void exportToSpotify_validRequest_returns200WithPlaylistResponse() throws Exception {
        Playlist exported = buildPlaylist("spotify-pl-id");
        when(exportPlaylistToSpotifyUseCase.execute(PLAYLIST_ID, USER_ID)).thenReturn(exported);

        mockMvc.perform(post("/v1/playlists/" + PLAYLIST_ID + "/export/spotify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PLAYLIST_ID.toString()))
                .andExpect(jsonPath("$.name").value("Mi playlist"));
    }

    @Test
    void create_validBody_returns201() throws Exception {
        when(createPlaylistUseCase.execute(USER_ID, "Mi playlist", "desc"))
                .thenReturn(buildPlaylist(null));

        mockMvc.perform(post("/v1/playlists")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name": "Mi playlist", "description": "desc"}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void getById_ownPlaylist_returns200() throws Exception {
        when(getPlaylistByIdUseCase.execute(PLAYLIST_ID)).thenReturn(buildPlaylist(null));

        mockMvc.perform(get("/v1/playlists/" + PLAYLIST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PLAYLIST_ID.toString()))
                .andExpect(jsonPath("$.name").value("Mi playlist"));
    }

    @Test
    void getById_foreignPlaylist_returns403() throws Exception {
        // La playlist pertenece a otro usuario — el usuario autenticado no debe poder leerla
        UUID otherUserId = UUID.fromString("00000000-0000-0000-0000-000000000099");
        Playlist foreignPlaylist = new Playlist(PLAYLIST_ID, otherUserId, "Ajena", null, null, LocalDateTime.now(), List.of());
        when(getPlaylistByIdUseCase.execute(PLAYLIST_ID)).thenReturn(foreignPlaylist);

        mockMvc.perform(get("/v1/playlists/" + PLAYLIST_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void getById_nonExistingPlaylist_returns404() throws Exception {
        when(getPlaylistByIdUseCase.execute(UNKNOWN_ID))
                .thenThrow(new IllegalArgumentException("Playlist no encontrada: " + UNKNOWN_ID));

        mockMvc.perform(get("/v1/playlists/" + UNKNOWN_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void delete_ownPlaylist_returns204() throws Exception {
        doNothing().when(deletePlaylistUseCase).execute(eq(PLAYLIST_ID), eq(USER_ID));

        mockMvc.perform(delete("/v1/playlists/" + PLAYLIST_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_foreignPlaylist_returns403() throws Exception {
        doThrow(new ForbiddenException("No tienes permiso para eliminar esta playlist"))
                .when(deletePlaylistUseCase).execute(eq(PLAYLIST_ID), eq(USER_ID));

        mockMvc.perform(delete("/v1/playlists/" + PLAYLIST_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void addSong_ownPlaylist_returns204() throws Exception {
        doNothing().when(addSongToPlaylistUseCase).execute(eq(PLAYLIST_ID), eq(SONG_ID), eq(USER_ID));

        mockMvc.perform(post("/v1/playlists/" + PLAYLIST_ID + "/songs")
                        .contentType(APPLICATION_JSON)
                        .content("{\"songId\": \"00000000-0000-0000-0000-000000000020\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void removeSong_ownPlaylist_returns204() throws Exception {
        doNothing().when(removeSongFromPlaylistUseCase).execute(eq(PLAYLIST_ID), any(UUID.class), eq(USER_ID));

        mockMvc.perform(delete("/v1/playlists/" + PLAYLIST_ID + "/songs/" + SONG_10))
                .andExpect(status().isNoContent());
    }

    @Test
    void update_ownPlaylist_returns200WithUpdatedName() throws Exception {
        Playlist updated = buildPlaylist(null);
        updated.setName("Nuevo nombre");
        when(getPlaylistByIdUseCase.execute(PLAYLIST_ID)).thenReturn(buildPlaylist(null));
        when(playlistRepository.save(any(Playlist.class))).thenReturn(updated);

        mockMvc.perform(patch("/v1/playlists/" + PLAYLIST_ID)
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\": \"Nuevo nombre\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nuevo nombre"));
    }

    @Test
    void update_foreignPlaylist_returns403() throws Exception {
        UUID otherUserId = UUID.fromString("00000000-0000-0000-0000-000000000099");
        Playlist foreignPlaylist = new Playlist(PLAYLIST_ID, otherUserId, "Ajena", null, null, LocalDateTime.now(), List.of());
        when(getPlaylistByIdUseCase.execute(PLAYLIST_ID)).thenReturn(foreignPlaylist);

        mockMvc.perform(patch("/v1/playlists/" + PLAYLIST_ID)
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\": \"Intento\"}"))
                .andExpect(status().isForbidden());
    }

}
