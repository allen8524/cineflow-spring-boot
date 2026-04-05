package com.cineflow.service;

import com.cineflow.domain.Movie;
import com.cineflow.dto.PublicMovieMetadataDto;
import com.cineflow.dto.TmdbGenreDto;
import com.cineflow.dto.TmdbMovieDetailDto;
import com.cineflow.dto.TmdbMovieSummaryDto;
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
import java.util.regex.Pattern;

@Slf4j
@Service
@Transactional(readOnly = true)
public class PublicMovieMetadataService {

    private static final String DEFAULT_POSTER_URL = "/images/uploads/movie-single.jpg";
    private static final String DEFAULT_BACKDROP_URL = "/images/uploads/slider-bg.jpg";
    private static final String DEFAULT_OVERVIEW = "\uC544\uC9C1 \uACF5\uAC1C\uB41C \uC904\uAC70\uB9AC \uC815\uBCF4\uAC00 \uC5C6\uC5B4 \uC0C1\uC601 \uC815\uBCF4 \uC911\uC2EC\uC73C\uB85C \uC548\uB0B4\uD574\uB4DC\uB9AC\uACE0 \uC788\uC2B5\uB2C8\uB2E4.";
    private static final String DEFAULT_SHORT_DESCRIPTION = "\uC0C1\uC138 \uC18C\uAC1C\uB294 \uC900\uBE44 \uC911\uC774\uC9C0\uB9CC \uC0C1\uC601 \uC815\uBCF4\uC640 \uC608\uB9E4\uB294 \uC815\uC0C1\uC801\uC73C\uB85C \uC774\uC6A9\uD558\uC2E4 \uC218 \uC788\uC2B5\uB2C8\uB2E4.";
    private static final int SHORT_DESCRIPTION_LIMIT = 120;
    private static final Pattern GENRE_SPLITTER = Pattern.compile("\\s*(?:,|/|\u00B7)\\s*");
    // Keep successful TMDB lookups warm across short bursts of repeated page loads.
    private static final Duration LIVE_METADATA_CACHE_TTL = Duration.ofMinutes(5);
    // Keep fallback entries much shorter so transient upstream issues do not linger.
    private static final Duration FALLBACK_METADATA_CACHE_TTL = Duration.ofSeconds(30);

    private final TmdbClient tmdbClient;
    private final Clock clock;
    private final Map<String, MetadataCacheEntry> metadataCache = new ConcurrentHashMap<>();

    public PublicMovieMetadataService(TmdbClient tmdbClient) {
        this(tmdbClient, Clock.systemUTC());
    }

    PublicMovieMetadataService(TmdbClient tmdbClient, Clock clock) {
        this.tmdbClient = tmdbClient;
        this.clock = clock;
    }

