package es.wara.ecos.backend.interfaces.rest.controller;

import es.wara.ecos.backend.application.usecase.search.SearchSongsUseCase;
import es.wara.ecos.backend.config.SearchSuggestionsProperties;
import es.wara.ecos.backend.config.SearchSuggestionsProperties.SuggestionItem;
import es.wara.ecos.backend.config.SearchSuggestionsProperties.SuggestionParams;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.argThat;
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
    @MockitoBean SearchSuggestionsProperties suggestionsProperties;

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
    void search_noResults_returns404() throws Exception {
        when(searchSongsUseCase.execute(argThat(p -> p.q() == null)))
                .thenReturn(new Page<>(List.of(), 0, 20, 0L));

        mockMvc.perform(get("/v1/search"))
                .andExpect(status().isNotFound());
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
                .thenReturn(pageOf(List.of(buildSong("Bohemian Rhapsody"))));

        mockMvc.perform(get("/v1/search").param("pageSize", "5").param("page", "2"))
                .andExpect(status().isOk());
    }

    @Test
    void search_defaultPageSizeIsTwenty() throws Exception {
        when(searchSongsUseCase.execute(argThat(p -> p.pageSize() == 20)))
                .thenReturn(pageOf(List.of(buildSong("Bohemian Rhapsody"))));

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

    @Test
    void search_withArtist_passesArtistToUseCase() throws Exception {
        when(searchSongsUseCase.execute(argThat(p -> "Radiohead".equals(p.artist()))))
                .thenReturn(pageOf(List.of(buildSong("Creep"))));

        mockMvc.perform(get("/v1/search").param("artist", "Radiohead"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].title").value("Creep"));
    }

    @Test
    void search_withCountry_passesCountryToUseCase() throws Exception {
        when(searchSongsUseCase.execute(argThat(p -> "Spain".equals(p.country()))))
                .thenReturn(pageOf(List.of(buildSong("Malamente"))));

        mockMvc.perform(get("/v1/search").param("country", "Spain"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].title").value("Malamente"));
    }

    @Test
    void search_withArtistAndCountry_passesBothToUseCase() throws Exception {
        when(searchSongsUseCase.execute(argThat(p ->
                "Rosalía".equals(p.artist()) && "Spain".equals(p.country()))))
                .thenReturn(pageOf(List.of(buildSong("Con Altura"))));

        mockMvc.perform(get("/v1/search")
                        .param("artist", "Rosalía")
                        .param("country", "Spain"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].title").value("Con Altura"));
    }

    @Test
    void search_enhancedFalseByDefault_passesEnhancedFalseToUseCase() throws Exception {
        when(searchSongsUseCase.execute(argThat(p -> !p.enhanced())))
                .thenReturn(pageOf(List.of(buildSong("Bohemian Rhapsody"))));

        mockMvc.perform(get("/v1/search").param("q", "rock"))
                .andExpect(status().isOk());
    }

    @Test
    void search_enhancedTrue_passesEnhancedTrueToUseCase() throws Exception {
        when(searchSongsUseCase.execute(argThat(p -> p.enhanced() && "canciones tristes".equals(p.q()))))
                .thenReturn(pageOf(List.of(buildSong("The Sound of Silence"))));

        mockMvc.perform(get("/v1/search")
                        .param("q", "canciones tristes")
                        .param("enhanced", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].title").value("The Sound of Silence"));
    }

    // ─── GET /v1/search/suggestions ───────────────────────────────────────────

    private SuggestionItem buildItem(String label, String genre, String q) {
        SuggestionParams params = new SuggestionParams();
        params.setGenre(genre);
        params.setQ(q);
        SuggestionItem item = new SuggestionItem();
        item.setLabel(label);
        item.setParams(params);
        return item;
    }

    @Test
    void searchSuggestions_returnsAllItems() throws Exception {
        when(suggestionsProperties.getItems()).thenReturn(List.of(
                buildItem("Tarde de jazz", "jazz", null),
                buildItem("Rock de los 80", "rock", null)
        ));

        mockMvc.perform(get("/v1/search/suggestions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].label").value("Tarde de jazz"))
                .andExpect(jsonPath("$[1].label").value("Rock de los 80"));
    }

    @Test
    void searchSuggestions_emptyList_returnsEmptyArray() throws Exception {
        when(suggestionsProperties.getItems()).thenReturn(List.of());

        mockMvc.perform(get("/v1/search/suggestions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void searchSuggestions_nullParamsOmittedFromJson() throws Exception {
        when(suggestionsProperties.getItems()).thenReturn(List.of(
                buildItem("Estudiar bajo la lluvia", null, "study lofi rain")
        ));

        mockMvc.perform(get("/v1/search/suggestions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].params.q").value("study lofi rain"))
                .andExpect(jsonPath("$[0].params.genre").doesNotExist());
    }

    @Test
    void searchSuggestions_energyParamsSerializedCorrectly() throws Exception {
        SuggestionParams params = new SuggestionParams();
        params.setGenre("hiphop");
        params.setEnergyMin(new BigDecimal("0.7"));
        SuggestionItem item = new SuggestionItem();
        item.setLabel("Hip-hop para entrenar");
        item.setParams(params);

        when(suggestionsProperties.getItems()).thenReturn(List.of(item));

        mockMvc.perform(get("/v1/search/suggestions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].params.energyMin").value(0.7))
                .andExpect(jsonPath("$[0].params.energyMax").doesNotExist());
    }

}