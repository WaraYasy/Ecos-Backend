package es.wara.ecos.backend.interfaces.rest.controller;

import es.wara.ecos.backend.application.usecase.playlist.AddSongToPlaylistUseCase;
import es.wara.ecos.backend.application.usecase.playlist.CreatePlaylistUseCase;
import es.wara.ecos.backend.application.usecase.playlist.DeletePlaylistUseCase;
import es.wara.ecos.backend.application.usecase.playlist.ExportPlaylistToSpotifyUseCase;
import es.wara.ecos.backend.application.usecase.playlist.GetPlaylistByIdUseCase;
import es.wara.ecos.backend.application.usecase.playlist.GetUserPlaylistsUseCase;
import es.wara.ecos.backend.application.usecase.playlist.RemoveSongFromPlaylistUseCase;
import es.wara.ecos.backend.domain.model.Playlist;
import es.wara.ecos.backend.infrastructure.security.JwtAuthFilter;
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

import es.wara.ecos.backend.domain.exception.ForbiddenException;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @BeforeEach
    void setUpSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("5", null, List.of()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private Playlist buildPlaylist(String spotifyExportId) {
        return new Playlist(1L, 5L, "Mi playlist", "desc", spotifyExportId, LocalDateTime.now(), List.of());
    }

    @Test
    void exportToSpotify_validRequest_returns200WithPlaylistResponse() throws Exception {
        Playlist exported = buildPlaylist("spotify-pl-id");
        when(exportPlaylistToSpotifyUseCase.execute(1L, 5L)).thenReturn(exported);

        mockMvc.perform(post("/v1/playlists/1/export/spotify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Mi playlist"));
    }

    @Test
    void create_validBody_returns201() throws Exception {
        when(createPlaylistUseCase.execute(5L, "Mi playlist", "desc"))
                .thenReturn(buildPlaylist(null));

        mockMvc.perform(post("/v1/playlists")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name": "Mi playlist", "description": "desc"}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void getById_nonExistingPlaylist_returns404() throws Exception {
        when(getPlaylistByIdUseCase.execute(99L))
                .thenThrow(new IllegalArgumentException("Playlist no encontrada: 99"));

        mockMvc.perform(get("/v1/playlists/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void delete_ownPlaylist_returns204() throws Exception {
        doNothing().when(deletePlaylistUseCase).execute(eq(1L), eq(5L));

        mockMvc.perform(delete("/v1/playlists/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_foreignPlaylist_returns403() throws Exception {
        doThrow(new ForbiddenException("No tienes permiso para eliminar esta playlist"))
                .when(deletePlaylistUseCase).execute(eq(1L), eq(5L));

        mockMvc.perform(delete("/v1/playlists/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void addSong_ownPlaylist_returns204() throws Exception {
        doNothing().when(addSongToPlaylistUseCase).execute(eq(1L), eq(20L), eq(5L));

        mockMvc.perform(post("/v1/playlists/1/songs")
                        .contentType(APPLICATION_JSON)
                        .content("{\"songId\": 20}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void removeSong_ownPlaylist_returns204() throws Exception {
        doNothing().when(removeSongFromPlaylistUseCase).execute(eq(1L), anyLong(), eq(5L));

        mockMvc.perform(delete("/v1/playlists/1/songs/10"))
                .andExpect(status().isNoContent());
    }

}
