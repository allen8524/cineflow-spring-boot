package com.cineflow.service;

import com.cineflow.domain.Booking;
import com.cineflow.domain.BookingStatus;
import com.cineflow.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;

    public List<Booking> getCurrentBookings() {
        return bookingRepository.findAll(Sort.by(Sort.Direction.ASC, "startTime"))
                .stream()
                .filter(booking -> booking.getStatus() == BookingStatus.BOOKED
                        || booking.getStatus() == BookingStatus.UPCOMING
                        || booking.getStatus() == BookingStatus.SOON)
                .toList();
    }

    public List<Booking> getPastBookings() {
        return bookingRepository.findAll(Sort.by(Sort.Direction.DESC, "startTime"))
                .stream()
                .filter(booking -> booking.getStatus() == BookingStatus.USED)
                .toList();
    }
}
