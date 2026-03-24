package es.wara.ecos.backend.interfaces.rest.controller;

import es.wara.ecos.backend.application.usecase.search.GetSimilarSongsUseCase;
import es.wara.ecos.backend.application.usecase.song.GetSongByIdUseCase;
import es.wara.ecos.backend.domain.model.Song;
import es.wara.ecos.backend.infrastructure.security.JwtAuthFilter;
import es.wara.ecos.backend.infrastructure.security.RateLimitFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SongController.class)
@AutoConfigureMockMvc(addFilters = false)
class SongControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean JwtAuthFilter jwtAuthFilter;
    @MockitoBean RateLimitFilter rateLimitFilter;
    @MockitoBean GetSongByIdUseCase getSongByIdUseCase;
    @MockitoBean GetSimilarSongsUseCase getSimilarSongsUseCase;

    private static final UUID SONG_ID    = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID UNKNOWN_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");

    private Song buildSong() {
        Song song = new Song();
        song.setId(SONG_ID);
        song.setTitle("Chariots of Fire");
        song.setSpotifyId("spotify123");
        return song;
    }

    @Test
    void getById_existingSong_returns200WithData() throws Exception {
        when(getSongByIdUseCase.execute(SONG_ID)).thenReturn(buildSong());

        mockMvc.perform(get("/v1/songs/" + SONG_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(SONG_ID.toString()))
                .andExpect(jsonPath("$.title").value("Chariots of Fire"));
    }

    @Test
    void getById_nonExistingSong_returns404() throws Exception {
        when(getSongByIdUseCase.execute(UNKNOWN_ID))
                .thenThrow(new IllegalArgumentException("Canción no encontrada: " + UNKNOWN_ID));

        mockMvc.perform(get("/v1/songs/" + UNKNOWN_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void getRelated_existingSong_returns200WithList() throws Exception {
        Song related = new Song();
        related.setId(UUID.randomUUID());
        related.setTitle("Related Song");
        when(getSongByIdUseCase.execute(SONG_ID)).thenReturn(buildSong());
        when(getSimilarSongsUseCase.execute(SONG_ID)).thenReturn(List.of(related));

        mockMvc.perform(get("/v1/songs/" + SONG_ID + "/related"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].title").value("Related Song"));
    }

    @Test
    void getRelated_nonExistingSong_returns404() throws Exception {
        when(getSimilarSongsUseCase.execute(UNKNOWN_ID))
                .thenThrow(new IllegalArgumentException("Canción no encontrada: " + UNKNOWN_ID));

        mockMvc.perform(get("/v1/songs/" + UNKNOWN_ID + "/related"))
                .andExpect(status().isNotFound());
    }

}