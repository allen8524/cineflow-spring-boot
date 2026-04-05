package com.cineflow.controller;

import com.cineflow.domain.Movie;
import com.cineflow.domain.MovieStatus;
import com.cineflow.dto.PublicMovieMetadataDto;
import com.cineflow.service.MovieService;
import com.cineflow.service.PublicMovieMetadataService;
import com.cineflow.service.ScheduleService;
import com.cineflow.service.TheaterService;
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
class MovieControllerPageTest {

    @Mock
    private MovieService movieService;

    @Mock
    private PublicMovieMetadataService publicMovieMetadataService;

    @Mock
    private TheaterService theaterService;

    @Mock
    private ScheduleService scheduleService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new MovieController(movieService, publicMovieMetadataService, theaterService, scheduleService)
                )
                .build();
    }

    @Test
    void listRendersSuccessfullyWhenLiveMetadataResolutionFails() throws Exception {
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

        when(movieService.getAllMovies()).thenReturn(List.of(movie));
        when(publicMovieMetadataService.resolveMetadata(List.of(movie)))
                .thenThrow(new RuntimeException("TMDB timeout"));
        when(publicMovieMetadataService.resolveLocalMetadata(List.of(movie)))
                .thenReturn(List.of(fallbackMetadata));

        mockMvc.perform(get("/movies"))
                .andExpect(status().isOk())
                .andExpect(view().name("movies/list"))
                .andExpect(model().attributeExists("movies"));
    }

    @Test
    void detailRendersSuccessfullyWhenLiveMetadataResolutionFails() throws Exception {
        Movie movie = Movie.builder()
                .id(1L)
                .title("Main Movie")
                .status(MovieStatus.NOW_SHOWING)
                .bookingOpen(true)
                .active(true)
                .build();

        Movie relatedMovie = Movie.builder()
                .id(2L)
                .title("Related Movie")
                .status(MovieStatus.COMING_SOON)
                .bookingOpen(false)
                .active(true)
                .build();

        PublicMovieMetadataDto movieFallback = PublicMovieMetadataDto.builder()
                .localMovieId(1L)
                .title("Main Movie")
                .status(MovieStatus.NOW_SHOWING)
                .bookingOpen(true)
                .active(true)
                .build();

        PublicMovieMetadataDto relatedFallback = PublicMovieMetadataDto.builder()
                .localMovieId(2L)
                .title("Related Movie")
                .status(MovieStatus.COMING_SOON)
                .bookingOpen(false)
                .active(true)
                .build();

        when(movieService.getMovie(1L)).thenReturn(movie);
        when(movieService.getRelatedMovies(1L, 4)).thenReturn(List.of(relatedMovie));
        when(publicMovieMetadataService.resolveMetadata(List.of(movie, relatedMovie)))
                .thenThrow(new RuntimeException("TMDB timeout"));
        when(publicMovieMetadataService.resolveLocalMetadata(List.of(movie, relatedMovie)))
                .thenReturn(List.of(movieFallback, relatedFallback));
        when(scheduleService.getSchedulesForMovie(1L)).thenReturn(List.of());
        when(scheduleService.getTheaterScheduleGroupsByMovie(1L)).thenReturn(List.of());
        when(theaterService.getTheatersForMovie(1L)).thenReturn(List.of());

        mockMvc.perform(get("/movies/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("movies/detail"))
                .andExpect(model().attributeExists("movie"))
                .andExpect(model().attributeExists("relatedMovies"))
                .andExpect(model().attributeExists("schedules"));
    }
}