    public PublicMovieMetadataDto resolveMetadata(Movie movie) {
        if (movie == null) {
            throw new IllegalArgumentException("Movie is required.");
        }

        PublicMovieMetadataDto localFallback = toLocalFallback(movie);
        if (!tmdbClient.isConfigured()) {
            return localFallback;
        }

        Optional<PublicMovieMetadataDto> cachedMetadata = resolveFromTtlCache(movie);
        if (cachedMetadata.isPresent()) {
            return cachedMetadata.get();
        }

        try {
            PublicMovieMetadataDto resolvedMetadata = resolveLiveMetadata(movie).orElse(localFallback);
            cacheResolvedMetadata(movie, resolvedMetadata);
            return resolvedMetadata;
        } catch (TmdbClientException exception) {
            log.warn("TMDB live metadata lookup failed. Falling back to local data. movieId={}, tmdbId={}, reason={}",
                    movie.getId(), movie.getTmdbId(), exception.getMessage());
            cacheResolvedMetadata(movie, localFallback);
            return localFallback;
        } catch (IllegalArgumentException exception) {
            log.warn("TMDB metadata matching failed. Falling back to local data. movieId={}, tmdbId={}, reason={}",
                    movie.getId(), movie.getTmdbId(), exception.getMessage());
            cacheResolvedMetadata(movie, localFallback);
            return localFallback;
        } catch (RuntimeException exception) {
            log.warn("Unexpected TMDB metadata error. Falling back to local data. movieId={}, tmdbId={}",
                    movie.getId(), movie.getTmdbId(), exception);
            cacheResolvedMetadata(movie, localFallback);
            return localFallback;
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
        return toLocalFallback(movie);
    }

    public List<PublicMovieMetadataDto> resolveLocalMetadata(List<Movie> movies) {
        if (movies == null || movies.isEmpty()) {
            return List.of();
        }

        return movies.stream()
                .filter(Objects::nonNull)
                .map(this::toLocalFallback)
                .toList();
    }

    private Optional<PublicMovieMetadataDto> resolveLiveMetadata(Movie movie) {
        if (movie.getTmdbId() != null && movie.getTmdbId() > 0) {
            return Optional.of(toLiveMetadata(movie, tmdbClient.getMovieDetailWithMedia(movie.getTmdbId())));
        }

        return findTmdbMatch(movie)
                .map(TmdbMovieSummaryDto::getId)
                .filter(Objects::nonNull)
                .map(tmdbClient::getMovieDetailWithMedia)
                .map(detail -> toLiveMetadata(movie, detail));
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

        var searchResponse = tmdbClient.searchMovies(query);
        List<TmdbMovieSummaryDto> results = searchResponse != null
                ? Objects.requireNonNullElse(searchResponse.getResults(), List.of())
                : List.of();
        if (results.isEmpty()) {
            return Optional.empty();
        }

        String normalizedTitle = normalizeTitle(query);
        Integer releaseYear = movie.getReleaseDate() != null ? movie.getReleaseDate().getYear() : null;

        // Conservative matching order:
        // 1) exact title and same year
        // 2) exact title
        // 3) first TMDB search result
        return results.stream()
                .filter(result -> titleMatches(result, normalizedTitle) && releaseYearMatches(result, releaseYear))
                .findFirst()
                .or(() -> results.stream()
                        .filter(result -> titleMatches(result, normalizedTitle))
                        .findFirst())
                .or(() -> Optional.of(results.get(0)));
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

    private PublicMovieMetadataDto toLiveMetadata(Movie movie, TmdbMovieDetailDto detail) {
        return PublicMovieMetadataDto.builder()
                .localMovieId(movie.getId())
                .tmdbId(firstNonNull(detail.getId(), movie.getTmdbId()))
                .title(firstNonBlank(detail.getTitle(), movie.getTitle()))
                .originalTitle(firstNonBlank(detail.getOriginalTitle(), detail.getTitle(), movie.getTitle()))
                .overview(resolveOverview(
                        trimToNull(detail.getOverview()),
                        trimToNull(movie.getOverview()),
                        trimToNull(movie.getDescription()),
                        trimToNull(movie.getShortDescription())
                ))
                .releaseDate(firstNonNull(detail.getReleaseDate(), movie.getReleaseDate()))
                .runtimeMinutes(firstNonNull(detail.getRuntime(), movie.getRuntimeMinutes(), movie.getRunningTime()))
                .genres(resolveGenres(detail.getGenres(), movie.getGenre()))
                .posterUrl(resolvePosterUrl(
                        trimToNull(detail.getPosterPath()),
                        trimToNull(movie.getPosterPath()),
                        trimToNull(movie.getPosterUrl())
                ))
                .backdropUrl(resolveBackdropUrl(
                        trimToNull(detail.getBackdropPath()),
                        trimToNull(movie.getBackdropPath()),
                        trimToNull(detail.getPosterPath()),
                        trimToNull(movie.getPosterPath()),
                        trimToNull(movie.getPosterUrl())
                ))
                .ageRating(trimToNull(movie.getAgeRating()))
                .bookingOpen(movie.isBookingOpen())
                .active(movie.isActive())
                .status(movie.getStatus())
                .bookingRate(movie.getBookingRate())
                .score(movie.getScore())
                .shortDescription(resolveShortDescription(
                        trimToNull(movie.getShortDescription()),
                        trimToNull(detail.getOverview()),
                        trimToNull(movie.getOverview()),
                        trimToNull(movie.getDescription())
                ))
                .liveMetadata(true)
                .build();
    }

    private PublicMovieMetadataDto toLocalFallback(Movie movie) {
        return PublicMovieMetadataDto.builder()
                .localMovieId(movie.getId())
                .tmdbId(movie.getTmdbId())
                .title(trimToNull(movie.getTitle()))
                .originalTitle(trimToNull(movie.getTitle()))
                .overview(resolveOverview(
                        trimToNull(movie.getOverview()),
                        trimToNull(movie.getDescription()),
                        trimToNull(movie.getShortDescription())
                ))
                .releaseDate(movie.getReleaseDate())
                .runtimeMinutes(firstNonNull(movie.getRuntimeMinutes(), movie.getRunningTime()))
                .genres(resolveGenres(List.of(), movie.getGenre()))
                .posterUrl(resolvePosterUrl(
                        trimToNull(movie.getPosterPath()),
                        null,
                        trimToNull(movie.getPosterUrl())
                ))
                .backdropUrl(resolveBackdropUrl(
                        trimToNull(movie.getBackdropPath()),
                        null,
                        trimToNull(movie.getPosterPath()),
                        null,
                        trimToNull(movie.getPosterUrl())
                ))
                .ageRating(trimToNull(movie.getAgeRating()))
                .bookingOpen(movie.isBookingOpen())
                .active(movie.isActive())
                .status(movie.getStatus())
                .bookingRate(movie.getBookingRate())
                .score(movie.getScore())
                .shortDescription(resolveShortDescription(
                        trimToNull(movie.getShortDescription()),
                        trimToNull(movie.getOverview()),
                        trimToNull(movie.getDescription())
                ))
                .liveMetadata(false)
                .build();
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
            PublicMovieMetadataDto localFallback = toLocalFallback(movie);
            perRequestCache.putIfAbsent(lookupKey, localFallback);
            log.warn("Failed to resolve public metadata within the current render pass. Falling back to local data. movieId={}, tmdbId={}",
                    movie.getId(), movie.getTmdbId(), exception);
            return localFallback;
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
            return toLocalFallback(movie);
        }

        return PublicMovieMetadataDto.builder()
                .localMovieId(movie.getId())
                .tmdbId(firstNonNull(cachedMetadata.getTmdbId(), movie.getTmdbId()))
                .title(firstNonBlank(cachedMetadata.getTitle(), movie.getTitle()))
                .originalTitle(firstNonBlank(cachedMetadata.getOriginalTitle(), cachedMetadata.getTitle(), movie.getTitle()))
                .overview(resolveOverview(
                        trimToNull(cachedMetadata.getOverview()),
                        trimToNull(movie.getOverview()),
                        trimToNull(movie.getDescription()),
                        trimToNull(movie.getShortDescription())
                ))
                .releaseDate(firstNonNull(cachedMetadata.getReleaseDate(), movie.getReleaseDate()))
                .runtimeMinutes(firstNonNull(cachedMetadata.getRuntimeMinutes(), movie.getRuntimeMinutes(), movie.getRunningTime()))
                .genres(cachedMetadata.getGenres() == null || cachedMetadata.getGenres().isEmpty()
                        ? resolveGenres(List.of(), movie.getGenre())
                        : cachedMetadata.getGenres())
                .posterUrl(firstNonBlank(
                        trimToNull(cachedMetadata.getPosterUrl()),
                        resolvePosterUrl(trimToNull(movie.getPosterPath()), null, trimToNull(movie.getPosterUrl()))
                ))
                .backdropUrl(firstNonBlank(
                        trimToNull(cachedMetadata.getBackdropUrl()),
                        resolveBackdropUrl(
                                trimToNull(movie.getBackdropPath()),
                                null,
                                trimToNull(movie.getPosterPath()),
                                null,
                                trimToNull(movie.getPosterUrl())
                        )
                ))
                .ageRating(trimToNull(movie.getAgeRating()))
                .bookingOpen(movie.isBookingOpen())
                .active(movie.isActive())
                .status(movie.getStatus())
                .bookingRate(movie.getBookingRate())
                .score(movie.getScore())
                .shortDescription(resolveShortDescription(
                        trimToNull(movie.getShortDescription()),
                        trimToNull(cachedMetadata.getShortDescription()),
                        trimToNull(cachedMetadata.getOverview()),
                        trimToNull(movie.getOverview()),
                        trimToNull(movie.getDescription())
                ))
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

    private List<String> resolveGenres(List<TmdbGenreDto> tmdbGenres, String localGenreText) {
        List<String> liveGenres = tmdbGenres == null ? List.of() : tmdbGenres.stream()
                .map(TmdbGenreDto::getName)
                .map(this::trimToNull)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (!liveGenres.isEmpty()) {
            return liveGenres;
        }

        String normalizedLocalGenre = trimToNull(localGenreText);
        if (normalizedLocalGenre == null) {
            return List.of();
        }

        return GENRE_SPLITTER.splitAsStream(normalizedLocalGenre)
                .map(this::trimToNull)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private String resolvePosterUrl(String primaryPosterPath, String secondaryPosterPath, String localPosterUrl) {
        return firstNonBlank(
                tmdbClient.buildPosterUrl(primaryPosterPath),
                tmdbClient.buildPosterUrl(secondaryPosterPath),
                localPosterUrl,
                DEFAULT_POSTER_URL
        );
    }

    private String resolveBackdropUrl(
            String primaryBackdropPath,
            String secondaryBackdropPath,
            String primaryPosterPath,
            String secondaryPosterPath,
            String localPosterUrl
    ) {
        return firstNonBlank(
                tmdbClient.buildBackdropUrl(primaryBackdropPath),
                tmdbClient.buildBackdropUrl(secondaryBackdropPath),
                tmdbClient.buildPosterUrl(primaryPosterPath),
                tmdbClient.buildPosterUrl(secondaryPosterPath),
                localPosterUrl,
                DEFAULT_BACKDROP_URL
        );
    }

    private String resolveOverview(String... candidates) {
        String overview = firstNonBlank(candidates);
        return overview != null ? overview : DEFAULT_OVERVIEW;
    }

    private String resolveShortDescription(String... candidates) {
        String value = firstNonBlank(candidates);
        if (value == null) {
            return DEFAULT_SHORT_DESCRIPTION;
        }

        return value.length() <= SHORT_DESCRIPTION_LIMIT ? value : value.substring(0, SHORT_DESCRIPTION_LIMIT) + "...";
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
