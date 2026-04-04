package com.cineflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
public class AdminTmdbMovieSearchResultDto {

    private final Long tmdbId;
    private final String title;
    private final LocalDate releaseDate;
    private final String posterPreviewUrl;
    private final String overviewSnippet;
}
