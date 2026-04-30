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
    public List<AdminTmdbMovieSearchResultDto> searchMovies(
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "keyword", required = false) String keyword
    ) {
        String resolvedQuery = firstNonBlank(query, q, keyword);
        return adminMovieTmdbService.searchMovies(resolvedQuery);
    }

    @GetMapping("/{tmdbId}")
    public AdminTmdbMovieDetailDto getMovieDetail(@PathVariable Long tmdbId) {
        return adminMovieTmdbService.getMovieDetail(tmdbId);
    }

    @ExceptionHandler(TmdbClientException.class)
    public ResponseEntity<AdminTmdbErrorResponseDto> handleTmdbClientException(TmdbClientException exception) {
        log.warn("TMDB admin request failed.", exception);
        return buildErrorResponse(resolveTmdbFailureStatus(exception), resolveTmdbFailureMessage(exception));
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
                        .code(resolveCode(status))
                        .build());
    }

    private String resolveBadRequestMessage(Exception exception) {
        if (exception instanceof MethodArgumentTypeMismatchException) {
            return "TMDB movie id must be a number.";
        }
        return exception.getMessage() != null ? exception.getMessage() : "Invalid TMDB admin request.";
    }

    private HttpStatus resolveTmdbFailureStatus(TmdbClientException exception) {
        if (exception.isConfigurationError()) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }
        return HttpStatus.BAD_GATEWAY;
    }

    private String resolveTmdbFailureMessage(TmdbClientException exception) {
        return exception.getMessage() != null ? exception.getMessage() : "TMDB request failed.";
    }

    private String resolveCode(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "BAD_REQUEST";
            case SERVICE_UNAVAILABLE -> "TMDB_NOT_CONFIGURED";
            case BAD_GATEWAY -> "TMDB_UPSTREAM_ERROR";
            default -> "TMDB_REQUEST_FAILED";
        };
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
