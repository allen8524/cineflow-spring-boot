package com.cineflow.service;

import com.cineflow.domain.Movie;
import com.cineflow.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepository movieRepository;

    public List<Movie> getAllMovies() {
        return movieRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }

    public Movie getMovie(Long id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("영화를 찾을 수 없습니다. id=" + id));
    }

    public Movie getMovieOrDefault(Long id) {
        if (id != null) {
            return getMovie(id);
        }
        return movieRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("등록된 영화가 없습니다."));
    }
}