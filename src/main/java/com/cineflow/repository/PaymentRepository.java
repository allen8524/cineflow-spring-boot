package com.cineflow.repository;

import com.cineflow.domain.Payment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @EntityGraph(attributePaths = {"booking"})
    Optional<Payment> findByBookingId(Long bookingId);
}
