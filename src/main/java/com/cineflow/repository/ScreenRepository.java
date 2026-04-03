package com.cineflow.repository;

import com.cineflow.domain.Screen;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScreenRepository extends JpaRepository<Screen, Long> {

    List<Screen> findByTheaterIdOrderByNameAsc(Long theaterId);
}
