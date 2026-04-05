package com.cineflow.service;

import com.cineflow.domain.Movie;
import com.cineflow.domain.MovieStatus;
import com.cineflow.dto.PublicMovieMetadataDto;
import com.cineflow.dto.TmdbGenreDto;
import com.cineflow.dto.TmdbMovieDetailDto;
import com.cineflow.dto.TmdbMovieSearchResponseDto;
import com.cineflow.dto.TmdbMovieSummaryDto;
import com.cineflow.repository.MovieRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
public class PublicMovieMetadataService {

    private static final String DEFAULT_TITLE = "영화 정보 준비 중";
    private static final String DEFAULT_POSTER_URL = "/images/uploads/movie-single.jpg";
    private static final String DEFAULT_BACKDROP_URL = "/images/uploads/slider-bg.jpg";
    private static final String DEFAULT_OVERVIEW = "현재 영화 소개를 불러오는 중입니다. 잠시 후 다시 확인해 주세요.";
    private static final String DEFAULT_SHORT_DESCRIPTION = "영화 소개를 준비 중이지만 상영 정보와 예매 연결은 계속 확인하실 수 있습니다.";
    private static final int SHORT_DESCRIPTION_LIMIT = 120;
    private static final Duration LIVE_METADATA_CACHE_TTL = Duration.ofMinutes(5);
    private static final Duration FALLBACK_METADATA_CACHE_TTL = Duration.ofSeconds(30);

    private final TmdbClient tmdbClient;
    private final MovieRepository movieRepository;
    private Clock clock;
    private final Map<String, MetadataCacheEntry> metadataCache = new ConcurrentHashMap<>();

    public PublicMovieMetadataService(TmdbClient tmdbClient, MovieRepository movieRepository) {
        this.tmdbClient = tmdbClient;
        this.movieRepository = movieRepository;
        this.clock = Clock.systemDefaultZone();
    }

