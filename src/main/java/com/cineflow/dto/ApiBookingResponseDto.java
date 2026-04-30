package com.cineflow.dto;

import com.cineflow.domain.Booking;
import com.cineflow.domain.Payment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

@Getter
@Builder
public class ApiBookingResponseDto {

    private final Long bookingId;
    private final String reservationNumber;
    private final String movieTitle;
    private final String theaterName;
    private final String screenName;
    private final LocalDate showDate;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final List<String> seats;
    private final Integer totalPrice;
    private final String bookingStatus;
    private final String paymentStatus;
    private final LocalDateTime createdAt;

    public static ApiBookingResponseDto from(Booking booking) {
        Payment payment = booking != null ? booking.getPayment() : null;
        return ApiBookingResponseDto.builder()
                .bookingId(booking != null ? booking.getId() : null)
                .reservationNumber(booking != null ? booking.getBookingCode() : null)
                .movieTitle(booking != null ? booking.getMovieTitle() : null)
                .theaterName(booking != null ? booking.getTheaterName() : null)
                .screenName(booking != null ? booking.getScreenName() : null)
                .showDate(booking != null && booking.getStartTime() != null ? booking.getStartTime().toLocalDate() : null)
                .startTime(booking != null && booking.getStartTime() != null ? booking.getStartTime().toLocalTime() : null)
                .endTime(booking != null && booking.getEndTime() != null ? booking.getEndTime().toLocalTime() : null)
                .seats(parseSeats(booking != null ? booking.getSeatNames() : null))
                .totalPrice(booking != null ? booking.getTotalPrice() : null)
                .bookingStatus(booking != null && booking.getStatus() != null ? booking.getStatus().name() : null)
                .paymentStatus(payment != null && payment.getPaymentStatus() != null ? payment.getPaymentStatus().name() : null)
                .createdAt(booking != null ? booking.getCreatedAt() : null)
                .build();
    }

    private static List<String> parseSeats(String seatNames) {
        if (seatNames == null || seatNames.isBlank()) {
            return List.of();
        }
        return Arrays.stream(seatNames.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }
}
