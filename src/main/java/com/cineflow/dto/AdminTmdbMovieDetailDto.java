package com.cineflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class AdminTmdbMovieDetailDto {

    private final Long tmdbId;
    private final String title;
    private final String originalTitle;
    private final String overview;
    private final LocalDate releaseDate;
    private final Integer runningTime;
    private final String genreText;
    private final List<String> genres;
    private final String posterPath;
    private final String backdropPath;
    private final String posterUrl;
    private final String backdropUrl;
}