    // Package-private hook for deterministic TTL tests. Production keeps the system default clock.
    PublicMovieMetadataService useClock(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "Clock is required.");
        return this;
    }

    public List<PublicMovieMetadataDto> getHeroMovies(int limit) {
        return getPopularMovies(limit);
    }

    public List<PublicMovieMetadataDto> getPopularMovies(int limit) {
        return getSectionMovies(tmdbClient::getPopularMovies, limit, null);
    }

    public List<PublicMovieMetadataDto> getNowShowingMovies(int limit) {
        return getSectionMovies(tmdbClient::getNowPlayingMovies, limit, MovieStatus.NOW_SHOWING);
    }

    public List<PublicMovieMetadataDto> getComingSoonMovies(int limit) {
        return getSectionMovies(tmdbClient::getUpcomingMovies, limit, MovieStatus.COMING_SOON);
    }

    public List<PublicMovieMetadataDto> getMovieList(int limit) {
        if (limit <= 0) {
            return List.of();
        }

        LinkedHashMap<Long, PublicMovieMetadataDto> mergedMovies = new LinkedHashMap<>();
        appendDistinctByTmdbId(mergedMovies, getNowShowingMovies(limit));
        appendDistinctByTmdbId(mergedMovies, getPopularMovies(limit));
        appendDistinctByTmdbId(mergedMovies, getComingSoonMovies(limit));

        return mergedMovies.values().stream()
                .limit(limit)
                .toList();
    }

    public PublicMovieMetadataDto getMovieDetail(Long routeId) {
        if (routeId == null || routeId <= 0) {
            throw new IllegalArgumentException("Movie id is required.");
        }

        Optional<Movie> linkedLocalMovie = movieRepository.findByIdAndActiveTrue(routeId);
        if (linkedLocalMovie.isPresent()) {
            return resolveMetadata(linkedLocalMovie.get());
        }

        Movie linkedByTmdbId = movieRepository.findByTmdbIdAndActiveTrue(routeId).orElse(null);
        return resolveMetadataByTmdbId(routeId, linkedByTmdbId, null);
    }

    public PublicMovieMetadataDto getDefaultMovieDetail() {
        return getHeroMovies(1).stream()
                .findFirst()
                .map(this::refreshDetailMetadata)
                .orElseGet(this::buildStandaloneFallback);
    }

    public List<PublicMovieMetadataDto> getRelatedMovies(Long currentTmdbId, int limit) {
        if (limit <= 0) {
            return List.of();
        }

        return getPopularMovies(limit + 4).stream()
                .filter(movie -> !Objects.equals(movie.getTmdbId(), currentTmdbId))
                .limit(limit)
                .toList();
    }

    public PublicMovieMetadataDto resolveMetadata(Movie movie) {
        if (movie == null) {
            throw new IllegalArgumentException("Movie is required.");
        }

        PublicMovieMetadataDto fallback = buildFallbackMetadata(movie, movie.getTmdbId(), movie.getStatus());
        if (!tmdbClient.isConfigured()) {
            return fallback;
        }

        Optional<PublicMovieMetadataDto> cachedMetadata = resolveFromTtlCache(movie);
        if (cachedMetadata.isPresent()) {
            return cachedMetadata.get();
        }

        try {
            PublicMovieMetadataDto resolvedMetadata = resolveLiveMetadata(movie).orElse(fallback);
            cacheResolvedMetadata(movie, resolvedMetadata);
            return resolvedMetadata;
        } catch (TmdbClientException exception) {
            log.warn("TMDB live metadata lookup failed. Falling back to a neutral placeholder. movieId={}, tmdbId={}, reason={}",
                    movie.getId(), movie.getTmdbId(), exception.getMessage());
            cacheResolvedMetadata(movie, fallback);
            return fallback;
        } catch (IllegalArgumentException exception) {
            log.warn("TMDB metadata matching failed. Falling back to a neutral placeholder. movieId={}, tmdbId={}, reason={}",
                    movie.getId(), movie.getTmdbId(), exception.getMessage());
            cacheResolvedMetadata(movie, fallback);
            return fallback;
        } catch (RuntimeException exception) {
            log.warn("Unexpected TMDB metadata error. Falling back to a neutral placeholder. movieId={}, tmdbId={}",
                    movie.getId(), movie.getTmdbId(), exception);
            cacheResolvedMetadata(movie, fallback);
            return fallback;
        }
    }

    public List<PublicMovieMetadataDto> resolveMetadata(List<Movie> movies) {
        if (movies == null || movies.isEmpty()) {
            return List.of();
        }

        Map<String, PublicMovieMetadataDto> perRequestCache = new LinkedHashMap<>();
        return movies.stream()
                .filter(Objects::nonNull)
                .map(movie -> resolveMetadata(movie, perRequestCache))
                .toList();
    }

    public PublicMovieMetadataDto resolveLocalMetadata(Movie movie) {
        if (movie == null) {
            throw new IllegalArgumentException("Movie is required.");
        }
        return buildFallbackMetadata(movie, movie.getTmdbId(), movie.getStatus());
    }

    public List<PublicMovieMetadataDto> resolveLocalMetadata(List<Movie> movies) {
        if (movies == null || movies.isEmpty()) {
            return List.of();
        }

        return movies.stream()
                .filter(Objects::nonNull)
                .map(this::resolveLocalMetadata)
                .toList();
    }

    private List<PublicMovieMetadataDto> getSectionMovies(
            Supplier<TmdbMovieSearchResponseDto> fetcher,
            int limit,
            MovieStatus statusHint
    ) {
        if (limit <= 0 || !tmdbClient.isConfigured()) {
            return List.of();
        }

        List<TmdbMovieSummaryDto> results;
        try {
            TmdbMovieSearchResponseDto response = fetcher.get();
            results = response != null ? Objects.requireNonNullElse(response.getResults(), List.of()) : List.of();
        } catch (TmdbClientException exception) {
            log.warn("TMDB public movie list lookup failed. Returning an empty public list. reason={}", exception.getMessage());
            return List.of();
        } catch (RuntimeException exception) {
            log.warn("Unexpected TMDB public movie list error. Returning an empty public list.", exception);
            return List.of();
        }

        Map<Long, Movie> linkedMoviesByTmdbId = findLinkedMoviesByTmdbId(results);

        LinkedHashMap<Long, PublicMovieMetadataDto> publicMovies = new LinkedHashMap<>();
        for (TmdbMovieSummaryDto summary : results) {
            if (summary == null || summary.getId() == null) {
                continue;
            }

            publicMovies.putIfAbsent(
                    summary.getId(),
                    toSummaryMetadata(summary, linkedMoviesByTmdbId.get(summary.getId()), statusHint)
            );

            if (publicMovies.size() >= limit) {
                break;
            }
        }

        return List.copyOf(publicMovies.values());
    }

    private PublicMovieMetadataDto refreshDetailMetadata(PublicMovieMetadataDto summaryMetadata) {
        if (summaryMetadata == null || summaryMetadata.getTmdbId() == null) {
            return buildStandaloneFallback();
        }

        Movie linkedMovie = summaryMetadata.getLocalMovieId() != null
                ? movieRepository.findByIdAndActiveTrue(summaryMetadata.getLocalMovieId()).orElse(null)
                : movieRepository.findByTmdbIdAndActiveTrue(summaryMetadata.getTmdbId()).orElse(null);

        return resolveMetadataByTmdbId(summaryMetadata.getTmdbId(), linkedMovie, summaryMetadata.getStatus());
    }

    private PublicMovieMetadataDto resolveMetadataByTmdbId(Long tmdbId, Movie linkedMovie, MovieStatus statusHint) {
        Movie cacheReference = linkedMovie != null
                ? linkedMovie
                : Movie.builder().tmdbId(tmdbId).status(statusHint).build();

        PublicMovieMetadataDto fallback = buildFallbackMetadata(linkedMovie, tmdbId, statusHint);
        if (!tmdbClient.isConfigured()) {
            return fallback;
        }

        Optional<PublicMovieMetadataDto> cachedMetadata = resolveFromTtlCache(cacheReference);
        if (cachedMetadata.isPresent()) {
            return cachedMetadata.get();
        }

        try {
            PublicMovieMetadataDto resolvedMetadata = toLiveMetadata(
                    linkedMovie,
                    tmdbClient.getMovieDetailWithMedia(tmdbId),
                    statusHint
            );
            cacheResolvedMetadata(cacheReference, resolvedMetadata);
            return resolvedMetadata;
        } catch (TmdbClientException exception) {
            log.warn("TMDB movie detail lookup failed. Falling back to a neutral placeholder. tmdbId={}, reason={}",
                    tmdbId, exception.getMessage());
            cacheResolvedMetadata(cacheReference, fallback);
            return fallback;
        } catch (RuntimeException exception) {
            log.warn("Unexpected TMDB movie detail error. Falling back to a neutral placeholder. tmdbId={}", tmdbId, exception);
            cacheResolvedMetadata(cacheReference, fallback);
            return fallback;
        }
    }

    private Optional<PublicMovieMetadataDto> resolveLiveMetadata(Movie movie) {
        if (movie.getTmdbId() != null && movie.getTmdbId() > 0) {
            return Optional.of(resolveMetadataByTmdbId(movie.getTmdbId(), movie, null));
        }

        return findTmdbMatch(movie)
                .map(TmdbMovieSummaryDto::getId)
                .filter(Objects::nonNull)
                .map(tmdbId -> resolveMetadataByTmdbId(tmdbId, movie, null));
    }

    private Optional<PublicMovieMetadataDto> resolveFromTtlCache(Movie movie) {
        Instant now = clock.instant();

        for (String cacheKey : buildCandidateCacheKeys(movie)) {
            MetadataCacheEntry cacheEntry = metadataCache.get(cacheKey);
            if (cacheEntry == null) {
                continue;
            }

            if (cacheEntry.isExpiredAt(now)) {
                metadataCache.remove(cacheKey, cacheEntry);
                continue;
            }

            return Optional.of(adaptCachedMetadata(movie, cacheEntry.metadata()));
        }

        return Optional.empty();
    }

    private Optional<TmdbMovieSummaryDto> findTmdbMatch(Movie movie) {
        String query = trimToNull(movie.getTitle());
        if (query == null) {
            return Optional.empty();
        }

        TmdbMovieSearchResponseDto searchResponse = tmdbClient.searchMovies(query);
        List<TmdbMovieSummaryDto> results = searchResponse != null
                ? Objects.requireNonNullElse(searchResponse.getResults(), List.of())
                : List.of();
        if (results.isEmpty()) {
            return Optional.empty();
        }

        String normalizedTitle = normalizeTitle(query);
        Integer releaseYear = movie.getReleaseDate() != null ? movie.getReleaseDate().getYear() : null;

        return results.stream()
                .filter(result -> titleMatches(result, normalizedTitle) && releaseYearMatches(result, releaseYear))
                .findFirst()
                .or(() -> results.stream()
                        .filter(result -> titleMatches(result, normalizedTitle))
                        .findFirst())
                .or(() -> Optional.of(results.get(0)));
    }

    private void appendDistinctByTmdbId(
            Map<Long, PublicMovieMetadataDto> target,
            List<PublicMovieMetadataDto> source
    ) {
        for (PublicMovieMetadataDto movie : source) {
            if (movie == null || movie.getTmdbId() == null) {
                continue;
            }
            target.putIfAbsent(movie.getTmdbId(), movie);
        }
    }

    private Map<Long, Movie> findLinkedMoviesByTmdbId(List<TmdbMovieSummaryDto> summaries) {
        List<Long> tmdbIds = summaries.stream()
                .filter(Objects::nonNull)
                .map(TmdbMovieSummaryDto::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (tmdbIds.isEmpty()) {
            return Map.of();
        }

        return movieRepository.findAllByActiveTrueAndTmdbIdIn(tmdbIds).stream()
                .filter(movie -> movie.getTmdbId() != null)
                .collect(Collectors.toMap(
                        Movie::getTmdbId,
                        Function.identity(),
                        (first, second) -> first,
                        LinkedHashMap::new
                ));
    }

    private void cacheResolvedMetadata(Movie movie, PublicMovieMetadataDto resolvedMetadata) {
        Instant now = clock.instant();
        Duration ttl = resolvedMetadata.isLiveMetadata() ? LIVE_METADATA_CACHE_TTL : FALLBACK_METADATA_CACHE_TTL;
        MetadataCacheEntry cacheEntry = new MetadataCacheEntry(resolvedMetadata, now.plus(ttl));

        for (String cacheKey : buildCacheKeys(movie, resolvedMetadata)) {
            metadataCache.put(cacheKey, cacheEntry);
        }

        evictExpiredEntries(now);
    }

    private void evictExpiredEntries(Instant now) {
        metadataCache.entrySet().removeIf(entry -> entry.getValue().isExpiredAt(now));
    }

    private PublicMovieMetadataDto toSummaryMetadata(
            TmdbMovieSummaryDto summary,
            Movie linkedMovie,
            MovieStatus statusHint
    ) {
        String overview = resolveOverview(summary.getOverview());

        return PublicMovieMetadataDto.builder()
                .localMovieId(linkedMovie != null ? linkedMovie.getId() : null)
                .tmdbId(summary.getId())
                .title(resolveTitle(summary.getTitle()))
                .originalTitle(resolveOriginalTitle(summary.getOriginalTitle(), summary.getTitle()))
                .overview(overview)
                .releaseDate(summary.getReleaseDate())
                .runtimeMinutes(null)
                .genres(List.of())
                .posterUrl(resolvePosterUrl(summary.getPosterPath()))
                .backdropUrl(resolveBackdropUrl(summary.getBackdropPath(), summary.getPosterPath()))
                .ageRating(resolveAgeRating(linkedMovie))
                .bookingOpen(resolveBookingOpen(linkedMovie))
                .active(resolveActive(linkedMovie))
                .status(resolveStatus(linkedMovie, statusHint, summary.getReleaseDate()))
                .bookingRate(null)
                .score(null)
                .shortDescription(resolveShortDescription(overview))
                .liveMetadata(true)
                .build();
    }

    private PublicMovieMetadataDto toLiveMetadata(
            Movie linkedMovie,
            TmdbMovieDetailDto detail,
            MovieStatus statusHint
    ) {
        String overview = resolveOverview(detail.getOverview());

        return PublicMovieMetadataDto.builder()
                .localMovieId(linkedMovie != null ? linkedMovie.getId() : null)
                .tmdbId(detail.getId())
                .title(resolveTitle(detail.getTitle()))
                .originalTitle(resolveOriginalTitle(detail.getOriginalTitle(), detail.getTitle()))
                .overview(overview)
                .releaseDate(detail.getReleaseDate())
                .runtimeMinutes(detail.getRuntime())
                .genres(resolveGenres(detail.getGenres()))
                .posterUrl(resolvePosterUrl(detail.getPosterPath()))
                .backdropUrl(resolveBackdropUrl(detail.getBackdropPath(), detail.getPosterPath()))
                .ageRating(resolveAgeRating(linkedMovie))
                .bookingOpen(resolveBookingOpen(linkedMovie))
                .active(resolveActive(linkedMovie))
                .status(resolveStatus(linkedMovie, statusHint, detail.getReleaseDate()))
                .bookingRate(null)
                .score(null)
                .shortDescription(resolveShortDescription(overview))
                .liveMetadata(true)
                .build();
    }

    private PublicMovieMetadataDto buildFallbackMetadata(Movie linkedMovie, Long tmdbId, MovieStatus statusHint) {
        return PublicMovieMetadataDto.builder()
                .localMovieId(linkedMovie != null ? linkedMovie.getId() : null)
                .tmdbId(tmdbId)
                .title(DEFAULT_TITLE)
                .originalTitle(null)
                .overview(DEFAULT_OVERVIEW)
                .releaseDate(null)
                .runtimeMinutes(null)
                .genres(List.of())
                .posterUrl(DEFAULT_POSTER_URL)
                .backdropUrl(DEFAULT_BACKDROP_URL)
                .ageRating(resolveAgeRating(linkedMovie))
                .bookingOpen(resolveBookingOpen(linkedMovie))
                .active(resolveActive(linkedMovie))
                .status(resolveStatus(linkedMovie, statusHint, null))
                .bookingRate(null)
                .score(null)
                .shortDescription(DEFAULT_SHORT_DESCRIPTION)
                .liveMetadata(false)
                .build();
    }

    private PublicMovieMetadataDto buildStandaloneFallback() {
        return buildFallbackMetadata(null, null, null);
    }

    private PublicMovieMetadataDto resolveMetadata(Movie movie, Map<String, PublicMovieMetadataDto> perRequestCache) {
        String lookupKey = buildLookupKey(movie);
        try {
            PublicMovieMetadataDto cachedMetadata = perRequestCache.get(lookupKey);
            if (cachedMetadata != null) {
                return adaptCachedMetadata(movie, cachedMetadata);
            }

            PublicMovieMetadataDto resolvedMetadata = resolveMetadata(movie);
            perRequestCache.put(lookupKey, resolvedMetadata);
            return resolvedMetadata;
        } catch (RuntimeException exception) {
            PublicMovieMetadataDto fallback = buildFallbackMetadata(movie, movie.getTmdbId(), movie.getStatus());
            perRequestCache.putIfAbsent(lookupKey, fallback);
            log.warn("Failed to resolve public metadata within the current render pass. Falling back to a neutral placeholder. movieId={}, tmdbId={}",
                    movie.getId(), movie.getTmdbId(), exception);
            return fallback;
        }
    }

    private String buildLookupKey(Movie movie) {
        if (movie.getTmdbId() != null && movie.getTmdbId() > 0) {
            return "tmdb:" + movie.getTmdbId();
        }
        if (movie.getId() != null) {
            return "local:" + movie.getId();
        }

        String normalizedTitle = normalizeTitle(movie.getTitle());
        Integer releaseYear = movie.getReleaseDate() != null ? movie.getReleaseDate().getYear() : null;
        return "search:" + Objects.toString(normalizedTitle, "") + ":" + Objects.toString(releaseYear, "");
    }

    private List<String> buildCandidateCacheKeys(Movie movie) {
        Set<String> keys = new LinkedHashSet<>();
        addTmdbKey(keys, movie.getTmdbId());
        addLocalKey(keys, movie.getId());
        addSearchKey(keys, movie.getTitle(), movie.getReleaseDate());
        return List.copyOf(keys);
    }

    private List<String> buildCacheKeys(Movie movie, PublicMovieMetadataDto resolvedMetadata) {
        Set<String> keys = new LinkedHashSet<>();
        addTmdbKey(keys, firstNonNull(resolvedMetadata.getTmdbId(), movie.getTmdbId()));
        addLocalKey(keys, resolvedMetadata.getLocalMovieId());
        addLocalKey(keys, movie.getId());
        addSearchKey(keys, movie.getTitle(), movie.getReleaseDate());
        return List.copyOf(keys);
    }

    private void addTmdbKey(Set<String> keys, Long tmdbId) {
        if (tmdbId != null && tmdbId > 0) {
            keys.add("tmdb:" + tmdbId);
        }
    }

    private void addLocalKey(Set<String> keys, Long localMovieId) {
        if (localMovieId != null && localMovieId > 0) {
            keys.add("local:" + localMovieId);
        }
    }

    private void addSearchKey(Set<String> keys, String title, LocalDate releaseDate) {
        String normalizedTitle = normalizeTitle(title);
        if (normalizedTitle == null) {
            return;
        }

        Integer releaseYear = releaseDate != null ? releaseDate.getYear() : null;
        keys.add("search:" + normalizedTitle + ":" + Objects.toString(releaseYear, ""));
    }

    private PublicMovieMetadataDto adaptCachedMetadata(Movie movie, PublicMovieMetadataDto cachedMetadata) {
        if (!cachedMetadata.isLiveMetadata()) {
            return buildFallbackMetadata(movie, firstNonNull(cachedMetadata.getTmdbId(), movie.getTmdbId()), cachedMetadata.getStatus());
        }

        return PublicMovieMetadataDto.builder()
                .localMovieId(movie.getId())
                .tmdbId(cachedMetadata.getTmdbId())
                .title(resolveTitle(cachedMetadata.getTitle()))
                .originalTitle(resolveOriginalTitle(cachedMetadata.getOriginalTitle(), cachedMetadata.getTitle()))
                .overview(resolveOverview(cachedMetadata.getOverview()))
                .releaseDate(cachedMetadata.getReleaseDate())
                .runtimeMinutes(cachedMetadata.getRuntimeMinutes())
                .genres(cachedMetadata.getGenres() == null ? List.of() : cachedMetadata.getGenres())
                .posterUrl(firstNonBlank(trimToNull(cachedMetadata.getPosterUrl()), DEFAULT_POSTER_URL))
                .backdropUrl(firstNonBlank(trimToNull(cachedMetadata.getBackdropUrl()), DEFAULT_BACKDROP_URL))
                .ageRating(resolveAgeRating(movie))
                .bookingOpen(resolveBookingOpen(movie))
                .active(resolveActive(movie))
                .status(resolveStatus(movie, cachedMetadata.getStatus(), cachedMetadata.getReleaseDate()))
                .bookingRate(null)
                .score(null)
                .shortDescription(resolveShortDescription(cachedMetadata.getOverview(), cachedMetadata.getShortDescription()))
                .liveMetadata(true)
                .build();
    }

    private boolean titleMatches(TmdbMovieSummaryDto result, String normalizedLocalTitle) {
        if (normalizedLocalTitle == null) {
            return false;
        }

        return normalizedLocalTitle.equals(normalizeTitle(result.getTitle()))
                || normalizedLocalTitle.equals(normalizeTitle(result.getOriginalTitle()));
    }

    private boolean releaseYearMatches(TmdbMovieSummaryDto result, Integer releaseYear) {
        return releaseYear != null
                && result.getReleaseDate() != null
                && result.getReleaseDate().getYear() == releaseYear;
    }

    private List<String> resolveGenres(List<TmdbGenreDto> tmdbGenres) {
        if (tmdbGenres == null) {
            return List.of();
        }

        return tmdbGenres.stream()
                .map(TmdbGenreDto::getName)
                .map(this::trimToNull)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private String resolvePosterUrl(String posterPath) {
        return firstNonBlank(
                tmdbClient.buildPosterUrl(posterPath),
                DEFAULT_POSTER_URL
        );
    }

    private String resolveBackdropUrl(String backdropPath, String posterPath) {
        return firstNonBlank(
                tmdbClient.buildBackdropUrl(backdropPath),
                tmdbClient.buildPosterUrl(posterPath),
                DEFAULT_BACKDROP_URL
        );
    }

    private String resolveTitle(String title) {
        return firstNonBlank(title, DEFAULT_TITLE);
    }

    private String resolveOriginalTitle(String originalTitle, String title) {
        return firstNonBlank(originalTitle, title);
    }

    private String resolveOverview(String overview) {
        return firstNonBlank(overview, DEFAULT_OVERVIEW);
    }

    private String resolveShortDescription(String... candidates) {
        String value = firstNonBlank(candidates);
        if (value == null) {
            return DEFAULT_SHORT_DESCRIPTION;
        }

        return value.length() <= SHORT_DESCRIPTION_LIMIT ? value : value.substring(0, SHORT_DESCRIPTION_LIMIT) + "...";
    }

    private String resolveAgeRating(Movie linkedMovie) {
        return linkedMovie != null ? trimToNull(linkedMovie.getAgeRating()) : null;
    }

    private boolean resolveBookingOpen(Movie linkedMovie) {
        return linkedMovie != null && linkedMovie.isBookingOpen();
    }

    private boolean resolveActive(Movie linkedMovie) {
        return linkedMovie != null && linkedMovie.isActive();
    }

    private MovieStatus resolveStatus(Movie linkedMovie, MovieStatus statusHint, LocalDate releaseDate) {
        if (statusHint != null) {
            return statusHint;
        }
        if (linkedMovie != null && linkedMovie.getStatus() != null) {
            return linkedMovie.getStatus();
        }
        if (releaseDate == null) {
            return MovieStatus.NOW_SHOWING;
        }
        return releaseDate.isAfter(LocalDate.now(clock)) ? MovieStatus.COMING_SOON : MovieStatus.NOW_SHOWING;
    }

    private String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            String normalized = trimToNull(candidate);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    @SafeVarargs
    private <T> T firstNonNull(T... candidates) {
        for (T candidate : candidates) {
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    private String normalizeTitle(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }

        return normalized.replaceAll("[^\\p{L}\\p{N}]", "").toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private record MetadataCacheEntry(PublicMovieMetadataDto metadata, Instant expiresAt) {
        private boolean isExpiredAt(Instant currentTime) {
            return !expiresAt.isAfter(currentTime);
        }
    }
}
