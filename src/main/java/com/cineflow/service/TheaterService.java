package com.cineflow.service;

import com.cineflow.domain.Theater;
import com.cineflow.repository.TheaterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TheaterService {

    private final TheaterRepository theaterRepository;

    public List<Theater> getAllTheaters() {
        return theaterRepository.findAllByOrderByRegionAscNameAsc();
    }

    public List<Theater> getTheatersForMovie(Long movieId) {
        if (movieId == null) {
            return getAllTheaters();
        }
        return theaterRepository.findDistinctTheatersByMovieId(movieId);
    }

    public Theater getTheaterOrNull(Long theaterId) {
        if (theaterId == null) {
            return null;
        }
        return theaterRepository.findById(theaterId).orElse(null);
    }
}
