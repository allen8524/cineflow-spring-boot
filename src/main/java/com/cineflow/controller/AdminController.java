package com.cineflow.controller;

import com.cineflow.domain.Booking;
import com.cineflow.domain.BookingStatus;
import com.cineflow.domain.User;
import com.cineflow.dto.AdminDashboardDto;
import com.cineflow.dto.AdminScheduleRowDto;
import com.cineflow.dto.ScheduleViewDto;
import com.cineflow.security.AuthenticatedUser;
import com.cineflow.service.BookingService;
import com.cineflow.service.MovieService;
import com.cineflow.service.ScheduleService;
import com.cineflow.service.SeatService;
import com.cineflow.service.TheaterService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {

    private final BookingService bookingService;
    private final MovieService movieService;
    private final TheaterService theaterService;
    private final ScheduleService scheduleService;
    private final SeatService seatService;

    @GetMapping
    public String index(Model model) {
        List<ScheduleViewDto> schedules = scheduleService.getAllScheduleViews();
        List<Booking> bookings = bookingService.getBookingsForAdmin(null, null, null, null);

        AdminDashboardDto dashboard = AdminDashboardDto.builder()
                .totalMovies(movieService.getAllMovies().size())
                .totalSchedules(schedules.size())
                .totalBookings(bookings.size())
                .upcomingBookings(bookingService.getCurrentBookings().size())
                .canceledBookings(bookingService.getCanceledBookings().size())
                .totalRevenue(bookingService.getTotalRevenue())
                .todaySchedules(schedules.stream().filter(schedule -> schedule.getShowDate().equals(LocalDate.now())).count())
                .todayBookings(bookings.stream().filter(booking -> booking.getStartTime() != null && booking.getStartTime().toLocalDate().equals(LocalDate.now())).count())
                .build();

        model.addAttribute("dashboard", dashboard);
        model.addAttribute("recentBookings", bookings.stream().limit(6).toList());
        model.addAttribute("todaySchedules", schedules.stream()
                .filter(schedule -> schedule.getShowDate().equals(LocalDate.now()))
                .limit(6)
                .map(AdminScheduleRowDto::from)
                .toList());
        return "admin/index";
    }

    @GetMapping("/bookings")
    public String bookings(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) Long theaterId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model
    ) {
        model.addAttribute("bookings", bookingService.getBookingsForAdmin(status, movieId, theaterId, date));
        model.addAttribute("movies", movieService.getAllMovies());
        model.addAttribute("theaters", theaterService.getAllTheaters());
        model.addAttribute("statuses", BookingStatus.values());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedMovieId", movieId);
        model.addAttribute("selectedTheaterId", theaterId);
        model.addAttribute("selectedDate", date);
        return "admin/bookings";
    }

    @GetMapping("/schedules")
    public String schedules(
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) Long theaterId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model
    ) {
        model.addAttribute("schedules", scheduleService.getSchedulesForAdmin(movieId, theaterId, date).stream().map(AdminScheduleRowDto::from).toList());
        model.addAttribute("movies", movieService.getAllMovies());
        model.addAttribute("theaters", theaterService.getAllTheaters());
        model.addAttribute("selectedMovieId", movieId);
        model.addAttribute("selectedTheaterId", theaterId);
        model.addAttribute("selectedDate", date);
        return "admin/schedules";
    }

    @GetMapping("/schedules/{id}")
    public String scheduleDetail(@PathVariable Long id, Model model) {
        ScheduleViewDto schedule = scheduleService.findScheduleView(id).orElse(null);

        model.addAttribute("schedule", schedule);
        model.addAttribute("scheduleRow", schedule != null ? AdminScheduleRowDto.from(schedule) : null);
        model.addAttribute("seatRows", schedule != null ? seatService.getSeatLayout(id) : List.of());
        model.addAttribute("reservedSeatCount", schedule != null ? seatService.countReservedSeats(id) : 0);
        model.addAttribute("availableSeatCount", schedule != null ? seatService.countAvailableSeats(id) : 0);
        model.addAttribute("bookings", bookingService.getBookingsForSchedule(id));
        return "admin/schedule-detail";
    }

    @PostMapping("/bookings/{bookingCode}/cancel")
    public String cancelBooking(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable String bookingCode,
            @RequestParam(required = false) String cancelReason,
            @RequestParam(required = false) String redirectTo,
            RedirectAttributes redirectAttributes
    ) {
        User currentUser = authenticatedUser != null ? authenticatedUser.getUser() : null;
        try {
            Booking booking = bookingService.cancelBooking(bookingCode, cancelReason, currentUser);
            redirectAttributes.addFlashAttribute("successMessage", booking.getBookingCode() + " 예매를 취소했습니다.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:" + sanitizeRedirect(redirectTo);
    }

    private String sanitizeRedirect(String redirectTo) {
        if (redirectTo == null || redirectTo.isBlank()) {
            return "/admin/bookings";
        }
        if (redirectTo.startsWith("/admin/schedules/")
                || redirectTo.startsWith("/admin/schedules")
                || redirectTo.startsWith("/admin/bookings")
                || "/admin".equals(redirectTo)) {
            return redirectTo;
        }
        return "/admin/bookings";
    }
}
