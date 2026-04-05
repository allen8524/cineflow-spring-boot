package com.cineflow.service;

import com.cineflow.config.TmdbProperties;
import com.cineflow.dto.TmdbMovieDetailDto;
import com.cineflow.dto.TmdbMovieSearchResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
public class TmdbClient {

    private static final String DEFAULT_POSTER_SIZE = "w500";
    private static final String DEFAULT_BACKDROP_SIZE = "w1280";
    private static final String TMDB_SETUP_GUIDE = "TMDB integration is not configured. Check tmdb.base-url and set TMDB_BEARER_TOKEN to enable admin TMDB search.";
    private static final String TMDB_NETWORK_ERROR = "TMDB request failed because the TMDB server could not be reached. Please try again later.";
    private static final String TMDB_SEARCH_FAILURE = "TMDB movie search request failed. Please try again later.";
    private static final String TMDB_DETAIL_FAILURE = "TMDB movie detail request failed. Please try again later.";
    private static final String TMDB_LIST_FAILURE = "TMDB movie list request failed. Please try again later.";
    private static final String DETAIL_APPEND_TO_RESPONSE = "images,videos";
    private static final int DEFAULT_LIST_PAGE = 1;

    private final RestClient tmdbRestClient;
    private final TmdbProperties tmdbProperties;

    public boolean isConfigured() {
        return StringUtils.hasText(tmdbProperties.resolveBaseUrl())
                && StringUtils.hasText(tmdbProperties.resolveBearerToken());
    }

    public TmdbMovieSearchResponseDto searchMovies(String query) {
        if (!StringUtils.hasText(query)) {
            return new TmdbMovieSearchResponseDto();
        }

        ensureApiConfigured();

        try {
            TmdbMovieSearchResponseDto response = tmdbRestClient.get()
                    .uri(uriBuilder -> buildSearchUri(uriBuilder, query.trim()))
                    .headers(this::applyAuthorization)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                        throw TmdbClientException.upstream(
                                TMDB_SEARCH_FAILURE + " status=" + clientResponse.getStatusCode().value()
                        );
                    })
                    .body(TmdbMovieSearchResponseDto.class);

