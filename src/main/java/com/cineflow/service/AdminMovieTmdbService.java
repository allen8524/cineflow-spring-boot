package com.cineflow.service;

import com.cineflow.dto.AdminTmdbMovieDetailDto;
import com.cineflow.dto.AdminTmdbMovieSearchResultDto;
import com.cineflow.dto.TmdbGenreDto;
import com.cineflow.dto.TmdbMovieDetailDto;
import com.cineflow.dto.TmdbMovieSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMovieTmdbService {

    private static final int OVERVIEW_SNIPPET_LIMIT = 120;

    private final TmdbClient tmdbClient;

    public List<AdminTmdbMovieSearchResultDto> searchMovies(String query) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }

        return Objects.requireNonNullElse(tmdbClient.searchMovies(query).getResults(), List.<TmdbMovieSummaryDto>of()).stream()
                .map(this::toSearchResult)
                .toList();
    }

    public AdminTmdbMovieDetailDto getMovieDetail(Long tmdbId) {
        TmdbMovieDetailDto detail = tmdbClient.getMovieDetail(tmdbId);
        List<String> genres = extractGenres(detail.getGenres());

        return AdminTmdbMovieDetailDto.builder()
                .tmdbId(detail.getId())
                .title(trimToNull(detail.getTitle()))
                .originalTitle(trimToNull(detail.getOriginalTitle()))
                .overview(trimToNull(detail.getOverview()))
                .releaseDate(detail.getReleaseDate())
                .runningTime(detail.getRuntime())
                .genreText(genres.isEmpty() ? null : String.join(", ", genres))
                .genres(genres)
                .posterPath(trimToNull(detail.getPosterPath()))
                .backdropPath(trimToNull(detail.getBackdropPath()))
                .posterUrl(tmdbClient.buildPosterUrl(detail.getPosterPath()))
                .backdropUrl(tmdbClient.buildBackdropUrl(detail.getBackdropPath()))
                .build();
    }

    private AdminTmdbMovieSearchResultDto toSearchResult(TmdbMovieSummaryDto movie) {
        return AdminTmdbMovieSearchResultDto.builder()
                .tmdbId(movie.getId())
                .title(trimToNull(movie.getTitle()))
                .releaseDate(movie.getReleaseDate())
                .posterPreviewUrl(tmdbClient.buildPosterUrl(movie.getPosterPath()))
                .overviewSnippet(buildOverviewSnippet(movie.getOverview()))
                .build();
    }

    private List<String> extractGenres(List<TmdbGenreDto> genres) {
        if (genres == null || genres.isEmpty()) {
            return List.of();
        }

        return genres.stream()
                .map(TmdbGenreDto::getName)
                .map(this::trimToNull)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
    }

    private String buildOverviewSnippet(String overview) {
        String normalizedOverview = trimToNull(overview);
        if (normalizedOverview == null) {
            return null;
        }
        if (normalizedOverview.length() <= OVERVIEW_SNIPPET_LIMIT) {
            return normalizedOverview;
        }
        return normalizedOverview.substring(0, OVERVIEW_SNIPPET_LIMIT) + "...";
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
