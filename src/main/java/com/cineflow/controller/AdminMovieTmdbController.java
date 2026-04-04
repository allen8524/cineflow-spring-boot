package com.cineflow.controller;

import com.cineflow.dto.AdminTmdbErrorResponseDto;
import com.cineflow.dto.AdminTmdbMovieDetailDto;
import com.cineflow.dto.AdminTmdbMovieSearchResultDto;
import com.cineflow.service.AdminMovieTmdbService;
import com.cineflow.service.TmdbClientException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/movies/tmdb")
public class AdminMovieTmdbController {

    private final AdminMovieTmdbService adminMovieTmdbService;

    @GetMapping("/search")
    public List<AdminTmdbMovieSearchResultDto> searchMovies(@RequestParam(name = "query", required = false) String query) {
        return adminMovieTmdbService.searchMovies(query);
    }

    @GetMapping("/{tmdbId}")
    public AdminTmdbMovieDetailDto getMovieDetail(@PathVariable Long tmdbId) {
        return adminMovieTmdbService.getMovieDetail(tmdbId);
    }

    @ExceptionHandler(TmdbClientException.class)
    public ResponseEntity<AdminTmdbErrorResponseDto> handleTmdbClientException(TmdbClientException exception) {
        log.warn("TMDB admin request failed.", exception);
        return buildErrorResponse(HttpStatus.BAD_GATEWAY, exception.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<AdminTmdbErrorResponseDto> handleBadRequest(Exception exception) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, resolveBadRequestMessage(exception));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<AdminTmdbErrorResponseDto> handleUnexpectedException(Exception exception) {
        log.error("Unexpected TMDB admin request error.", exception);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to process TMDB admin request.");
    }

    private ResponseEntity<AdminTmdbErrorResponseDto> buildErrorResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(AdminTmdbErrorResponseDto.builder()
                        .status(status.value())
                        .message(message)
                        .build());
    }

    private String resolveBadRequestMessage(Exception exception) {
        if (exception instanceof MethodArgumentTypeMismatchException) {
            return "TMDB movie id must be a number.";
        }
        return exception.getMessage();
    }
}
