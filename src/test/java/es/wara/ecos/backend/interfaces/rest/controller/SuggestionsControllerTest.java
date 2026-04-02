package es.wara.ecos.backend.interfaces.rest.controller;

import es.wara.ecos.backend.application.service.SuggestionsService;
import es.wara.ecos.backend.infrastructure.security.JwtAuthFilter;
import es.wara.ecos.backend.infrastructure.security.RateLimitFilter;
import es.wara.ecos.backend.interfaces.rest.dto.SuggestionResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SuggestionsController.class)
@AutoConfigureMockMvc(addFilters = false)
class SuggestionsControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean JwtAuthFilter jwtAuthFilter;
    @MockitoBean RateLimitFilter rateLimitFilter;
    @MockitoBean SuggestionsService suggestionsService;

    @Test
    void suggest_movieType_returnsTmdbSuggestions() throws Exception {
        when(suggestionsService.suggestMovies("avengers"))
                .thenReturn(List.of(new SuggestionResponse("The Avengers", "The Avengers (2012)")));

        mockMvc.perform(get("/v1/suggestions").param("type", "movie").param("q", "avengers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].value").value("The Avengers"))
                .andExpect(jsonPath("$[0].label").value("The Avengers (2012)"));
    }

    @Test
    void suggest_aestheticType_returnsAestheticSuggestions() throws Exception {
        when(suggestionsService.suggestAesthetics("dark"))
                .thenReturn(List.of(new SuggestionResponse("darkAcademia", "Dark academia")));

        mockMvc.perform(get("/v1/suggestions").param("type", "aesthetic").param("q", "dark"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].value").value("darkAcademia"));
    }

    @Test
    void suggest_genreType_returnsGenreSuggestions() throws Exception {
        when(suggestionsService.suggestGenres("rock"))
                .thenReturn(List.of(
                        new SuggestionResponse("rock", "Rock"),
                        new SuggestionResponse("indie rock", "Indie rock")));

        mockMvc.perform(get("/v1/suggestions").param("type", "genre").param("q", "rock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].value").value("rock"));
    }

    @Test
    void suggest_artistType_returnsArtistSuggestions() throws Exception {
        when(suggestionsService.suggestArtists("radio"))
                .thenReturn(List.of(new SuggestionResponse("Radiohead", "Radiohead")));

        mockMvc.perform(get("/v1/suggestions").param("type", "artist").param("q", "radio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].value").value("Radiohead"))
                .andExpect(jsonPath("$[0].label").value("Radiohead"));
    }

    @Test
    void suggest_countryType_returnsCountrySuggestions() throws Exception {
        when(suggestionsService.suggestCountries("sp"))
                .thenReturn(List.of(new SuggestionResponse("Spain", "Spain")));

        mockMvc.perform(get("/v1/suggestions").param("type", "country").param("q", "sp"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].value").value("Spain"));
    }

    @Test
    void suggest_unknownType_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/v1/suggestions").param("type", "unknown").param("q", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void suggest_withoutQParam_usesEmptyDefault() throws Exception {
        when(suggestionsService.suggestGenres(""))
                .thenReturn(List.of(new SuggestionResponse("rock", "Rock")));

        mockMvc.perform(get("/v1/suggestions").param("type", "genre"))
                .andExpect(status().isOk());
    }
}