            return response != null ? response : new TmdbMovieSearchResponseDto();
        } catch (TmdbClientException exception) {
            throw exception;
        } catch (RestClientException exception) {
            log.warn("TMDB movie search request failed. query={}", query, exception);
            throw TmdbClientException.network(TMDB_NETWORK_ERROR, exception);
        }
    }

    public TmdbMovieDetailDto getMovieDetail(Long movieId) {
        return getMovieDetail(movieId, false);
    }

    public TmdbMovieSearchResponseDto getNowPlayingMovies() {
        return getMovieCollection("/movie/now_playing");
    }

    public TmdbMovieSearchResponseDto getPopularMovies() {
        return getMovieCollection("/movie/popular");
    }

    public TmdbMovieSearchResponseDto getUpcomingMovies() {
        return getMovieCollection("/movie/upcoming");
    }

    public TmdbMovieDetailDto getMovieDetailWithMedia(Long movieId) {
        return getMovieDetail(movieId, true);
    }

    private TmdbMovieDetailDto getMovieDetail(Long movieId, boolean includeRelatedMedia) {
        if (movieId == null || movieId <= 0) {
            throw new IllegalArgumentException("TMDB movie id is invalid. id=" + movieId);
        }

        ensureApiConfigured();

        try {
            TmdbMovieDetailDto response = tmdbRestClient.get()
                    .uri(uriBuilder -> buildDetailUri(uriBuilder, movieId, includeRelatedMedia))
                    .headers(this::applyAuthorization)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                        throw TmdbClientException.upstream(
                                TMDB_DETAIL_FAILURE + " status=" + clientResponse.getStatusCode().value()
                        );
                    })
                    .body(TmdbMovieDetailDto.class);

            if (response == null) {
                throw TmdbClientException.upstream(TMDB_DETAIL_FAILURE + " Empty response received.");
            }
            return response;
        } catch (TmdbClientException exception) {
            throw exception;
        } catch (RestClientException exception) {
            log.warn("TMDB movie detail request failed. movieId={}", movieId, exception);
            throw TmdbClientException.network(TMDB_NETWORK_ERROR, exception);
        }
    }

    public String buildPosterUrl(String posterPath) {
        return buildImageUrl(posterPath, DEFAULT_POSTER_SIZE);
    }

    public String buildBackdropUrl(String backdropPath) {
        return buildImageUrl(backdropPath, DEFAULT_BACKDROP_SIZE);
    }

    private java.net.URI buildSearchUri(UriBuilder uriBuilder, String query) {
        return uriBuilder
                .path("/search/movie")
                .queryParam("language", tmdbProperties.resolveLanguage())
                .queryParam("query", query)
                .build();
    }

    private TmdbMovieSearchResponseDto getMovieCollection(String path) {
        ensureApiConfigured();

        try {
            TmdbMovieSearchResponseDto response = tmdbRestClient.get()
                    .uri(uriBuilder -> buildListUri(uriBuilder, path))
                    .headers(this::applyAuthorization)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                        throw TmdbClientException.upstream(
                                TMDB_LIST_FAILURE + " status=" + clientResponse.getStatusCode().value()
                        );
                    })
                    .body(TmdbMovieSearchResponseDto.class);

            return response != null ? response : new TmdbMovieSearchResponseDto();
        } catch (TmdbClientException exception) {
            throw exception;
        } catch (RestClientException exception) {
            log.warn("TMDB movie list request failed. path={}", path, exception);
            throw TmdbClientException.network(TMDB_NETWORK_ERROR, exception);
        }
    }

    private java.net.URI buildListUri(UriBuilder uriBuilder, String path) {
        return uriBuilder
                .path(path)
                .queryParam("language", tmdbProperties.resolveLanguage())
                .queryParam("page", DEFAULT_LIST_PAGE)
                .build();
    }

    private java.net.URI buildDetailUri(UriBuilder uriBuilder, Long movieId, boolean includeRelatedMedia) {
        UriBuilder configuredUriBuilder = uriBuilder
                .path("/movie/{movieId}")
                .queryParam("language", tmdbProperties.resolveLanguage());

        if (includeRelatedMedia) {
            configuredUriBuilder = configuredUriBuilder.queryParam("append_to_response", DETAIL_APPEND_TO_RESPONSE);
        }

        return configuredUriBuilder.build(movieId);
    }

    private void applyAuthorization(HttpHeaders headers) {
        headers.setBearerAuth(resolveBearerToken());
    }

    private void ensureApiConfigured() {
        if (!StringUtils.hasText(tmdbProperties.resolveBaseUrl())) {
            throw TmdbClientException.configuration(TMDB_SETUP_GUIDE);
        }
        resolveBearerToken();
    }

    private String resolveBearerToken() {
        String bearerToken = tmdbProperties.resolveBearerToken();
        if (!StringUtils.hasText(bearerToken)) {
            throw TmdbClientException.configuration(TMDB_SETUP_GUIDE);
        }
        return bearerToken;
    }

    private String buildImageUrl(String imagePath, String size) {
        if (!StringUtils.hasText(imagePath)) {
            return null;
        }
        if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
            return imagePath;
        }

        String imageBaseUrl = tmdbProperties.resolveImageBaseUrl();
        if (!StringUtils.hasText(imageBaseUrl)) {
            return null;
        }

        String normalizedBaseUrl = StringUtils.trimTrailingCharacter(imageBaseUrl.trim(), '/');
        String normalizedPath = imagePath.startsWith("/") ? imagePath : "/" + imagePath;
        return normalizedBaseUrl + "/" + size + normalizedPath;
    }
}
