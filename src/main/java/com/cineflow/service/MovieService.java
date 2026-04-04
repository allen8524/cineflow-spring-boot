package com.cineflow.service;

import com.cineflow.domain.Movie;
import com.cineflow.dto.AdminMovieForm;
import com.cineflow.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MovieService {

    private static final String DEFAULT_POSTER_URL = "/images/uploads/movie-single.jpg";

    private final MovieRepository movieRepository;

    public List<Movie> getAllMovies() {
        return movieRepository.findAllByActiveTrueOrderByReleaseDateDescTitleAsc();
    }

    public List<Movie> getBookableMovies() {
        return movieRepository.findAllByActiveTrueAndBookingOpenTrueOrderByReleaseDateDescTitleAsc();
    }

    public List<Movie> getAllMoviesForAdmin() {
        return movieRepository.findAll(Sort.by(
                Sort.Order.desc("active"),
                Sort.Order.desc("releaseDate"),
                Sort.Order.asc("title")
        ));
    }

    public Movie getMovie(Long id) {
        return movieRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new IllegalArgumentException("영화를 찾을 수 없습니다. id=" + id));
    }

    public Movie getMovieForAdmin(Long id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("영화를 찾을 수 없습니다. id=" + id));
    }

    public Movie getMovieOrDefault(Long id) {
        if (id != null) {
            return getMovie(id);
        }
        return getAllMovies().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("등록된 영화가 없습니다."));
    }

    @Transactional
    public Movie createMovie(AdminMovieForm form) {
        Movie movie = Movie.builder()
                .bookingRate(0.0)
                .score(0.0)
                .build();
        applyForm(movie, form);
        return movieRepository.save(movie);
    }

    @Transactional
    public Movie updateMovie(Long id, AdminMovieForm form) {
        Movie movie = getMovieForAdmin(id);
        applyForm(movie, form);
        return movieRepository.save(movie);
    }

    @Transactional
    public void deactivateMovie(Long id) {
        Movie movie = getMovieForAdmin(id);
        movie.setActive(false);
        movie.setBookingOpen(false);
        movieRepository.save(movie);
    }

    private void applyForm(Movie movie, AdminMovieForm form) {
        String description = trimToNull(form.getDescription());
        movie.setTitle(trimToNull(form.getTitle()));
        movie.setShortDescription(resolveShortDescription(form.getShortDescription(), description));
        movie.setDescription(description);
        movie.setGenre(trimToNull(form.getGenre()));
        movie.setAgeRating(trimToNull(form.getAgeRating()));
        movie.setRunningTime(form.getRunningTime());
        movie.setReleaseDate(form.getReleaseDate());
        movie.setPosterUrl(StringUtils.hasText(form.getPosterUrl()) ? form.getPosterUrl().trim() : DEFAULT_POSTER_URL);
        movie.setStatus(form.getStatus());
        movie.setActive(form.isActive());
        movie.setBookingOpen(form.isActive() && form.isBookingOpen());

        if (movie.getBookingRate() == null) {
            movie.setBookingRate(0.0);
        }
        if (movie.getScore() == null) {
            movie.setScore(0.0);
        }
    }

    private String resolveShortDescription(String shortDescription, String description) {
        String normalized = trimToNull(shortDescription);
        if (normalized != null) {
            return normalized;
        }
        if (!StringUtils.hasText(description)) {
            return null;
        }
        return description.length() <= 120 ? description : description.substring(0, 120) + "...";
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
