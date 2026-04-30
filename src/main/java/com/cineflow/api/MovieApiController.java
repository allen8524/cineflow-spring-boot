package com.cineflow.api;

import com.cineflow.dto.ApiErrorResponseDto;
import com.cineflow.dto.ApiMovieResponseDto;
import com.cineflow.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/movies")
public class MovieApiController {

    private final MovieService movieService;

    @GetMapping
    public List<ApiMovieResponseDto> findAll() {
        return movieService.getAllMovies().stream()
                .map(ApiMovieResponseDto::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ApiMovieResponseDto findOne(@PathVariable Long id) {
        return ApiMovieResponseDto.from(movieService.getMovie(id));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponseDto> handleNotFound(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponseDto.builder()
                        .message(exception.getMessage() != null ? exception.getMessage() : "Movie not found.")
                        .code("MOVIE_NOT_FOUND")
                        .build());
    }
}
