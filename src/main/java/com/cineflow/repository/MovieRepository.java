package com.cineflow.repository;

import com.cineflow.domain.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MovieRepository extends JpaRepository<Movie, Long> {

    List<Movie> findAllByActiveTrueOrderByReleaseDateDescTitleAsc();

    List<Movie> findAllByActiveTrueAndBookingOpenTrueOrderByReleaseDateDescTitleAsc();

    boolean existsByTmdbId(Long tmdbId);

    boolean existsByTmdbIdAndIdNot(Long tmdbId, Long id);

    Optional<Movie> findByIdAndActiveTrue(Long id);
}
