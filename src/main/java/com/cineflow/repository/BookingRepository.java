package com.cineflow.repository;

import com.cineflow.domain.Booking;
import com.cineflow.domain.BookingStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @EntityGraph(attributePaths = {"bookingSeats", "payment", "schedule", "schedule.movie", "schedule.screen", "schedule.screen.theater"})
    Optional<Booking> findByBookingCode(String bookingCode);

    @EntityGraph(attributePaths = {"bookingSeats", "payment", "schedule", "schedule.movie", "schedule.screen", "schedule.screen.theater"})
    Optional<Booking> findTopByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"bookingSeats", "payment", "schedule", "schedule.movie", "schedule.screen", "schedule.screen.theater"})
    List<Booking> findAllByOrderByStartTimeAsc();

    @EntityGraph(attributePaths = {"bookingSeats", "payment", "schedule", "schedule.movie", "schedule.screen", "schedule.screen.theater"})
    List<Booking> findAllByOrderByStartTimeDesc();

    @EntityGraph(attributePaths = {"bookingSeats", "payment", "schedule", "schedule.movie", "schedule.screen", "schedule.screen.theater"})
    List<Booking> findAllByStatusOrderByStartTimeAsc(BookingStatus status);

    @EntityGraph(attributePaths = {"bookingSeats", "payment", "schedule", "schedule.movie", "schedule.screen", "schedule.screen.theater"})
    List<Booking> findAllByStatusOrderByCanceledAtDesc(BookingStatus status);

    @EntityGraph(attributePaths = {"bookingSeats", "payment", "schedule", "schedule.movie", "schedule.screen", "schedule.screen.theater"})
    List<Booking> findAllByScheduleIdOrderByCreatedAtDesc(Long scheduleId);
}
