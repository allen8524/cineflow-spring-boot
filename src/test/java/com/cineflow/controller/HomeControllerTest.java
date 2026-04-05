package com.cineflow.controller;

import com.cineflow.domain.Movie;
import com.cineflow.domain.MovieStatus;
import com.cineflow.dto.PublicMovieMetadataDto;
import com.cineflow.service.MovieService;
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
    private MovieService movieService;

    @Mock
    private PublicMovieMetadataService publicMovieMetadataService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new HomeController(movieService, publicMovieMetadataService))
                .build();
    }

    @Test
    void homeRendersSuccessfullyWhenLiveMetadataResolutionFails() throws Exception {
        Movie movie = Movie.builder()
                .id(1L)
                .title("Fallback Movie")
                .status(MovieStatus.NOW_SHOWING)
                .bookingOpen(true)
                .active(true)
                .build();

        PublicMovieMetadataDto fallbackMetadata = PublicMovieMetadataDto.builder()
                .localMovieId(1L)
                .title("Fallback Movie")
                .status(MovieStatus.NOW_SHOWING)
                .bookingOpen(true)
                .active(true)
                .build();

        when(movieService.getFeaturedMovies(3)).thenReturn(List.of(movie));
        when(movieService.getBoxOfficeMovies(8)).thenReturn(List.of(movie));
        when(movieService.getNowShowingMovies(8)).thenReturn(List.of(movie));
        when(movieService.getComingSoonMovies(8)).thenReturn(List.of());
        when(publicMovieMetadataService.resolveMetadata(List.of(movie)))
                .thenThrow(new RuntimeException("TMDB timeout"));
        when(publicMovieMetadataService.resolveLocalMetadata(List.of(movie)))
                .thenReturn(List.of(fallbackMetadata));

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
