package com.cineflow.service;

import com.cineflow.domain.Movie;
import com.cineflow.domain.MovieStatus;
import com.cineflow.dto.MovieViewDto;
import com.cineflow.repository.MovieRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovieServiceTest {

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private TmdbClient tmdbClient;

    private MovieService movieService;

    @BeforeEach
    void setUp() {
        movieService = new MovieService(movieRepository, tmdbClient);
    }

    @Test
    void toViewPrefersStoredTmdbMetadataWhenAvailable() {
        Movie movie = Movie.builder()
                .id(1L)
                .title("Parasite")
                .shortDescription("Legacy short description")
                .description("Legacy description")
                .overview("TMDB overview")
                .genre("Drama, Thriller")
                .ageRating("15")
                .runningTime(132)
                .runtimeMinutes(133)
                .posterUrl("/images/uploads/local-poster.jpg")
                .posterPath("/poster.jpg")
                .backdropPath("/backdrop.jpg")
                .bookingRate(32.1)
                .score(9.0)
                .releaseDate(LocalDate.of(2026, 4, 10))
                .status(MovieStatus.NOW_SHOWING)
                .bookingOpen(true)
                .active(true)
                .build();

        when(tmdbClient.buildPosterUrl("/poster.jpg")).thenReturn("https://image.tmdb.org/t/p/w500/poster.jpg");
        when(tmdbClient.buildBackdropUrl("/backdrop.jpg")).thenReturn("https://image.tmdb.org/t/p/w1280/backdrop.jpg");

        MovieViewDto result = movieService.toView(movie);

        assertThat(result.getPosterUrl()).isEqualTo("https://image.tmdb.org/t/p/w500/poster.jpg");
        assertThat(result.getBackdropUrl()).isEqualTo("https://image.tmdb.org/t/p/w1280/backdrop.jpg");
        assertThat(result.getDescription()).isEqualTo("TMDB overview");
        assertThat(result.getShortDescription()).isEqualTo("Legacy short description");
        assertThat(result.getRunningTime()).isEqualTo(133);
        assertThat(result.getReleaseDateText()).isEqualTo("개봉 2026.04.10");
    }

    @Test
    void toViewFallsBackToLocalValuesAndDefaultsWhenTmdbMetadataIsMissing() {
        Movie movie = Movie.builder()
                .id(2L)
                .title("Fallback Movie")
                .posterUrl("/images/uploads/existing-poster.jpg")
                .status(MovieStatus.COMING_SOON)
                .bookingOpen(false)
                .active(true)
                .build();

        MovieViewDto result = movieService.toView(movie);

        assertThat(result.getPosterUrl()).isEqualTo("/images/uploads/existing-poster.jpg");
        assertThat(result.getBackdropUrl()).isEqualTo("/images/uploads/existing-poster.jpg");
        assertThat(result.getDescription()).isEqualTo("상세 설명을 준비 중입니다.");
        assertThat(result.getShortDescription()).isEqualTo("영화 소개를 준비 중입니다.");
        assertThat(result.getRunningTimeText()).isEqualTo("상영시간 업데이트 예정");
        assertThat(result.getReleaseDateText()).isEqualTo("개봉일 업데이트 예정");
        assertThat(result.getGenreText()).isEqualTo("장르 정보 업데이트 예정");
    }
}
