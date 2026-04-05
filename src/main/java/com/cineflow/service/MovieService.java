package com.cineflow.service;

import com.cineflow.domain.Movie;
import com.cineflow.domain.MovieStatus;
import com.cineflow.dto.AdminMovieForm;
import com.cineflow.dto.MovieViewDto;
import com.cineflow.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MovieService {

    private static final String DEFAULT_POSTER_URL = "/images/uploads/movie-single.jpg";
    private static final String DEFAULT_BACKDROP_URL = "/images/uploads/slider-bg.jpg";
    private static final String DEFAULT_SHORT_DESCRIPTION = "\uC601\uD654 \uC18C\uAC1C\uB97C \uC900\uBE44 \uC911\uC785\uB2C8\uB2E4.";
    private static final String DEFAULT_DESCRIPTION = "\uC0C1\uC138 \uC124\uBA85\uC744 \uC900\uBE44 \uC911\uC785\uB2C8\uB2E4.";
    private static final int SHORT_DESCRIPTION_LIMIT = 120;
    private static final int OVERVIEW_SNIPPET_LIMIT = 150;

    private static final Comparator<Movie> FEATURED_MOVIE_COMPARATOR = Comparator
            .comparing((Movie movie) -> movie.getStatus() != MovieStatus.NOW_SHOWING)
            .thenComparing(movie -> !movie.isBookingOpen())
            .thenComparing(Movie::getBookingRate, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(Movie::getScore, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(Movie::getReleaseDate, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(Movie::getTitle, String.CASE_INSENSITIVE_ORDER);

    private static final Comparator<Movie> BOX_OFFICE_COMPARATOR = Comparator
            .comparing(Movie::getBookingRate, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(Movie::getScore, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(Movie::getReleaseDate, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(Movie::getTitle, String.CASE_INSENSITIVE_ORDER);

    private final MovieRepository movieRepository;
    private final TmdbClient tmdbClient;

    public List<Movie> getAllMovies() {
        return movieRepository.findAllByActiveTrueOrderByReleaseDateDescTitleAsc();
    }

    public List<MovieViewDto> getAllMovieViews() {
        return getAllMovies().stream()
                .map(this::toView)
                .toList();
    }

    public List<Movie> getFeaturedMovies(int limit) {
        return getAllMovies().stream()
                .sorted(FEATURED_MOVIE_COMPARATOR)
                .limit(limit)
                .toList();
    }

    public List<Movie> getBoxOfficeMovies(int limit) {
        return getAllMovies().stream()
                .sorted(BOX_OFFICE_COMPARATOR)
                .limit(limit)
                .toList();
    }

    public List<Movie> getNowShowingMovies(int limit) {
        return getAllMovies().stream()
                .filter(movie -> movie.getStatus() == MovieStatus.NOW_SHOWING)
                .sorted(FEATURED_MOVIE_COMPARATOR)
                .limit(limit)
                .toList();
    }

    public List<Movie> getComingSoonMovies(int limit) {
        return getAllMovies().stream()
                .filter(movie -> movie.getStatus() == MovieStatus.COMING_SOON)
                .sorted(Comparator
                        .comparing(Movie::getReleaseDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Movie::getTitle, String.CASE_INSENSITIVE_ORDER))
                .limit(limit)
                .toList();
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
                .orElseThrow(() -> new IllegalArgumentException("Movie not found. id=" + id));
    }

    public Optional<Movie> findActiveMovie(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return movieRepository.findByIdAndActiveTrue(id);
    }

    public Optional<Movie> findActiveMovieByTmdbId(Long tmdbId) {
        if (tmdbId == null) {
            return Optional.empty();
        }
        return movieRepository.findByTmdbIdAndActiveTrue(tmdbId);
    }

    public MovieViewDto getMovieView(Long id) {
        return toView(getMovie(id));
    }

    public Movie getMovieForAdmin(Long id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Movie not found. id=" + id));
    }

    public Movie getMovieOrDefault(Long id) {
        if (id != null) {
            return getMovie(id);
        }

        return getAllMovies().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No active movie is registered."));
    }

    public MovieViewDto getMovieViewOrDefault(Long id) {
        return toView(getMovieOrDefault(id));
    }

    public List<MovieViewDto> getFeaturedMovieViews(int limit) {
        return getFeaturedMovies(limit).stream()
                .map(this::toView)
                .toList();
    }

    public List<MovieViewDto> getBoxOfficeMovieViews(int limit) {
        return getBoxOfficeMovies(limit).stream()
                .map(this::toView)
                .toList();
    }

    public List<MovieViewDto> getNowShowingMovieViews(int limit) {
        return getNowShowingMovies(limit).stream()
                .map(this::toView)
                .toList();
    }

    public List<MovieViewDto> getComingSoonMovieViews(int limit) {
        return getComingSoonMovies(limit).stream()
                .map(this::toView)
                .toList();
    }

    public List<Movie> getRelatedMovies(Long currentMovieId, int limit) {
        return getAllMovies().stream()
                .filter(movie -> !movie.getId().equals(currentMovieId))
                .sorted(FEATURED_MOVIE_COMPARATOR)
                .limit(limit)
                .toList();
    }

    public List<MovieViewDto> getRelatedMovieViews(Long currentMovieId, int limit) {
        return getRelatedMovies(currentMovieId, limit).stream()
                .map(this::toView)
                .toList();
    }

    public MovieViewDto toView(Movie movie) {
        String description = resolveDescription(movie);

        return MovieViewDto.builder()
                .id(movie.getId())
                .title(movie.getTitle())
                .shortDescription(resolveShortDescription(movie))
                .description(description)
                .overviewSnippet(abbreviate(description, OVERVIEW_SNIPPET_LIMIT))
                .genre(trimToNull(movie.getGenre()))
                .ageRating(trimToNull(movie.getAgeRating()))
                .runningTime(resolveRunningTime(movie))
                .posterUrl(resolvePosterUrl(movie))
                .backdropUrl(resolveBackdropUrl(movie))
                .bookingRate(movie.getBookingRate())
                .score(movie.getScore())
                .releaseDate(movie.getReleaseDate())
                .status(movie.getStatus())
                .bookingOpen(movie.isBookingOpen())
                .active(movie.isActive())
                .build();
    }

    @Transactional
    public Movie createMovie(AdminMovieForm form) {
        Movie movie = Movie.builder()
                .bookingRate(0.0)
                .score(0.0)
                .build();
        applyForm(movie, form);
        return saveMovie(movie);
    }

    @Transactional
    public Movie updateMovie(Long id, AdminMovieForm form) {
        Movie movie = getMovieForAdmin(id);
        applyForm(movie, form);
        return saveMovie(movie);
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
        Long resolvedTmdbId = resolveTmdbId(movie, form);

        movie.setTitle(trimToNull(form.getTitle()));
        movie.setShortDescription(resolveShortDescription(form.getShortDescription(), description));
        movie.setDescription(description);
        movie.setGenre(trimToNull(form.getGenre()));
        movie.setAgeRating(trimToNull(form.getAgeRating()));
        movie.setRunningTime(form.getRunningTime());
        movie.setReleaseDate(form.getReleaseDate());
        movie.setPosterUrl(StringUtils.hasText(form.getPosterUrl()) ? form.getPosterUrl().trim() : DEFAULT_POSTER_URL);
        movie.setTmdbId(resolvedTmdbId);
        movie.setPosterPath(resolveStringMetadata(form.getPosterPath(), movie.getPosterPath()));
        movie.setBackdropPath(resolveStringMetadata(form.getBackdropPath(), movie.getBackdropPath()));
        movie.setOverview(resolveStringMetadata(form.getOverview(), movie.getOverview()));
        movie.setRuntimeMinutes(resolveIntegerMetadata(form.getRuntimeMinutes(), movie.getRuntimeMinutes()));
        movie.setStatus(form.getStatus());
        movie.setActive(form.isActive());
        movie.setBookingOpen(form.isActive() && form.isBookingOpen());

        validateTmdbId(resolvedTmdbId, movie.getId());

        if (movie.getBookingRate() == null) {
            movie.setBookingRate(0.0);
        }
        if (movie.getScore() == null) {
            movie.setScore(0.0);
        }
    }

    private Movie saveMovie(Movie movie) {
        try {
            return movieRepository.save(movie);
        } catch (DataIntegrityViolationException exception) {
            if (movie.getTmdbId() != null) {
                throw new IllegalStateException("A movie with the same TMDB id already exists. tmdbId=" + movie.getTmdbId(), exception);
            }
            throw exception;
        }
    }

    private void validateTmdbId(Long tmdbId, Long movieId) {
        if (tmdbId == null) {
            return;
        }

        boolean duplicated = movieId == null
                ? movieRepository.existsByTmdbId(tmdbId)
                : movieRepository.existsByTmdbIdAndIdNot(tmdbId, movieId);

        if (duplicated) {
            throw new IllegalStateException("A movie with the same TMDB id already exists. tmdbId=" + tmdbId);
        }
    }

    private Long resolveTmdbId(Movie movie, AdminMovieForm form) {
        if (form.getTmdbId() != null) {
            return form.getTmdbId();
        }
        return movie.getId() != null ? movie.getTmdbId() : null;
    }

    private Integer resolveIntegerMetadata(Integer requestedValue, Integer currentValue) {
        if (requestedValue != null) {
            return requestedValue;
        }
        return currentValue;
    }

    private String resolveStringMetadata(String requestedValue, String currentValue) {
        if (requestedValue == null) {
            return currentValue;
        }
        return trimToNull(requestedValue);
    }

    private String resolvePosterUrl(Movie movie) {
        return firstNonBlank(
                tmdbClient.buildPosterUrl(movie.getPosterPath()),
                trimToNull(movie.getPosterUrl()),
                DEFAULT_POSTER_URL
        );
    }

    private String resolveBackdropUrl(Movie movie) {
        return firstNonBlank(
                tmdbClient.buildBackdropUrl(movie.getBackdropPath()),
                tmdbClient.buildPosterUrl(movie.getPosterPath()),
                trimToNull(movie.getPosterUrl()),
                DEFAULT_BACKDROP_URL
        );
    }

    private Integer resolveRunningTime(Movie movie) {
        if (movie.getRuntimeMinutes() != null) {
            return movie.getRuntimeMinutes();
        }
        return movie.getRunningTime();
    }

    private String resolveDescription(Movie movie) {
        return firstNonBlank(
                trimToNull(movie.getOverview()),
                trimToNull(movie.getDescription()),
                trimToNull(movie.getShortDescription()),
                DEFAULT_DESCRIPTION
        );
    }

    private String resolveShortDescription(Movie movie) {
        return firstNonBlank(
                trimToNull(movie.getShortDescription()),
                abbreviate(trimToNull(movie.getOverview()), SHORT_DESCRIPTION_LIMIT),
                abbreviate(trimToNull(movie.getDescription()), SHORT_DESCRIPTION_LIMIT),
                DEFAULT_SHORT_DESCRIPTION
        );
    }

    private String resolveShortDescription(String shortDescription, String description) {
        String normalized = trimToNull(shortDescription);
        if (normalized != null) {
            return normalized;
        }
        if (!StringUtils.hasText(description)) {
            return null;
        }
        return description.length() <= SHORT_DESCRIPTION_LIMIT
                ? description
                : description.substring(0, SHORT_DESCRIPTION_LIMIT) + "...";
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

    private String abbreviate(String value, int maxLength) {
        String normalized = trimToNull(value);
        if (normalized == null || normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
