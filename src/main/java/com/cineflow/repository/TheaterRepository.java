package com.cineflow.repository;

import com.cineflow.domain.Theater;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TheaterRepository extends JpaRepository<Theater, Long> {

    List<Theater> findAllByOrderByRegionAscNameAsc();

    List<Theater> findAllByActiveTrueOrderByRegionAscNameAsc();

    @Query("""
            select distinct t
            from Theater t
            join t.screens s
            join s.schedules sc
            join sc.movie m
            where sc.movie.id = :movieId
              and t.active = true
              and s.active = true
              and sc.active = true
              and m.active = true
              and m.bookingOpen = true
            order by t.name asc
            """)
    List<Theater> findDistinctTheatersByMovieId(@Param("movieId") Long movieId);
}
