package com.cineflow.repository;

import com.cineflow.domain.Booking;
import com.cineflow.domain.BookingStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @EntityGraph(attributePaths = {"bookingSeats", "payment", "schedule", "schedule.movie", "schedule.screen", "schedule.screen.theater", "user"})
    Optional<Booking> findByBookingCode(String bookingCode);

    @EntityGraph(attributePaths = {"bookingSeats", "payment", "schedule", "schedule.movie", "schedule.screen", "schedule.screen.theater", "user"})
    Optional<Booking> findTopByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"bookingSeats", "payment", "schedule", "schedule.movie", "schedule.screen", "schedule.screen.theater", "user"})
    List<Booking> findAllByOrderByStartTimeAsc();

    @EntityGraph(attributePaths = {"bookingSeats", "payment", "schedule", "schedule.movie", "schedule.screen", "schedule.screen.theater", "user"})
    List<Booking> findAllByOrderByStartTimeDesc();

    @EntityGraph(attributePaths = {"bookingSeats", "payment", "schedule", "schedule.movie", "schedule.screen", "schedule.screen.theater", "user"})
    List<Booking> findAllByStatusOrderByStartTimeAsc(BookingStatus status);

    @EntityGraph(attributePaths = {"bookingSeats", "payment", "schedule", "schedule.movie", "schedule.screen", "schedule.screen.theater", "user"})
    List<Booking> findAllByStatusOrderByCanceledAtDesc(BookingStatus status);

    @EntityGraph(attributePaths = {"bookingSeats", "payment", "schedule", "schedule.movie", "schedule.screen", "schedule.screen.theater", "user"})
    List<Booking> findAllByScheduleIdOrderByCreatedAtDesc(Long scheduleId);

    @EntityGraph(attributePaths = {"bookingSeats", "payment", "schedule", "schedule.movie", "schedule.screen", "schedule.screen.theater", "user"})
    List<Booking> findAllByUserIdOrderByStartTimeAsc(Long userId);

    @EntityGraph(attributePaths = {"bookingSeats", "payment", "schedule", "schedule.movie", "schedule.screen", "schedule.screen.theater", "user"})
    List<Booking> findAllByUserIdOrderByStartTimeDesc(Long userId);

    @EntityGraph(attributePaths = {"bookingSeats", "payment", "schedule", "schedule.movie", "schedule.screen", "schedule.screen.theater", "user"})
    List<Booking> findAllByUserIdAndStatusOrderByCanceledAtDesc(Long userId, BookingStatus status);

    @EntityGraph(attributePaths = {"bookingSeats", "payment", "schedule", "schedule.movie", "schedule.screen", "schedule.screen.theater", "user"})
    Optional<Booking> findTopByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsByScheduleId(Long scheduleId);
}
