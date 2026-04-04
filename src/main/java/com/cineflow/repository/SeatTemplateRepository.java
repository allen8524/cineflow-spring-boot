package com.cineflow.repository;

import com.cineflow.domain.SeatTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatTemplateRepository extends JpaRepository<SeatTemplate, Long> {

    List<SeatTemplate> findByScreenIdAndActiveTrueOrderBySeatRowAscSeatNumberAsc(Long screenId);

    List<SeatTemplate> findByScreenIdOrderBySeatRowAscSeatNumberAsc(Long screenId);

    long countByScreenIdAndActiveTrue(Long screenId);
}
