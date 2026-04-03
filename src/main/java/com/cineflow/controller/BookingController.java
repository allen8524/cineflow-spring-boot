package com.cineflow.controller;

import com.cineflow.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping
public class BookingController {

    private final BookingService bookingService;

    @GetMapping({"/booking", "/booking.html"})
    public String booking() {
        return "booking/quick";
    }

    @GetMapping({"/booking/history", "/booking-history.html"})
    public String history(Model model) {
        model.addAttribute("currentBookings", bookingService.getCurrentBookings());
        model.addAttribute("pastBookings", bookingService.getPastBookings());
        return "booking/history";
    }

    @GetMapping({"/booking/seat", "/booking-seat.html"})
    public String seat() {
        return "booking/seat";
    }

    @GetMapping({"/booking/payment", "/booking-payment.html"})
    public String payment() {
        return "booking/payment";
    }

    @GetMapping({"/booking/complete", "/booking-complete.html"})
    public String complete() {
        return "booking/complete";
    }
}
