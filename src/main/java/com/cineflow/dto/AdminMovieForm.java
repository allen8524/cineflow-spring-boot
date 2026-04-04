package com.cineflow.dto;

import com.cineflow.domain.Movie;
import com.cineflow.domain.MovieStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
public class AdminMovieForm {

    @NotBlank(message = "Please enter a movie title.")
    @Size(max = 200, message = "Movie title must be 200 characters or fewer.")
    private String title;

    @Size(max = 300, message = "Short description must be 300 characters or fewer.")
    private String shortDescription;

    @NotBlank(message = "Please enter a description.")
    @Size(max = 2000, message = "Description must be 2000 characters or fewer.")
    private String description;

    @NotBlank(message = "Please enter a genre.")
    @Size(max = 100, message = "Genre must be 100 characters or fewer.")
    private String genre;

    @NotBlank(message = "Please select an age rating.")
    @Size(max = 20, message = "Age rating is invalid.")
    private String ageRating;

    @NotNull(message = "Please enter running time.")
    @Min(value = 1, message = "Running time must be at least 1 minute.")
    private Integer runningTime;

    @NotNull(message = "Please select a release date.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate releaseDate;

    @Size(max = 255, message = "Poster path must be 255 characters or fewer.")
    private String posterUrl;

    @Positive(message = "TMDB id must be greater than 0.")
    private Long tmdbId;

    @Size(max = 255, message = "Poster path must be 255 characters or fewer.")
    private String posterPath;

    @Size(max = 255, message = "Backdrop path must be 255 characters or fewer.")
    private String backdropPath;

    @Size(max = 2000, message = "Overview must be 2000 characters or fewer.")
    private String overview;

    @Min(value = 1, message = "Runtime minutes must be at least 1 minute.")
    private Integer runtimeMinutes;

    @NotNull(message = "Please select a movie status.")
    private MovieStatus status;

    private boolean bookingOpen = true;

    private boolean active = true;

    public static AdminMovieForm from(Movie movie) {
        AdminMovieForm form = new AdminMovieForm();
        form.setTitle(movie.getTitle());
        form.setShortDescription(movie.getShortDescription());
        form.setDescription(movie.getDescription());
        form.setGenre(movie.getGenre());
        form.setAgeRating(movie.getAgeRating());
        form.setRunningTime(movie.getRunningTime());
        form.setReleaseDate(movie.getReleaseDate());
        form.setPosterUrl(movie.getPosterUrl());
        form.setTmdbId(movie.getTmdbId());
        form.setPosterPath(movie.getPosterPath());
        form.setBackdropPath(movie.getBackdropPath());
        form.setOverview(movie.getOverview());
        form.setRuntimeMinutes(movie.getRuntimeMinutes());
        form.setStatus(movie.getStatus());
        form.setBookingOpen(movie.isBookingOpen());
        form.setActive(movie.isActive());
        return form;
    }
}
