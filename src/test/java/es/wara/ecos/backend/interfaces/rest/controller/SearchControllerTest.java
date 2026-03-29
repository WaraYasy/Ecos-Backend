package es.wara.ecos.backend.interfaces.rest.controller;

import es.wara.ecos.backend.application.usecase.search.SearchSongsUseCase;
import es.wara.ecos.backend.domain.model.Page;
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
    @MockitoBean RateLimitFilter rateLimitFilter;
    @MockitoBean SearchSongsUseCase searchSongsUseCase;

    private Song buildSong(String title) {
        Song s = new Song();
        s.setId(UUID.randomUUID());
        s.setTitle(title);
        return s;
    }

    private static <T> Page<T> pageOf(List<T> items) {
        return new Page<>(items, 0, 20, items.size());
    }

    @Test
    void search_withTextQuery_returns200WithResults() throws Exception {
        when(searchSongsUseCase.execute(argThat(p -> "Vangelis".equals(p.q()))))
                .thenReturn(pageOf(List.of(buildSong("Chariots of Fire"))));

        mockMvc.perform(get("/v1/search").param("q", "Vangelis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].title").value("Chariots of Fire"));
    }

    @Test
    void search_noParams_returns200WithEmptyList() throws Exception {
        when(searchSongsUseCase.execute(argThat(p -> p.q() == null)))
                .thenReturn(new Page<>(List.of(), 0, 20, 0L));

        mockMvc.perform(get("/v1/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void search_withAesthetic_passesAestheticToUseCase() throws Exception {
        when(searchSongsUseCase.execute(argThat(p -> "dark academia".equals(p.aesthetic()))))
                .thenReturn(pageOf(List.of(buildSong("Clair de Lune"))));

        mockMvc.perform(get("/v1/search").param("aesthetic", "dark academia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].title").value("Clair de Lune"));
    }

    @Test
    void search_withCustomPageSizeAndPage_passesThemToUseCase() throws Exception {
        when(searchSongsUseCase.execute(argThat(p -> p.pageSize() == 5 && p.page() == 2)))
                .thenReturn(new Page<>(List.of(), 2, 5, 0L));

        mockMvc.perform(get("/v1/search").param("pageSize", "5").param("page", "2"))
                .andExpect(status().isOk());
    }

    @Test
    void search_defaultPageSizeIsTwenty() throws Exception {
        when(searchSongsUseCase.execute(argThat(p -> p.pageSize() == 20)))
                .thenReturn(new Page<>(List.of(), 0, 20, 0L));

        mockMvc.perform(get("/v1/search"))
                .andExpect(status().isOk());
    }

    @Test
    void search_withGenre_passesGenreToUseCase() throws Exception {
        when(searchSongsUseCase.execute(argThat(p -> "jazz".equals(p.genre()))))
                .thenReturn(pageOf(List.of(buildSong("Kind of Blue"))));

        mockMvc.perform(get("/v1/search").param("genre", "jazz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].title").value("Kind of Blue"));
    }

    @Test
    void search_withGenreAndAesthetic_passesBothToUseCase() throws Exception {
        when(searchSongsUseCase.execute(argThat(p ->
                "rock".equals(p.genre()) && "dark academia".equals(p.aesthetic()))))
                .thenReturn(pageOf(List.of(buildSong("Creep"))));

        mockMvc.perform(get("/v1/search")
                        .param("genre", "rock")
                        .param("aesthetic", "dark academia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].title").value("Creep"));
    }

}