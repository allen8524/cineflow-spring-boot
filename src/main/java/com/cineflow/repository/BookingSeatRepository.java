package com.cineflow.repository;

import com.cineflow.domain.BookingSeat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingSeatRepository extends JpaRepository<BookingSeat, Long> {

    List<BookingSeat> findByBookingIdOrderBySeatRowAscSeatNumberAsc(Long bookingId);
}
