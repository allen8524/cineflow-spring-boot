package com.cineflow.api;

import com.cineflow.domain.User;
import com.cineflow.dto.ApiBookingResponseDto;
import com.cineflow.dto.ApiErrorResponseDto;
import com.cineflow.service.BookingService;
import com.cineflow.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/bookings")
public class BookingApiController {

    private final BookingService bookingService;
    private final UserService userService;

    @GetMapping("/current")
    public List<ApiBookingResponseDto> currentBookings() {
        User currentUser = userService.findCurrentUser()
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Authentication required."));
        return bookingService.getCurrentBookingsForUser(currentUser).stream()
                .map(ApiBookingResponseDto::from)
                .toList();
    }

    @GetMapping("/past")
    public List<ApiBookingResponseDto> pastBookings() {
        User currentUser = userService.findCurrentUser()
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Authentication required."));
        return bookingService.getPastBookingsForUser(currentUser).stream()
                .map(ApiBookingResponseDto::from)
                .toList();
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponseDto> handleUnauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiErrorResponseDto.builder().message("Authentication required.").code("UNAUTHORIZED").build());
    }
}
