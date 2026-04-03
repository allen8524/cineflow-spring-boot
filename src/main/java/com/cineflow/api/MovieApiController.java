package com.cineflow.api;

import com.cineflow.domain.Movie;
import com.cineflow.service.MovieService;
import lombok.RequiredArgsConstructor;
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
    public List<Movie> findAll() {
        return movieService.getAllMovies();
    }

    @GetMapping("/{id}")
    public Movie findOne(@PathVariable Long id) {
        return movieService.getMovie(id);
    }
}
