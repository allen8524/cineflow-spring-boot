package com.cineflow.service;

import com.cineflow.domain.Movie;
import com.cineflow.domain.MovieStatus;
import com.cineflow.dto.PublicMovieMetadataDto;
import com.cineflow.dto.TmdbGenreDto;
import com.cineflow.dto.TmdbMovieDetailDto;
import com.cineflow.dto.TmdbMovieSearchResponseDto;
import com.cineflow.dto.TmdbMovieSummaryDto;
import com.cineflow.dto.TmdbReleaseDateCountryDto;
import com.cineflow.dto.TmdbReleaseDateItemDto;
import com.cineflow.dto.TmdbReleaseDatesDto;
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
    void resolveMetadataUsesTmdbDetailWithLocalDisplayMetadataFirst() {
        Movie movie = Movie.builder()
                .id(1L)
                .tmdbId(550L)
                .title("로컬 테스트 영화")
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
        assertThat(result.getReleaseDate()).isEqualTo(LocalDate.of(1999, 10, 15));
        assertThat(result.getRuntimeMinutes()).isEqualTo(132);
        assertThat(result.getGenres()).containsExactly("SF");
        assertThat(result.getAgeRating()).isEqualTo("12");
        assertThat(result.getRunningTimeText()).isEqualTo("132분");
        assertThat(result.getAgeRatingText()).isEqualTo("12세 이상 관람가");
        assertThat(result.getAgeBadgeText()).isEqualTo("12");
        assertThat(result.getPosterUrl()).isEqualTo("https://image.tmdb.org/t/p/w500/fight-club-poster.jpg");
        assertThat(result.getBackdropUrl()).isEqualTo("https://image.tmdb.org/t/p/w1280/fight-club-backdrop.jpg");
        assertThat(result.isLiveMetadata()).isTrue();
        assertThat(result.getTitle()).isNotEqualTo("로컬 테스트 영화");
        assertThat(result.getOverview()).isNotEqualTo("로컬 시드 설명");
        verify(tmdbClient, never()).searchMovies("로컬 테스트 영화");
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
    void resolveMetadataDoesNotForceFirstTmdbSearchResultWhenTitleDoesNotMatch() {
        Movie movie = Movie.builder()
                .id(20L)
                .title("로컬 테스트 영화")
                .description("로컬 더미 설명")
                .releaseDate(LocalDate.of(2026, 4, 1))
                .status(MovieStatus.NOW_SHOWING)
                .bookingOpen(true)
                .active(true)
                .build();

        TmdbMovieSummaryDto unrelatedResult = new TmdbMovieSummaryDto();
        unrelatedResult.setId(99L);
        unrelatedResult.setTitle("Unrelated TMDB Movie");
        unrelatedResult.setReleaseDate(LocalDate.of(2026, 4, 1));

        TmdbMovieSearchResponseDto response = new TmdbMovieSearchResponseDto();
        response.setResults(List.of(unrelatedResult));

        when(tmdbClient.isConfigured()).thenReturn(true);
        when(tmdbClient.searchMovies("로컬 테스트 영화")).thenReturn(response);

        PublicMovieMetadataDto result = publicMovieMetadataService.resolveMetadata(movie);

        assertThat(result.getLocalMovieId()).isEqualTo(20L);
        assertThat(result.getTmdbId()).isNull();
        assertThat(result.getTitle()).isEqualTo("로컬 테스트 영화");
        assertThat(result.getOverview()).isEqualTo("로컬 더미 설명");
        assertThat(result.isLiveMetadata()).isFalse();
        verify(tmdbClient, never()).getMovieDetailWithMedia(99L);
    }

    @Test
    void getPopularMoviesUsesTmdbSummaryAndLinksMatchingLocalMovie() {
        Movie linkedMovie = Movie.builder()
                .id(55L)
                .tmdbId(101L)
                .title("로컬 연결 영화")
                .genre("스릴러 · 미스터리")
                .ageRating("15")
                .runningTime(118)
                .releaseDate(LocalDate.of(2026, 4, 3))
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
        summary.setGenreIds(List.of(28, 12));

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
        assertThat(result.get(0).getReleaseDate()).isEqualTo(LocalDate.of(2026, 4, 3));
        assertThat(result.get(0).getRuntimeMinutes()).isEqualTo(118);
        assertThat(result.get(0).getGenres()).containsExactly("스릴러", "미스터리");
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
        assertThat(result.getTitle()).isEqualTo("Local Only Movie");
        assertThat(result.getOverview()).isEqualTo("Local description");
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

    @Test
    void summaryMetadataUsesTmdbGenreIdsWhenNoLocalGenreExists() {
        TmdbMovieSummaryDto summary = new TmdbMovieSummaryDto();
        summary.setId(202L);
        summary.setTitle("Mapped Genre Movie");
        summary.setGenreIds(List.of(28, 12, 878));

        TmdbMovieSearchResponseDto response = new TmdbMovieSearchResponseDto();
        response.setResults(List.of(summary));

        when(tmdbClient.isConfigured()).thenReturn(true);
        when(tmdbClient.getPopularMovies()).thenReturn(response);
        when(movieRepository.findAllByActiveTrueAndTmdbIdIn(anyCollection())).thenReturn(List.of());

        List<PublicMovieMetadataDto> result = publicMovieMetadataService.getPopularMovies(1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getGenres()).containsExactly("액션", "모험", "SF");
        assertThat(result.get(0).getGenreText()).isEqualTo("액션 · 모험");
    }

    @Test
    void getMovieListPrioritizesTmdbSectionsBeforeLocalMovies() {
        Movie localMovie = Movie.builder()
                .id(78L)
                .title("로컬 테스트 영화")
                .genre("SF")
                .releaseDate(LocalDate.of(2026, 5, 1))
                .status(MovieStatus.NOW_SHOWING)
                .bookingOpen(true)
                .active(true)
                .build();

        TmdbMovieSummaryDto nowPlaying = new TmdbMovieSummaryDto();
        nowPlaying.setId(401L);
        nowPlaying.setTitle("TMDB Now Playing");

        TmdbMovieSummaryDto popular = new TmdbMovieSummaryDto();
        popular.setId(402L);
        popular.setTitle("TMDB Popular");

        TmdbMovieSummaryDto upcoming = new TmdbMovieSummaryDto();
        upcoming.setId(403L);
        upcoming.setTitle("TMDB Upcoming");

        TmdbMovieSearchResponseDto nowPlayingResponse = new TmdbMovieSearchResponseDto();
        nowPlayingResponse.setResults(List.of(nowPlaying));
        TmdbMovieSearchResponseDto popularResponse = new TmdbMovieSearchResponseDto();
        popularResponse.setResults(List.of(popular));
        TmdbMovieSearchResponseDto upcomingResponse = new TmdbMovieSearchResponseDto();
        upcomingResponse.setResults(List.of(upcoming));

        TmdbMovieDetailDto nowPlayingDetail = new TmdbMovieDetailDto();
        nowPlayingDetail.setId(401L);
        nowPlayingDetail.setTitle("TMDB Now Playing");
        TmdbMovieDetailDto popularDetail = new TmdbMovieDetailDto();
        popularDetail.setId(402L);
        popularDetail.setTitle("TMDB Popular");
        TmdbMovieDetailDto upcomingDetail = new TmdbMovieDetailDto();
        upcomingDetail.setId(403L);
        upcomingDetail.setTitle("TMDB Upcoming");

        when(tmdbClient.isConfigured()).thenReturn(true);
        when(tmdbClient.getNowPlayingMovies()).thenReturn(nowPlayingResponse);
        when(tmdbClient.getPopularMovies()).thenReturn(popularResponse);
        when(tmdbClient.getUpcomingMovies()).thenReturn(upcomingResponse);
        when(tmdbClient.getMovieDetailWithMedia(401L)).thenReturn(nowPlayingDetail);
        when(tmdbClient.getMovieDetailWithMedia(402L)).thenReturn(popularDetail);
        when(tmdbClient.getMovieDetailWithMedia(403L)).thenReturn(upcomingDetail);
        when(movieRepository.findAllByActiveTrueAndTmdbIdIn(anyCollection())).thenReturn(List.of());
        when(movieRepository.findAllByActiveTrueOrderByReleaseDateDescTitleAsc()).thenReturn(List.of(localMovie));

        List<PublicMovieMetadataDto> result = publicMovieMetadataService.getMovieList(3);

        assertThat(result).extracting(PublicMovieMetadataDto::getTitle)
                .containsExactly("TMDB Now Playing", "TMDB Popular", "TMDB Upcoming");
        assertThat(result).extracting(PublicMovieMetadataDto::getTitle)
                .doesNotContain("로컬 테스트 영화");
    }

    @Test
    void getMovieListFallsBackToLocalMoviesWhenTmdbIsNotConfigured() {
        Movie localMovie = Movie.builder()
                .id(77L)
                .title("로컬 상영작")
                .genre("드라마")
                .ageRating("ALL")
                .runningTime(101)
                .releaseDate(LocalDate.of(2026, 5, 1))
                .status(MovieStatus.NOW_SHOWING)
                .bookingOpen(true)
                .active(true)
                .build();

        when(tmdbClient.isConfigured()).thenReturn(false);
        when(movieRepository.findAllByActiveTrueOrderByReleaseDateDescTitleAsc()).thenReturn(List.of(localMovie));

        List<PublicMovieMetadataDto> result = publicMovieMetadataService.getMovieList(24);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLocalMovieId()).isEqualTo(77L);
        assertThat(result.get(0).getGenreText()).isEqualTo("드라마");
        assertThat(result.get(0).getAgeRatingText()).isEqualTo("전체관람가");
        assertThat(result.get(0).getAgeBadgeText()).isEqualTo("ALL");
        assertThat(result.get(0).getRunningTimeText()).isEqualTo("101분");
    }


    @Test
    void getPopularMoviesEnrichesRuntimeFromTmdbDetail() {
        TmdbMovieSummaryDto summary = new TmdbMovieSummaryDto();
        summary.setId(303L);
        summary.setTitle("Runtime Enriched Movie");

        TmdbMovieSearchResponseDto response = new TmdbMovieSearchResponseDto();
        response.setResults(List.of(summary));

        TmdbMovieDetailDto detail = new TmdbMovieDetailDto();
        detail.setId(303L);
        detail.setTitle("Runtime Enriched Movie");
        detail.setRuntime(123);

        when(tmdbClient.isConfigured()).thenReturn(true);
        when(tmdbClient.getPopularMovies()).thenReturn(response);
        when(tmdbClient.getMovieDetailWithMedia(303L)).thenReturn(detail);
        when(movieRepository.findAllByActiveTrueAndTmdbIdIn(anyCollection())).thenReturn(List.of());

        List<PublicMovieMetadataDto> result = publicMovieMetadataService.getPopularMovies(1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRuntimeMinutes()).isEqualTo(123);
        assertThat(result.get(0).getRunningTimeText()).isEqualTo("123분");
    }

    @Test
    void getPopularMoviesNormalizesKoreanCertificationFromTmdbReleaseDates() {
        TmdbMovieSummaryDto summary = new TmdbMovieSummaryDto();
        summary.setId(304L);
        summary.setTitle("Certified Movie");

        TmdbMovieSearchResponseDto response = new TmdbMovieSearchResponseDto();
        response.setResults(List.of(summary));

        TmdbMovieDetailDto detail = new TmdbMovieDetailDto();
        detail.setId(304L);
        detail.setTitle("Certified Movie");
        detail.setReleaseDates(releaseDates("KR", "15"));

        when(tmdbClient.isConfigured()).thenReturn(true);
        when(tmdbClient.getPopularMovies()).thenReturn(response);
        when(tmdbClient.getMovieDetailWithMedia(304L)).thenReturn(detail);
        when(movieRepository.findAllByActiveTrueAndTmdbIdIn(anyCollection())).thenReturn(List.of());

        List<PublicMovieMetadataDto> result = publicMovieMetadataService.getPopularMovies(1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAgeRating()).isEqualTo("15");
        assertThat(result.get(0).getAgeRatingText()).isEqualTo("15세 이상 관람가");
    }

    @Test
    void getPopularMoviesKeepsLocalAgeRatingBeforeTmdbCertification() {
        Movie linkedMovie = Movie.builder()
                .id(305L)
                .tmdbId(3050L)
                .title("Local Rated Movie")
                .ageRating("12")
                .active(true)
                .build();

        TmdbMovieSummaryDto summary = new TmdbMovieSummaryDto();
        summary.setId(3050L);
        summary.setTitle("Local Rated Movie");

        TmdbMovieSearchResponseDto response = new TmdbMovieSearchResponseDto();
        response.setResults(List.of(summary));

        TmdbMovieDetailDto detail = new TmdbMovieDetailDto();
        detail.setId(3050L);
        detail.setTitle("Local Rated Movie");
        detail.setReleaseDates(releaseDates("KR", "청소년관람불가"));

        when(tmdbClient.isConfigured()).thenReturn(true);
        when(tmdbClient.getPopularMovies()).thenReturn(response);
        when(tmdbClient.getMovieDetailWithMedia(3050L)).thenReturn(detail);
        when(movieRepository.findAllByActiveTrueAndTmdbIdIn(anyCollection())).thenReturn(List.of(linkedMovie));

        List<PublicMovieMetadataDto> result = publicMovieMetadataService.getPopularMovies(1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLocalMovieId()).isEqualTo(305L);
        assertThat(result.get(0).getAgeRating()).isEqualTo("12");
    }

    @Test
    void getPopularMoviesKeepsLocalRuntimeBeforeTmdbRuntime() {
        Movie linkedMovie = Movie.builder()
                .id(306L)
                .tmdbId(3060L)
                .title("Local Runtime Movie")
                .runtimeMinutes(95)
                .runningTime(105)
                .active(true)
                .build();

        TmdbMovieSummaryDto summary = new TmdbMovieSummaryDto();
        summary.setId(3060L);
        summary.setTitle("Local Runtime Movie");

        TmdbMovieSearchResponseDto response = new TmdbMovieSearchResponseDto();
        response.setResults(List.of(summary));

        TmdbMovieDetailDto detail = new TmdbMovieDetailDto();
        detail.setId(3060L);
        detail.setTitle("Local Runtime Movie");
        detail.setRuntime(140);

        when(tmdbClient.isConfigured()).thenReturn(true);
        when(tmdbClient.getPopularMovies()).thenReturn(response);
        when(tmdbClient.getMovieDetailWithMedia(3060L)).thenReturn(detail);
        when(movieRepository.findAllByActiveTrueAndTmdbIdIn(anyCollection())).thenReturn(List.of(linkedMovie));

        List<PublicMovieMetadataDto> result = publicMovieMetadataService.getPopularMovies(1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRuntimeMinutes()).isEqualTo(95);
    }

    @Test
    void getPopularMoviesFallsBackToSummaryWhenTmdbDetailFails() {
        TmdbMovieSummaryDto summary = new TmdbMovieSummaryDto();
        summary.setId(307L);
        summary.setTitle("Summary Fallback Movie");
        summary.setGenreIds(List.of(28));

        TmdbMovieSearchResponseDto response = new TmdbMovieSearchResponseDto();
        response.setResults(List.of(summary));

        when(tmdbClient.isConfigured()).thenReturn(true);
        when(tmdbClient.getPopularMovies()).thenReturn(response);
        when(tmdbClient.getMovieDetailWithMedia(307L)).thenThrow(TmdbClientException.network(
                "TMDB request failed because the TMDB server could not be reached. Please try again later.",
                new RuntimeException("timeout")
        ));
        when(movieRepository.findAllByActiveTrueAndTmdbIdIn(anyCollection())).thenReturn(List.of());

        List<PublicMovieMetadataDto> result = publicMovieMetadataService.getPopularMovies(1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Summary Fallback Movie");
        assertThat(result.get(0).getGenreText()).isEqualTo("액션");
        assertThat(result.get(0).getRunningTimeText()).isEqualTo("시간 미정");
    }

    @Test
    void getMovieListReusesCachedDetailForDuplicateTmdbIdsAcrossSections() {
        TmdbMovieSummaryDto nowPlayingSummary = new TmdbMovieSummaryDto();
        nowPlayingSummary.setId(308L);
        nowPlayingSummary.setTitle("Duplicate Section Movie");

        TmdbMovieSearchResponseDto nowPlayingResponse = new TmdbMovieSearchResponseDto();
        nowPlayingResponse.setResults(List.of(nowPlayingSummary));

        TmdbMovieSearchResponseDto popularResponse = new TmdbMovieSearchResponseDto();
        popularResponse.setResults(List.of(nowPlayingSummary));

        TmdbMovieSearchResponseDto upcomingResponse = new TmdbMovieSearchResponseDto();
        upcomingResponse.setResults(List.of(nowPlayingSummary));

        TmdbMovieDetailDto detail = new TmdbMovieDetailDto();
        detail.setId(308L);
        detail.setTitle("Duplicate Section Movie");
        detail.setRuntime(111);

        when(tmdbClient.isConfigured()).thenReturn(true);
        when(movieRepository.findAllByActiveTrueOrderByReleaseDateDescTitleAsc()).thenReturn(List.of());
        when(movieRepository.findAllByActiveTrueAndTmdbIdIn(anyCollection())).thenReturn(List.of());
        when(tmdbClient.getNowPlayingMovies()).thenReturn(nowPlayingResponse);
        when(tmdbClient.getPopularMovies()).thenReturn(popularResponse);
        when(tmdbClient.getUpcomingMovies()).thenReturn(upcomingResponse);
        when(tmdbClient.getMovieDetailWithMedia(308L)).thenReturn(detail);

        List<PublicMovieMetadataDto> result = publicMovieMetadataService.getMovieList(3);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRuntimeMinutes()).isEqualTo(111);
        verify(tmdbClient, times(1)).getMovieDetailWithMedia(308L);
    }

    private TmdbReleaseDatesDto releaseDates(String countryCode, String certification) {
        TmdbReleaseDateItemDto item = new TmdbReleaseDateItemDto();
        item.setCertification(certification);
        item.setType(3);

        TmdbReleaseDateCountryDto country = new TmdbReleaseDateCountryDto();
        country.setIso31661(countryCode);
        country.setReleaseDates(List.of(item));

        TmdbReleaseDatesDto releaseDates = new TmdbReleaseDatesDto();
        releaseDates.setResults(List.of(country));
        return releaseDates;
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
