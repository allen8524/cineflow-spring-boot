package com.cineflow.repository;

import com.cineflow.domain.Screen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ScreenRepository extends JpaRepository<Screen, Long> {

    List<Screen> findByTheaterIdOrderByNameAsc(Long theaterId);

    List<Screen> findByTheaterIdAndActiveTrueOrderByNameAsc(Long theaterId);

    Optional<Screen> findByIdAndActiveTrue(Long id);

    @Query("""
            select s
            from Screen s
            join fetch s.theater t
            order by t.name asc, s.name asc
            """)
    List<Screen> findAllWithTheaterOrderByTheaterNameAscNameAsc();

    @Query("""
            select s
            from Screen s
            join fetch s.theater t
            where s.active = true
              and t.active = true
            order by t.name asc, s.name asc
            """)
    List<Screen> findAllActiveWithTheaterOrderByTheaterNameAscNameAsc();
}
