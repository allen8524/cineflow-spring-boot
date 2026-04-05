package com.cineflow.service;

import com.cineflow.domain.Movie;
import com.cineflow.domain.MovieStatus;
import com.cineflow.dto.PublicMovieMetadataDto;
import com.cineflow.dto.TmdbGenreDto;
import com.cineflow.dto.TmdbMovieDetailDto;
import com.cineflow.dto.TmdbMovieSearchResponseDto;
import com.cineflow.dto.TmdbMovieSummaryDto;
import com.cineflow.repository.MovieRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicMovieMetadataServiceTest {

    @Mock
    private TmdbClient tmdbClient;

    @Mock
    private MovieRepository movieRepository;

    private MutableClock clock;
    private PublicMovieMetadataService publicMovieMetadataService;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-04-05T00:00:00Z"), ZoneId.of("UTC"));
        publicMovieMetadataService = new PublicMovieMetadataService(tmdbClient, movieRepository).useClock(clock);
    }

    @Test
    void resolveMetadataUsesTmdbIdFirstAndIgnoresLocalSeedMetadata() {
        Movie movie = Movie.builder()
                .id(1L)
                .tmdbId(550L)
                .title("시간의 궤도")
                .description("로컬 시드 설명")
                .genre("SF")
                .ageRating("12")
                .runningTime(132)
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

        assertThat(result.getLocalMovieId()).isEqualTo(1L);
        assertThat(result.getTmdbId()).isEqualTo(550L);
        assertThat(result.getTitle()).isEqualTo("Fight Club");
        assertThat(result.getOverview()).isEqualTo("TMDB live overview");
        assertThat(result.getRuntimeMinutes()).isEqualTo(139);
        assertThat(result.getGenres()).containsExactly("Drama");
        assertThat(result.getPosterUrl()).isEqualTo("https://image.tmdb.org/t/p/w500/fight-club-poster.jpg");
        assertThat(result.getBackdropUrl()).isEqualTo("https://image.tmdb.org/t/p/w1280/fight-club-backdrop.jpg");
        assertThat(result.isLiveMetadata()).isTrue();
        assertThat(result.getTitle()).isNotEqualTo("시간의 궤도");
        assertThat(result.getOverview()).isNotEqualTo("로컬 시드 설명");
        verify(tmdbClient, never()).searchMovies("시간의 궤도");
    }

    @Test
    void resolveMetadataSearchesByTitleWhenTmdbIdIsMissing() {
        Movie movie = Movie.builder()
                .id(2L)
                .title("Parasite")
                .releaseDate(LocalDate.of(2019, 5, 30))
                .status(MovieStatus.NOW_SHOWING)
                .bookingOpen(true)
                .active(true)
                .build();

        TmdbMovieSummaryDto exactMatch = new TmdbMovieSummaryDto();
        exactMatch.setId(2L);
        exactMatch.setTitle("Parasite");
        exactMatch.setReleaseDate(LocalDate.of(2019, 5, 30));

        TmdbMovieSearchResponseDto response = new TmdbMovieSearchResponseDto();
        response.setResults(List.of(exactMatch));

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
    void getPopularMoviesUsesTmdbSummaryAndLinksMatchingLocalMovie() {
        Movie linkedMovie = Movie.builder()
                .id(55L)
                .tmdbId(101L)
                .title("보이스 노이즈")
                .status(MovieStatus.NOW_SHOWING)
                .bookingOpen(true)
                .active(true)
                .build();

        TmdbMovieSummaryDto summary = new TmdbMovieSummaryDto();
        summary.setId(101L);
        summary.setTitle("Real Popular Movie");
        summary.setOriginalTitle("Real Popular Movie");
        summary.setOverview("Popular TMDB overview");
        summary.setReleaseDate(LocalDate.of(2026, 4, 5));
        summary.setPosterPath("/popular-poster.jpg");
        summary.setBackdropPath("/popular-backdrop.jpg");

        TmdbMovieSearchResponseDto response = new TmdbMovieSearchResponseDto();
        response.setResults(List.of(summary));

        when(tmdbClient.isConfigured()).thenReturn(true);
        when(tmdbClient.getPopularMovies()).thenReturn(response);
        when(tmdbClient.buildPosterUrl("/popular-poster.jpg")).thenReturn("https://image.tmdb.org/t/p/w500/popular-poster.jpg");
        when(tmdbClient.buildBackdropUrl("/popular-backdrop.jpg")).thenReturn("https://image.tmdb.org/t/p/w1280/popular-backdrop.jpg");
        when(movieRepository.findAllByActiveTrueAndTmdbIdIn(anyCollection())).thenReturn(List.of(linkedMovie));

        List<PublicMovieMetadataDto> result = publicMovieMetadataService.getPopularMovies(8);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLocalMovieId()).isEqualTo(55L);
        assertThat(result.get(0).getTitle()).isEqualTo("Real Popular Movie");
        assertThat(result.get(0).isBookable()).isTrue();
    }

    @Test
    void getMovieDetailUsesTmdbRouteIdWhenNoLocalMovieExists() {
        TmdbMovieDetailDto detail = new TmdbMovieDetailDto();
        detail.setId(900L);
        detail.setTitle("TMDB Direct Movie");
        detail.setOriginalTitle("TMDB Direct Movie");
        detail.setOverview("Direct detail overview");

        when(movieRepository.findByIdAndActiveTrue(900L)).thenReturn(Optional.empty());
        when(movieRepository.findByTmdbIdAndActiveTrue(900L)).thenReturn(Optional.empty());
        when(tmdbClient.isConfigured()).thenReturn(true);
        when(tmdbClient.getMovieDetailWithMedia(900L)).thenReturn(detail);

        PublicMovieMetadataDto result = publicMovieMetadataService.getMovieDetail(900L);

        assertThat(result.getLocalMovieId()).isNull();
        assertThat(result.getTmdbId()).isEqualTo(900L);
        assertThat(result.getTitle()).isEqualTo("TMDB Direct Movie");
        assertThat(result.isLiveMetadata()).isTrue();
    }

    @Test
    void resolveMetadataFallsBackToNeutralPlaceholderWhenTmdbIsUnavailable() {
        Movie movie = Movie.builder()
                .id(3L)
                .tmdbId(999L)
                .title("Local Only Movie")
                .description("Local description")
                .status(MovieStatus.COMING_SOON)
                .bookingOpen(false)
                .active(true)
                .build();

        when(tmdbClient.isConfigured()).thenReturn(false);

        PublicMovieMetadataDto result = publicMovieMetadataService.resolveMetadata(movie);

        assertThat(result.getLocalMovieId()).isEqualTo(3L);
        assertThat(result.getTmdbId()).isEqualTo(999L);
        assertThat(result.getTitle()).isEqualTo("영화 정보 준비 중");
        assertThat(result.getOverview()).isEqualTo("현재 영화 소개를 불러오는 중입니다. 잠시 후 다시 확인해 주세요.");
        assertThat(result.getPosterUrl()).isEqualTo("/images/uploads/movie-single.jpg");
        assertThat(result.isLiveMetadata()).isFalse();
    }

    @Test
    void resolveMetadataUsesLiveCacheAcrossRepeatedCallsWithinTtl() {
        Movie movie = Movie.builder()
                .id(6L)
                .tmdbId(777L)
                .title("Cached Live Movie")
                .status(MovieStatus.NOW_SHOWING)
                .bookingOpen(true)
                .active(true)
                .build();

        TmdbMovieDetailDto detail = new TmdbMovieDetailDto();
        detail.setId(777L);
        detail.setTitle("Cached Live Movie");
        detail.setOverview("Live overview");

        when(tmdbClient.isConfigured()).thenReturn(true);
        when(tmdbClient.getMovieDetailWithMedia(777L)).thenReturn(detail);

        PublicMovieMetadataDto first = publicMovieMetadataService.resolveMetadata(movie);
        PublicMovieMetadataDto second = publicMovieMetadataService.resolveMetadata(movie);

        assertThat(first.isLiveMetadata()).isTrue();
        assertThat(second.isLiveMetadata()).isTrue();
        verify(tmdbClient, times(1)).getMovieDetailWithMedia(777L);
    }

    @Test
    void resolveMetadataRefreshesLiveCacheAfterTtlExpires() {
        Movie movie = Movie.builder()
                .id(7L)
                .tmdbId(888L)
                .title("Expiring Live Movie")
                .status(MovieStatus.NOW_SHOWING)
                .bookingOpen(true)
                .active(true)
                .build();

        TmdbMovieDetailDto firstDetail = new TmdbMovieDetailDto();
        firstDetail.setId(888L);
        firstDetail.setTitle("Expiring Live Movie");
        firstDetail.setOverview("First live overview");

        TmdbMovieDetailDto refreshedDetail = new TmdbMovieDetailDto();
        refreshedDetail.setId(888L);
        refreshedDetail.setTitle("Expiring Live Movie");
        refreshedDetail.setOverview("Refreshed live overview");

        when(tmdbClient.isConfigured()).thenReturn(true);
        when(tmdbClient.getMovieDetailWithMedia(888L)).thenReturn(firstDetail, refreshedDetail);

        PublicMovieMetadataDto first = publicMovieMetadataService.resolveMetadata(movie);
        clock.advance(Duration.ofMinutes(6));
        PublicMovieMetadataDto second = publicMovieMetadataService.resolveMetadata(movie);

        assertThat(first.getOverview()).isEqualTo("First live overview");
        assertThat(second.getOverview()).isEqualTo("Refreshed live overview");
        verify(tmdbClient, times(2)).getMovieDetailWithMedia(888L);
    }

    @Test
    void resolveMetadataCachesFallbackOnlyBrieflyBeforeRetryingLiveLookup() {
        Movie movie = Movie.builder()
                .id(8L)
                .tmdbId(4321L)
                .title("Recovering Movie")
                .status(MovieStatus.NOW_SHOWING)
                .bookingOpen(true)
                .active(true)
                .build();

        TmdbMovieDetailDto recoveredDetail = new TmdbMovieDetailDto();
        recoveredDetail.setId(4321L);
        recoveredDetail.setTitle("Recovering Movie");
        recoveredDetail.setOverview("Recovered live overview");

        when(tmdbClient.isConfigured()).thenReturn(true);
        when(tmdbClient.getMovieDetailWithMedia(4321L))
                .thenThrow(TmdbClientException.network(
                        "TMDB request failed because the TMDB server could not be reached. Please try again later.",
                        new RuntimeException("timeout")
                ))
                .thenReturn(recoveredDetail);

        PublicMovieMetadataDto first = publicMovieMetadataService.resolveMetadata(movie);
        PublicMovieMetadataDto second = publicMovieMetadataService.resolveMetadata(movie);
        clock.advance(Duration.ofSeconds(31));
        PublicMovieMetadataDto third = publicMovieMetadataService.resolveMetadata(movie);

        assertThat(first.isLiveMetadata()).isFalse();
        assertThat(second.isLiveMetadata()).isFalse();
        assertThat(third.isLiveMetadata()).isTrue();
        assertThat(third.getOverview()).isEqualTo("Recovered live overview");
        verify(tmdbClient, times(2)).getMovieDetailWithMedia(4321L);
    }

    @Test
    void resolveMetadataListAvoidsDuplicateLiveLookupWithinSingleRender() {
        Movie first = Movie.builder()
                .id(9L)
                .tmdbId(9999L)
                .title("Duplicated Movie")
                .status(MovieStatus.NOW_SHOWING)
                .bookingOpen(true)
                .active(true)
                .build();

        Movie second = Movie.builder()
                .id(10L)
                .tmdbId(9999L)
                .title("Duplicated Movie")
                .status(MovieStatus.NOW_SHOWING)
                .bookingOpen(true)
                .active(true)
                .build();

        TmdbMovieDetailDto detail = new TmdbMovieDetailDto();
        detail.setId(9999L);
        detail.setTitle("Duplicated Movie");
        detail.setOverview("Live overview");

        when(tmdbClient.isConfigured()).thenReturn(true);
        when(tmdbClient.getMovieDetailWithMedia(9999L)).thenReturn(detail);

        List<PublicMovieMetadataDto> results = publicMovieMetadataService.resolveMetadata(List.of(first, second));

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getLocalMovieId()).isEqualTo(9L);
        assertThat(results.get(1).getLocalMovieId()).isEqualTo(10L);
        assertThat(results.get(0).getTmdbId()).isEqualTo(9999L);
        assertThat(results.get(1).getTmdbId()).isEqualTo(9999L);
        assertThat(results.get(0).isLiveMetadata()).isTrue();
        assertThat(results.get(1).isLiveMetadata()).isTrue();
        verify(tmdbClient, times(1)).getMovieDetailWithMedia(9999L);
    }

    private static final class MutableClock extends Clock {

        private Instant currentInstant;
        private final ZoneId zoneId;

        private MutableClock(Instant currentInstant, ZoneId zoneId) {
            this.currentInstant = currentInstant;
            this.zoneId = zoneId;
        }

        private void advance(Duration duration) {
            currentInstant = currentInstant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return zoneId;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(currentInstant, zone);
        }

        @Override
        public Instant instant() {
            return currentInstant;
        }
    }
}
