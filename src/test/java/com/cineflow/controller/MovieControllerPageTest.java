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
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
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
    void listRendersSuccessfullyWithTmdbPublicMovies() throws Exception {
        PublicMovieMetadataDto publicMovie = PublicMovieMetadataDto.builder()
                .tmdbId(100L)
                .title("TMDB Public Movie")
                .status(MovieStatus.NOW_SHOWING)
                .bookingOpen(false)
                .active(false)
                .build();

        when(publicMovieMetadataService.getMovieList(24)).thenReturn(List.of(publicMovie));

        mockMvc.perform(get("/movies"))
                .andExpect(status().isOk())
                .andExpect(view().name("movies/list"))
                .andExpect(model().attributeExists("movies"));
    }

    @Test
    void legacyMovieListRedirectsToCanonicalMovieList() throws Exception {
        mockMvc.perform(get("/movielist.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/movies"));
    }

    @Test
    void legacyMovieSingleRedirectsToCanonicalDetailUrlWhenIdIsPresent() throws Exception {
        mockMvc.perform(get("/moviesingle.html").param("id", "101"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/movies/101"));
    }

    @Test
    void legacyMovieSingleRedirectsToMovieListWhenIdIsMissing() throws Exception {
        mockMvc.perform(get("/moviesingle.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/movies"));
    }

    @Test
    void detailRendersSuccessfullyForTmdbOnlyMovie() throws Exception {
        PublicMovieMetadataDto movie = PublicMovieMetadataDto.builder()
                .tmdbId(101L)
                .title("TMDB Only Movie")
                .status(MovieStatus.NOW_SHOWING)
                .bookingOpen(false)
                .active(false)
                .build();

        PublicMovieMetadataDto relatedMovie = PublicMovieMetadataDto.builder()
                .tmdbId(202L)
                .title("Related TMDB Movie")
                .status(MovieStatus.COMING_SOON)
                .bookingOpen(false)
                .active(false)
                .build();

        when(publicMovieMetadataService.getMovieDetail(101L)).thenReturn(movie);
        when(publicMovieMetadataService.getRelatedMovies(101L, 4)).thenReturn(List.of(relatedMovie));

        mockMvc.perform(get("/movies/101"))
                .andExpect(status().isOk())
                .andExpect(view().name("movies/detail"))
                .andExpect(model().attributeExists("movie"))
                .andExpect(model().attributeExists("relatedMovies"))
                .andExpect(model().attributeExists("schedules"));
    }

    @Test
    void detailRendersSchedulesWhenTmdbMovieIsLinkedToLocalBookingMovie() throws Exception {
        Movie linkedMovie = Movie.builder()
                .id(1L)
                .title("Linked Movie")
                .status(MovieStatus.NOW_SHOWING)
                .bookingOpen(true)
                .active(true)
                .build();

        PublicMovieMetadataDto movie = PublicMovieMetadataDto.builder()
                .localMovieId(1L)
                .tmdbId(301L)
                .title("TMDB Linked Movie")
                .status(MovieStatus.NOW_SHOWING)
                .bookingOpen(true)
                .active(true)
                .build();

        when(publicMovieMetadataService.getMovieDetail(1L)).thenReturn(movie);
        when(publicMovieMetadataService.getRelatedMovies(301L, 4)).thenReturn(List.of());
        when(movieService.findActiveMovie(1L)).thenReturn(Optional.of(linkedMovie));
        when(scheduleService.getSchedulesForMovie(1L)).thenReturn(List.of());
        when(scheduleService.getTheaterScheduleGroupsByMovie(1L)).thenReturn(List.of());
        when(theaterService.getTheatersForMovie(1L)).thenReturn(List.of());

        mockMvc.perform(get("/movies/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("movies/detail"))
                .andExpect(model().attributeExists("movie"))
                .andExpect(model().attributeExists("theaters"))
                .andExpect(model().attributeExists("schedules"));
    }
}
