package es.wara.ecos.backend.interfaces.rest.controller;

import es.wara.ecos.backend.application.usecase.playlist.AddSongToPlaylistUseCase;
import es.wara.ecos.backend.application.usecase.playlist.CreatePlaylistUseCase;
import es.wara.ecos.backend.application.usecase.playlist.DeletePlaylistUseCase;
import es.wara.ecos.backend.application.usecase.playlist.ExportPlaylistToSpotifyUseCase;
import es.wara.ecos.backend.application.usecase.playlist.RemoveSongFromPlaylistUseCase;
import es.wara.ecos.backend.domain.model.Playlist;
import es.wara.ecos.backend.domain.port.PlaylistRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PlaylistController.class)
class PlaylistControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PlaylistRepository playlistRepository;

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

    private Playlist buildPlaylist(String spotifyExportId) {
        return new Playlist(1L, 5L, "Mi playlist", "desc", spotifyExportId, LocalDateTime.now(), List.of());
    }

    @Test
    void exportToSpotify_validRequest_returns200WithPlaylistResponse() throws Exception {
        Playlist exported = buildPlaylist("spotify-pl-id");
        when(exportPlaylistToSpotifyUseCase.execute(1L, 5L)).thenReturn(exported);

        mockMvc.perform(post("/v1/playlists/1/export/spotify").param("userId", "5"))
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
                                {"name": "Mi playlist", "description": "desc", "userId": 5}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void getById_nonExistingPlaylist_returns404() throws Exception {
        when(playlistRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/v1/playlists/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

}
