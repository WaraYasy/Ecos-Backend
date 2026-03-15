package es.wara.ecos.backend.interfaces.rest.controller;

import es.wara.ecos.backend.application.usecase.search.SearchSongsUseCase;
import es.wara.ecos.backend.domain.model.Song;
import es.wara.ecos.backend.infrastructure.security.JwtAuthFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SearchController.class)
@AutoConfigureMockMvc(addFilters = false)
class SearchControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean JwtAuthFilter jwtAuthFilter;
    @MockitoBean SearchSongsUseCase searchSongsUseCase;

    private Song buildSong(String title) {
        Song s = new Song();
        s.setId(UUID.randomUUID());
        s.setTitle(title);
        return s;
    }

    @Test
    void search_withTextQuery_returns200WithResults() throws Exception {
        when(searchSongsUseCase.execute(argThat(p -> "Vangelis".equals(p.q()))))
                .thenReturn(List.of(buildSong("Chariots of Fire")));

        mockMvc.perform(get("/v1/search").param("q", "Vangelis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].title").value("Chariots of Fire"));
    }

    @Test
    void search_noParams_returns200WithEmptyList() throws Exception {
        when(searchSongsUseCase.execute(argThat(p -> p.q() == null))).thenReturn(List.of());

        mockMvc.perform(get("/v1/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void search_withAesthetic_passesAestheticToUseCase() throws Exception {
        when(searchSongsUseCase.execute(argThat(p -> "dark academia".equals(p.aesthetic()))))
                .thenReturn(List.of(buildSong("Clair de Lune")));

        mockMvc.perform(get("/v1/search").param("aesthetic", "dark academia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Clair de Lune"));
    }

    @Test
    void search_withCustomLimitAndOffset_passesThemToUseCase() throws Exception {
        when(searchSongsUseCase.execute(argThat(p -> p.limit() == 5 && p.offset() == 10)))
                .thenReturn(List.of());

        mockMvc.perform(get("/v1/search").param("limit", "5").param("offset", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void search_defaultLimitIsTwenty() throws Exception {
        when(searchSongsUseCase.execute(argThat(p -> p.limit() == 20))).thenReturn(List.of());

        mockMvc.perform(get("/v1/search"))
                .andExpect(status().isOk());
    }

}