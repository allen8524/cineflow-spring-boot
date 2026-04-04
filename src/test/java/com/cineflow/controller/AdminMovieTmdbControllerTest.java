package com.cineflow.controller;

import com.cineflow.dto.AdminTmdbMovieSearchResultDto;
import com.cineflow.service.AdminMovieTmdbService;
import com.cineflow.service.TmdbClientException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminMovieTmdbControllerTest {

    @Mock
    private AdminMovieTmdbService adminMovieTmdbService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminMovieTmdbController(adminMovieTmdbService))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    void searchMoviesReturnsJsonList() throws Exception {
        AdminTmdbMovieSearchResultDto result = AdminTmdbMovieSearchResultDto.builder()
                .tmdbId(10L)
                .title("Parasite")
                .releaseDate(LocalDate.of(2026, 4, 10))
                .posterPreviewUrl("https://image.tmdb.org/t/p/w500/poster.jpg")
                .overviewSnippet("TMDB summary")
                .build();

        when(adminMovieTmdbService.searchMovies("Parasite")).thenReturn(List.of(result));

        mockMvc.perform(get("/admin/movies/tmdb/search").param("query", "Parasite"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].tmdbId").value(10))
                .andExpect(jsonPath("$[0].title").value("Parasite"))
                .andExpect(jsonPath("$[0].overviewSnippet").value("TMDB summary"));
    }

    @Test
    void searchMoviesReturnsBadGatewayWhenNetworkErrorOccurs() throws Exception {
        when(adminMovieTmdbService.searchMovies("Parasite"))
                .thenThrow(TmdbClientException.network("TMDB request failed because the TMDB server could not be reached. Please try again later.", new RuntimeException("timeout")));

        mockMvc.perform(get("/admin/movies/tmdb/search").param("query", "Parasite"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.message").value("TMDB request failed because the TMDB server could not be reached. Please try again later."));
    }

    @Test
    void getMovieDetailReturnsServiceUnavailableWhenTmdbIsNotConfigured() throws Exception {
        when(adminMovieTmdbService.getMovieDetail(1L))
                .thenThrow(TmdbClientException.configuration("TMDB integration is not configured. Check tmdb.base-url and set TMDB_BEARER_TOKEN to enable admin TMDB search."));

        mockMvc.perform(get("/admin/movies/tmdb/1"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message").value("TMDB integration is not configured. Check tmdb.base-url and set TMDB_BEARER_TOKEN to enable admin TMDB search."));
    }

    @Test
    void getMovieDetailReturnsBadRequestForNonNumericTmdbId() throws Exception {
        mockMvc.perform(get("/admin/movies/tmdb/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("TMDB movie id must be a number."));
    }
}
