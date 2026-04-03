package com.cineflow.api;

import com.cineflow.domain.Booking;
import com.cineflow.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/bookings")
public class BookingApiController {

    private final BookingService bookingService;

    @GetMapping("/current")
    public List<Booking> currentBookings() {
        return bookingService.getCurrentBookings();
    }

    @GetMapping("/past")
    public List<Booking> pastBookings() {
        return bookingService.getPastBookings();
    }
}
