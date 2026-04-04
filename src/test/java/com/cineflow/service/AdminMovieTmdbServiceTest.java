package com.cineflow.service;

import com.cineflow.dto.AdminTmdbMovieDetailDto;
import com.cineflow.dto.AdminTmdbMovieSearchResultDto;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminMovieTmdbServiceTest {

    @Mock
    private TmdbClient tmdbClient;

    private AdminMovieTmdbService adminMovieTmdbService;

    @BeforeEach
    void setUp() {
        adminMovieTmdbService = new AdminMovieTmdbService(tmdbClient);
    }

    @Test
    void searchMoviesReturnsEmptyListWithoutCallingTmdbWhenQueryIsBlank() {
        List<AdminTmdbMovieSearchResultDto> result = adminMovieTmdbService.searchMovies("   ");

        assertThat(result).isEmpty();
        verifyNoInteractions(tmdbClient);
    }

    @Test
    void getMovieDetailMapsStoredFieldsForAdminFormUsage() {
        TmdbGenreDto genreOne = new TmdbGenreDto();
        genreOne.setId(1L);
        genreOne.setName("Drama");

        TmdbGenreDto genreTwo = new TmdbGenreDto();
        genreTwo.setId(2L);
        genreTwo.setName("Thriller");

        TmdbMovieDetailDto detail = new TmdbMovieDetailDto();
        detail.setId(101L);
        detail.setTitle("Parasite");
        detail.setOriginalTitle("Gisaengchung");
        detail.setOverview("TMDB overview");
        detail.setReleaseDate(LocalDate.of(2026, 4, 10));
        detail.setRuntime(132);
        detail.setPosterPath("/poster.jpg");
        detail.setBackdropPath("/backdrop.jpg");
        detail.setGenres(List.of(genreOne, genreTwo));

        when(tmdbClient.getMovieDetail(101L)).thenReturn(detail);
        when(tmdbClient.buildPosterUrl("/poster.jpg")).thenReturn("https://image.tmdb.org/t/p/w500/poster.jpg");
        when(tmdbClient.buildBackdropUrl("/backdrop.jpg")).thenReturn("https://image.tmdb.org/t/p/w1280/backdrop.jpg");

        AdminTmdbMovieDetailDto result = adminMovieTmdbService.getMovieDetail(101L);

        assertThat(result.getTmdbId()).isEqualTo(101L);
        assertThat(result.getTitle()).isEqualTo("Parasite");
        assertThat(result.getOriginalTitle()).isEqualTo("Gisaengchung");
        assertThat(result.getOverview()).isEqualTo("TMDB overview");
        assertThat(result.getRunningTime()).isEqualTo(132);
        assertThat(result.getGenreText()).isEqualTo("Drama, Thriller");
        assertThat(result.getGenres()).containsExactly("Drama", "Thriller");
        assertThat(result.getPosterPath()).isEqualTo("/poster.jpg");
        assertThat(result.getBackdropPath()).isEqualTo("/backdrop.jpg");
        assertThat(result.getPosterUrl()).isEqualTo("https://image.tmdb.org/t/p/w500/poster.jpg");
        assertThat(result.getBackdropUrl()).isEqualTo("https://image.tmdb.org/t/p/w1280/backdrop.jpg");
    }

    @Test
    void searchMoviesMapsResultsAndOverviewSnippet() {
        TmdbMovieSummaryDto summary = new TmdbMovieSummaryDto();
        summary.setId(202L);
        summary.setTitle("Interstellar");
        summary.setReleaseDate(LocalDate.of(2026, 4, 12));
        summary.setPosterPath("/poster.jpg");
        summary.setOverview("A very long overview that should still be kept available to the admin search result for quick selection.");

        TmdbMovieSearchResponseDto response = new TmdbMovieSearchResponseDto();
        response.setResults(List.of(summary));

        when(tmdbClient.searchMovies("Interstellar")).thenReturn(response);
        when(tmdbClient.buildPosterUrl("/poster.jpg")).thenReturn("https://image.tmdb.org/t/p/w500/poster.jpg");

        List<AdminTmdbMovieSearchResultDto> result = adminMovieTmdbService.searchMovies("Interstellar");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTmdbId()).isEqualTo(202L);
        assertThat(result.get(0).getTitle()).isEqualTo("Interstellar");
        assertThat(result.get(0).getPosterPreviewUrl()).isEqualTo("https://image.tmdb.org/t/p/w500/poster.jpg");
        assertThat(result.get(0).getOverviewSnippet()).startsWith("A very long overview");
    }
}
