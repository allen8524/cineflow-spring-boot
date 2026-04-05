package com.cineflow.service;

import com.cineflow.domain.Movie;
import com.cineflow.domain.MovieStatus;
import com.cineflow.dto.PublicMovieMetadataDto;
import com.cineflow.dto.TmdbGenreDto;
import com.cineflow.dto.TmdbMovieDetailDto;
import com.cineflow.dto.TmdbMovieSearchResponseDto;
import com.cineflow.dto.TmdbMovieSummaryDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicMovieMetadataServiceTest {

    @Mock
    private TmdbClient tmdbClient;

    private PublicMovieMetadataService publicMovieMetadataService;

    @BeforeEach
    void setUp() {
        publicMovieMetadataService = new PublicMovieMetadataService(tmdbClient);
    }

    @Test
    void resolveMetadataUsesTmdbIdFirstWhenAvailable() {
        Movie movie = Movie.builder()
                .id(1L)
                .tmdbId(550L)
                .title("Fight Club")
                .genre("Drama")
                .ageRating("19")
                .runningTime(139)
                .posterUrl("/images/uploads/local-poster.jpg")
                .releaseDate(LocalDate.of(2026, 4, 10))
                .status(MovieStatus.NOW_SHOWING)
                .bookingOpen(true)
                .active(true)
                .build();

        TmdbGenreDto genre = new TmdbGenreDto();
        genre.setName("Drama");

        TmdbMovieDetailDto detail = new TmdbMovieDetailDto();
        detail.setId(550L);
        detail.setTitle("Fight Club");
        detail.setOriginalTitle("Fight Club");
        detail.setOverview("TMDB live overview");
        detail.setReleaseDate(LocalDate.of(1999, 10, 15));
        detail.setRuntime(139);
        detail.setPosterPath("/fight-club-poster.jpg");
        detail.setBackdropPath("/fight-club-backdrop.jpg");
        detail.setGenres(List.of(genre));

        when(tmdbClient.isConfigured()).thenReturn(true);
        when(tmdbClient.getMovieDetailWithMedia(550L)).thenReturn(detail);
        when(tmdbClient.buildPosterUrl(any())).thenAnswer(invocation -> {
            String path = invocation.getArgument(0);
            return "/fight-club-poster.jpg".equals(path)
                    ? "https://image.tmdb.org/t/p/w500/fight-club-poster.jpg"
                    : null;
        });
        when(tmdbClient.buildBackdropUrl(any())).thenAnswer(invocation -> {
            String path = invocation.getArgument(0);
            return "/fight-club-backdrop.jpg".equals(path)
                    ? "https://image.tmdb.org/t/p/w1280/fight-club-backdrop.jpg"
                    : null;
        });

        PublicMovieMetadataDto result = publicMovieMetadataService.resolveMetadata(movie);

        assertThat(result.getTmdbId()).isEqualTo(550L);
        assertThat(result.getTitle()).isEqualTo("Fight Club");
        assertThat(result.getOverview()).isEqualTo("TMDB live overview");
        assertThat(result.getRuntimeMinutes()).isEqualTo(139);
        assertThat(result.getGenres()).containsExactly("Drama");
        assertThat(result.getPosterUrl()).isEqualTo("https://image.tmdb.org/t/p/w500/fight-club-poster.jpg");
        assertThat(result.getBackdropUrl()).isEqualTo("https://image.tmdb.org/t/p/w1280/fight-club-backdrop.jpg");
        assertThat(result.isLiveMetadata()).isTrue();
        verify(tmdbClient, never()).searchMovies("Fight Club");
    }

    @Test
    void resolveMetadataSearchesByTitleAndPrefersExactTitleWithSameYear() {
        Movie movie = Movie.builder()
                .id(2L)
                .title("Parasite")
                .releaseDate(LocalDate.of(2019, 5, 30))
                .genre("Drama, Thriller")
                .ageRating("15")
                .runningTime(132)
                .status(MovieStatus.NOW_SHOWING)
                .bookingOpen(true)
                .active(true)
                .build();

        TmdbMovieSummaryDto wrongYear = new TmdbMovieSummaryDto();
        wrongYear.setId(1L);
        wrongYear.setTitle("Parasite");
        wrongYear.setReleaseDate(LocalDate.of(2020, 1, 1));

        TmdbMovieSummaryDto exactMatch = new TmdbMovieSummaryDto();
        exactMatch.setId(2L);
        exactMatch.setTitle("Parasite");
        exactMatch.setReleaseDate(LocalDate.of(2019, 5, 30));

        TmdbMovieSearchResponseDto response = new TmdbMovieSearchResponseDto();
        response.setResults(List.of(wrongYear, exactMatch));

        TmdbMovieDetailDto detail = new TmdbMovieDetailDto();
        detail.setId(2L);
        detail.setTitle("Parasite");
        detail.setOriginalTitle("Gisaengchung");
        detail.setOverview("TMDB matched overview");
        detail.setRuntime(132);

        when(tmdbClient.isConfigured()).thenReturn(true);
        when(tmdbClient.searchMovies("Parasite")).thenReturn(response);
        when(tmdbClient.getMovieDetailWithMedia(2L)).thenReturn(detail);

        PublicMovieMetadataDto result = publicMovieMetadataService.resolveMetadata(movie);

        assertThat(result.getTmdbId()).isEqualTo(2L);
        assertThat(result.getOriginalTitle()).isEqualTo("Gisaengchung");
        assertThat(result.getOverview()).isEqualTo("TMDB matched overview");
        assertThat(result.isLiveMetadata()).isTrue();
    }

    @Test
    void resolveMetadataFallsBackToLocalValuesWhenTmdbIsUnavailable() {
        Movie movie = Movie.builder()
                .id(3L)
                .tmdbId(999L)
                .title("Local Only Movie")
                .shortDescription("Local short description")
                .description("Local description")
                .genre("SF · Drama")
                .ageRating("12")
                .runningTime(118)
                .posterUrl("/images/uploads/local-only.jpg")
                .releaseDate(LocalDate.of(2026, 4, 10))
                .status(MovieStatus.COMING_SOON)
                .bookingOpen(false)
                .active(true)
                .build();

        when(tmdbClient.isConfigured()).thenReturn(false);

        PublicMovieMetadataDto result = publicMovieMetadataService.resolveMetadata(movie);

        assertThat(result.getLocalMovieId()).isEqualTo(3L);
        assertThat(result.getTmdbId()).isEqualTo(999L);
        assertThat(result.getTitle()).isEqualTo("Local Only Movie");
        assertThat(result.getOverview()).isEqualTo("Local description");
        assertThat(result.getRuntimeMinutes()).isEqualTo(118);
        assertThat(result.getGenres()).containsExactly("SF", "Drama");
        assertThat(result.getPosterUrl()).isEqualTo("/images/uploads/local-only.jpg");
        assertThat(result.getBackdropUrl()).isEqualTo("/images/uploads/local-only.jpg");
        assertThat(result.isLiveMetadata()).isFalse();
        verify(tmdbClient, never()).getMovieDetailWithMedia(999L);
    }

    @Test
    void resolveMetadataFallsBackToLocalValuesWhenLiveLookupFails() {
        Movie movie = Movie.builder()
                .id(4L)
                .tmdbId(321L)
                .title("Fallback On Error")
                .description("Local fallback description")
                .runningTime(110)
                .posterUrl("/images/uploads/fallback-on-error.jpg")
                .status(MovieStatus.NOW_SHOWING)
                .bookingOpen(true)
                .active(true)
                .build();

        when(tmdbClient.isConfigured()).thenReturn(true);
        when(tmdbClient.getMovieDetailWithMedia(321L))
                .thenThrow(TmdbClientException.network("TMDB request failed because the TMDB server could not be reached. Please try again later.", new RuntimeException("timeout")));

        PublicMovieMetadataDto result = publicMovieMetadataService.resolveMetadata(movie);

        assertThat(result.getTmdbId()).isEqualTo(321L);
        assertThat(result.getOverview()).isEqualTo("Local fallback description");
        assertThat(result.getPosterUrl()).isEqualTo("/images/uploads/fallback-on-error.jpg");
        assertThat(result.isLiveMetadata()).isFalse();
    }

    @Test
    void resolveMetadataFallsBackToLocalValuesWhenSearchReturnsNoResults() {
        Movie movie = Movie.builder()
                .id(5L)
                .title("Unknown Movie")
                .description("Local description")
                .status(MovieStatus.COMING_SOON)
                .bookingOpen(false)
                .active(true)
                .build();

        TmdbMovieSearchResponseDto response = new TmdbMovieSearchResponseDto();
        response.setResults(List.of());

        when(tmdbClient.isConfigured()).thenReturn(true);
        when(tmdbClient.searchMovies("Unknown Movie")).thenReturn(response);

        PublicMovieMetadataDto result = publicMovieMetadataService.resolveMetadata(movie);

        assertThat(result.getTitle()).isEqualTo("Unknown Movie");
        assertThat(result.getOverview()).isEqualTo("Local description");
        assertThat(result.isLiveMetadata()).isFalse();
    }

    @Test
    void resolveMetadataListAvoidsDuplicateLiveLookupWithinSingleRender() {
        Movie first = Movie.builder()
                .id(6L)
                .tmdbId(777L)
                .title("Duplicated Movie")
                .status(MovieStatus.NOW_SHOWING)
                .bookingOpen(true)
                .active(true)
                .build();

        Movie second = Movie.builder()
                .id(7L)
                .tmdbId(777L)
                .title("Duplicated Movie")
                .status(MovieStatus.NOW_SHOWING)
                .bookingOpen(true)
                .active(true)
                .build();

        TmdbMovieDetailDto detail = new TmdbMovieDetailDto();
        detail.setId(777L);
        detail.setTitle("Duplicated Movie");
        detail.setOverview("Live overview");

        when(tmdbClient.isConfigured()).thenReturn(true);
        when(tmdbClient.getMovieDetailWithMedia(777L)).thenReturn(detail);

        List<PublicMovieMetadataDto> results = publicMovieMetadataService.resolveMetadata(List.of(first, second));

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getLocalMovieId()).isEqualTo(6L);
        assertThat(results.get(1).getLocalMovieId()).isEqualTo(7L);
        assertThat(results.get(0).getTmdbId()).isEqualTo(777L);
        assertThat(results.get(1).getTmdbId()).isEqualTo(777L);
        assertThat(results.get(0).isLiveMetadata()).isTrue();
        assertThat(results.get(1).isLiveMetadata()).isTrue();
        verify(tmdbClient).getMovieDetailWithMedia(777L);
    }
}
