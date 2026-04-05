package com.cineflow.controller;

import com.cineflow.domain.MovieStatus;
import com.cineflow.dto.PublicMovieMetadataDto;
import com.cineflow.service.PublicMovieMetadataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class HomeControllerTest {

    @Mock
    private PublicMovieMetadataService publicMovieMetadataService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new HomeController(publicMovieMetadataService))
                .build();
    }

    @Test
    void homeRendersSuccessfullyWithTmdbPublicLists() throws Exception {
        PublicMovieMetadataDto heroMovie = PublicMovieMetadataDto.builder()
                .tmdbId(550L)
                .title("TMDB Hero Movie")
                .status(MovieStatus.NOW_SHOWING)
                .bookingOpen(false)
                .active(false)
                .liveMetadata(true)
                .build();

        when(publicMovieMetadataService.getHeroMovies(3)).thenReturn(List.of(heroMovie));
        when(publicMovieMetadataService.getPopularMovies(8)).thenReturn(List.of(heroMovie));
        when(publicMovieMetadataService.getNowShowingMovies(8)).thenReturn(List.of(heroMovie));
        when(publicMovieMetadataService.getComingSoonMovies(8)).thenReturn(List.of());

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("heroMovies"))
                .andExpect(model().attributeExists("featuredMovie"))
                .andExpect(model().attributeExists("boxOfficeMovies"))
                .andExpect(model().attributeExists("nowShowingMovies"))
                .andExpect(model().attributeExists("comingSoonMovies"));
    }
}
