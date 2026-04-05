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
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovieControllerDetailTest {

    @Mock
    private MovieService movieService;

    @Mock
    private PublicMovieMetadataService publicMovieMetadataService;

    @Mock
    private TheaterService theaterService;

    @Mock
    private ScheduleService scheduleService;

    private MovieController movieController;

    @BeforeEach
    void setUp() {
        movieController = new MovieController(movieService, publicMovieMetadataService, theaterService, scheduleService);
    }

    @Test
    void detailUsesTmdbMetadataAndKeepsLocalScheduleConnectionWhenAvailable() {
        Movie linkedMovie = Movie.builder()
                .id(1L)
                .title("Linked Movie")
                .status(MovieStatus.NOW_SHOWING)
                .bookingOpen(true)
                .active(true)
                .build();

        PublicMovieMetadataDto movieMetadata = PublicMovieMetadataDto.builder()
                .localMovieId(1L)
                .tmdbId(700L)
                .title("TMDB Main Movie")
                .status(MovieStatus.NOW_SHOWING)
                .bookingOpen(true)
                .active(true)
                .build();

        PublicMovieMetadataDto relatedMovieMetadata = PublicMovieMetadataDto.builder()
                .tmdbId(701L)
                .title("TMDB Related Movie")
                .status(MovieStatus.COMING_SOON)
                .bookingOpen(false)
                .active(false)
                .build();

        when(publicMovieMetadataService.getMovieDetail(1L)).thenReturn(movieMetadata);
        when(publicMovieMetadataService.getRelatedMovies(700L, 4)).thenReturn(List.of(relatedMovieMetadata));
        when(movieService.findActiveMovie(1L)).thenReturn(Optional.of(linkedMovie));
        when(scheduleService.getSchedulesForMovie(1L)).thenReturn(List.of());
        when(scheduleService.getTheaterScheduleGroupsByMovie(1L)).thenReturn(List.of());
        when(theaterService.getTheatersForMovie(1L)).thenReturn(List.of());

        Model model = new ExtendedModelMap();
        String viewName = movieController.detail(1L, model);

        assertThat(viewName).isEqualTo("movies/detail");
        assertThat(model.getAttribute("movie")).isSameAs(movieMetadata);
        assertThat(model.getAttribute("relatedMovies")).isEqualTo(List.of(relatedMovieMetadata));
        assertThat(model.getAttribute("schedules")).isEqualTo(List.of());
        verify(publicMovieMetadataService).getMovieDetail(1L);
        verify(publicMovieMetadataService).getRelatedMovies(700L, 4);
    }
}
