package com.cineflow.dto;

import com.cineflow.domain.Movie;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class ApiMovieResponseDto {

    private final Long id;
    private final String title;
    private final String originalTitle;
    private final String genre;
    private final LocalDate releaseDate;
    private final Integer runningTime;
    private final String ageRating;
    private final String status;
    private final String posterUrl;
    private final String backdropUrl;
    private final String overview;
    private final Double rating;

    public static ApiMovieResponseDto from(Movie movie) {
        if (movie == null) {
            return null;
        }

        return ApiMovieResponseDto.builder()
                .id(movie.getId())
                .title(movie.getTitle())
                .originalTitle(movie.getTitle())
                .genre(movie.getGenre())
                .releaseDate(movie.getReleaseDate())
                .runningTime(movie.getRunningTime() != null ? movie.getRunningTime() : movie.getRuntimeMinutes())
                .ageRating(movie.getAgeRating())
                .status(movie.getStatus() != null ? movie.getStatus().name() : null)
                .posterUrl(movie.getPosterUrl())
                .backdropUrl(movie.getBackdropPath())
                .overview(movie.getOverview() != null ? movie.getOverview() : movie.getDescription())
                .rating(movie.getScore())
                .build();
    }
}
