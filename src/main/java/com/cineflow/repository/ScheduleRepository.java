package com.cineflow.repository;

import com.cineflow.domain.Schedule;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    @EntityGraph(attributePaths = {"movie", "screen", "screen.theater"})
    List<Schedule> findByActiveTrueOrderByStartTimeAsc();

    @EntityGraph(attributePaths = {"movie", "screen", "screen.theater"})
    List<Schedule> findByMovieIdAndActiveTrueOrderByStartTimeAsc(Long movieId);

    @EntityGraph(attributePaths = {"movie", "screen", "screen.theater"})
    List<Schedule> findByMovieIdAndStartTimeBetweenAndActiveTrueOrderByStartTimeAsc(
            Long movieId,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    @EntityGraph(attributePaths = {"movie", "screen", "screen.theater"})
    List<Schedule> findByScreenTheaterIdAndActiveTrueOrderByStartTimeAsc(Long theaterId);

    @EntityGraph(attributePaths = {"movie", "screen", "screen.theater"})
    List<Schedule> findByMovieIdAndScreenTheaterIdAndActiveTrueOrderByStartTimeAsc(Long movieId, Long theaterId);

    @EntityGraph(attributePaths = {"movie", "screen", "screen.theater"})
    List<Schedule> findByMovieIdAndScreenTheaterIdAndStartTimeBetweenAndActiveTrueOrderByStartTimeAsc(
            Long movieId,
            Long theaterId,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    @EntityGraph(attributePaths = {"movie", "screen", "screen.theater"})
    Optional<Schedule> findByIdAndActiveTrue(Long id);
}
