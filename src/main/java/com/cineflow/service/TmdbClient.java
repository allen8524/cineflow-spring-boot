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

    private final RestClient tmdbRestClient;
    private final TmdbProperties tmdbProperties;

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
                        throw new TmdbClientException(
                                "TMDB movie search request failed. status=" + clientResponse.getStatusCode().value()
                        );
                    })
                    .body(TmdbMovieSearchResponseDto.class);

            return response != null ? response : new TmdbMovieSearchResponseDto();
        } catch (TmdbClientException exception) {
            throw exception;
        } catch (RestClientException exception) {
            log.warn("TMDB movie search request failed. query={}", query, exception);
            throw new TmdbClientException("TMDB movie search request errored. query=" + query, exception);
        }
    }

    public TmdbMovieDetailDto getMovieDetail(Long movieId) {
        if (movieId == null || movieId <= 0) {
            throw new IllegalArgumentException("TMDB movie id is invalid. id=" + movieId);
        }

        ensureApiConfigured();

        try {
            TmdbMovieDetailDto response = tmdbRestClient.get()
                    .uri(uriBuilder -> buildDetailUri(uriBuilder, movieId))
                    .headers(this::applyAuthorization)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                        throw new TmdbClientException(
                                "TMDB movie detail request failed. id=" + movieId
                                        + ", status=" + clientResponse.getStatusCode().value()
                        );
                    })
                    .body(TmdbMovieDetailDto.class);

            if (response == null) {
                throw new TmdbClientException("TMDB movie detail response was empty. id=" + movieId);
            }
            return response;
        } catch (TmdbClientException exception) {
            throw exception;
        } catch (RestClientException exception) {
            log.warn("TMDB movie detail request failed. movieId={}", movieId, exception);
            throw new TmdbClientException("TMDB movie detail request errored. id=" + movieId, exception);
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

    private java.net.URI buildDetailUri(UriBuilder uriBuilder, Long movieId) {
        return uriBuilder
                .path("/movie/{movieId}")
                .queryParam("language", tmdbProperties.resolveLanguage())
                .build(movieId);
    }

    private void applyAuthorization(HttpHeaders headers) {
        headers.setBearerAuth(resolveBearerToken());
    }

    private void ensureApiConfigured() {
        if (!StringUtils.hasText(tmdbProperties.resolveBaseUrl())) {
            throw new TmdbClientException("TMDB base URL is not configured.");
        }
        resolveBearerToken();
    }

    private String resolveBearerToken() {
        String bearerToken = tmdbProperties.resolveBearerToken();
        if (!StringUtils.hasText(bearerToken)) {
            throw new TmdbClientException("TMDB bearer token is not configured. Check the TMDB_BEARER_TOKEN environment variable.");
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
