package com.cineflow.controller;

import com.cineflow.domain.Booking;
import com.cineflow.domain.Movie;
import com.cineflow.domain.PaymentMethod;
import com.cineflow.domain.Theater;
import com.cineflow.dto.BookingRequestDto;
import com.cineflow.dto.BookingSummaryDto;
import com.cineflow.dto.PaymentResultDto;
import com.cineflow.dto.ScheduleViewDto;
import com.cineflow.service.BookingService;
import com.cineflow.service.MovieService;
import com.cineflow.service.ScheduleService;
import com.cineflow.service.SeatService;
import com.cineflow.service.TheaterService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping
public class BookingController {

    private final BookingService bookingService;
    private final MovieService movieService;
    private final TheaterService theaterService;
    private final ScheduleService scheduleService;
    private final SeatService seatService;

    @GetMapping({"/booking", "/booking.html"})
    public String booking(
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) Long theaterId,
            @RequestParam(required = false) Long scheduleId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model
    ) {
        ScheduleViewDto scheduleFromRequest = scheduleService.findScheduleView(scheduleId).orElse(null);

        if (movieId == null && scheduleFromRequest != null) {
            movieId = scheduleFromRequest.getMovieId();
        }
        if (theaterId == null && scheduleFromRequest != null) {
            theaterId = scheduleFromRequest.getTheaterId();
        }
        if (date == null && scheduleFromRequest != null) {
            date = scheduleFromRequest.getShowDate();
        }

        List<Movie> movies = movieService.getAllMovies();
        Movie selectedMovie = resolveSelectedMovie(movies, movieId);
        List<Theater> theaters = selectedMovie != null
                ? theaterService.getTheatersForMovie(selectedMovie.getId())
                : theaterService.getAllTheaters();
        Theater selectedTheater = resolveSelectedTheater(theaters, theaterId);

        List<LocalDate> availableDates = selectedMovie != null
                ? scheduleService.getAvailableDates(selectedMovie.getId(), selectedTheater != null ? selectedTheater.getId() : null)
                : List.of();
        LocalDate selectedDate = resolveSelectedDate(availableDates, date);

        List<ScheduleViewDto> schedules = selectedMovie != null
                ? scheduleService.getSchedulesForMovieAndTheaterAndDate(
                selectedMovie.getId(),
                selectedTheater != null ? selectedTheater.getId() : null,
                selectedDate
        )
                : List.of();

        ScheduleViewDto selectedSchedule = resolveSelectedSchedule(schedules, scheduleId);

        model.addAttribute("movies", movies);
        model.addAttribute("selectedMovie", selectedMovie);
        model.addAttribute("theaters", theaters);
        model.addAttribute("regions", theaters.stream().map(Theater::getRegion).distinct().toList());
        model.addAttribute("selectedTheater", selectedTheater);
        model.addAttribute("availableDates", availableDates);
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("schedules", schedules);
        model.addAttribute("selectedSchedule", selectedSchedule);
        return "booking/quick";
    }

    @GetMapping({"/booking/history", "/booking-history.html"})
    public String history(Model model) {
        List<Booking> currentBookings = bookingService.getCurrentBookings();
        List<Booking> pastBookings = bookingService.getPastBookings();
        List<Booking> canceledBookings = bookingService.getCanceledBookings();
        Booking previewBooking = !currentBookings.isEmpty() ? currentBookings.get(0) : bookingService.getBookingByCodeOrLatest(null);

        model.addAttribute("currentBookings", currentBookings);
        model.addAttribute("pastBookings", pastBookings);
        model.addAttribute("canceledBookings", canceledBookings);
        model.addAttribute("previewBooking", previewBooking);
        model.addAttribute("currentBookingCount", currentBookings.size());
        model.addAttribute("todayBookingCount", currentBookings.stream().filter(booking -> booking.getStartTime() != null && booking.getStartTime().toLocalDate().equals(LocalDate.now())).count());
        model.addAttribute("pastBookingCount", pastBookings.size());
        model.addAttribute("canceledBookingCount", canceledBookings.size());
        return "booking/history";
    }

    @GetMapping({"/booking/seat", "/booking-seat.html"})
    public String seat(
            @RequestParam(required = false) Long scheduleId,
            @ModelAttribute("bookingRequest") BookingRequestDto bookingRequest,
            Model model
    ) {
        Long resolvedScheduleId = bookingRequest.getScheduleId() != null ? bookingRequest.getScheduleId() : scheduleId;
        ScheduleViewDto selectedSchedule = scheduleService.findScheduleView(resolvedScheduleId).orElseGet(scheduleService::getDefaultScheduleView);

        if (selectedSchedule == null) {
            model.addAttribute("selectedSchedule", null);
            model.addAttribute("seatRows", List.of());
            return "booking/seat";
        }

        if (bookingRequest.getScheduleId() == null) {
            bookingRequest.setScheduleId(selectedSchedule.getScheduleId());
        }
        if (bookingRequest.getPeopleCount() == 0) {
            bookingRequest.setAdultCount(1);
            bookingRequest.setTeenCount(0);
            bookingRequest.setSeniorCount(0);
        }

        model.addAttribute("bookingRequest", bookingRequest);
        model.addAttribute("selectedSchedule", selectedSchedule);
        model.addAttribute("seatRows", seatService.getSeatLayout(selectedSchedule.getScheduleId()));
        model.addAttribute("reservedSeatCodes", seatService.getReservedSeatCodes(selectedSchedule.getScheduleId()));
        model.addAttribute("baseSeatPrice", selectedSchedule.getPrice());
        return "booking/seat";
    }

    @PostMapping("/booking/payment")
    public String paymentPost(@ModelAttribute BookingRequestDto bookingRequest, RedirectAttributes redirectAttributes) {
        try {
            bookingService.createBookingSummary(bookingRequest);
            addBookingRequestAttributes(bookingRequest, redirectAttributes);
            return "redirect:/booking/payment";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            redirectAttributes.addFlashAttribute("bookingRequest", bookingRequest);
            redirectAttributes.addAttribute("scheduleId", bookingRequest.getScheduleId());
            return "redirect:/booking/seat";
        }
    }

    @GetMapping({"/booking/payment", "/booking-payment.html"})
    public String payment(@ModelAttribute("bookingRequest") BookingRequestDto bookingRequest, Model model) {
        BookingSummaryDto bookingSummary = null;
        if (bookingRequest.getScheduleId() != null && bookingRequest.getSeatCodes() != null && !bookingRequest.getSeatCodes().isEmpty()) {
            try {
                bookingSummary = bookingService.createBookingSummary(bookingRequest);
            } catch (IllegalArgumentException | IllegalStateException ex) {
                model.addAttribute("errorMessage", ex.getMessage());
            }
        }

        model.addAttribute("bookingRequest", bookingRequest);
        model.addAttribute("bookingSummary", bookingSummary);
        model.addAttribute("paymentMethods", PaymentMethod.values());
        return "booking/payment";
    }

    @PostMapping("/booking/complete")
    public String completePost(@ModelAttribute BookingRequestDto bookingRequest, RedirectAttributes redirectAttributes) {
        try {
            Booking booking = bookingService.completeBooking(bookingRequest);
            redirectAttributes.addFlashAttribute("successMessage", booking.getBookingCode() + " 예매가 완료되었습니다.");
            redirectAttributes.addAttribute("bookingCode", booking.getBookingCode());
            return "redirect:/booking/complete";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            addBookingRequestFlashAttributes(bookingRequest, redirectAttributes);
            addBookingRequestAttributes(bookingRequest, redirectAttributes);
            return "redirect:/booking/payment";
        }
    }

    @PostMapping("/booking/cancel")
    public String cancel(
            @RequestParam(required = false) String bookingCode,
            @RequestParam(required = false) String cancelReason,
            @RequestParam(required = false) String redirectTo,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Booking booking = bookingService.cancelBooking(bookingCode, cancelReason);
            redirectAttributes.addFlashAttribute("successMessage", booking.getBookingCode() + " 예매가 취소되었습니다.");
            if (redirectTo != null && !redirectTo.isBlank() && "/booking/history".equals(redirectTo)) {
                return "redirect:/booking/history";
            }
            redirectAttributes.addAttribute("bookingCode", booking.getBookingCode());
            return "redirect:/booking/complete";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            if (bookingCode != null && !bookingCode.isBlank()) {
                redirectAttributes.addAttribute("bookingCode", bookingCode);
                return "redirect:/booking/complete";
            }
            return "redirect:/booking/history";
        }
    }

    @GetMapping({"/booking/complete", "/booking-complete.html"})
    public String complete(@RequestParam(required = false) String bookingCode, Model model) {
        Booking booking = bookingService.getBookingByCodeOrLatest(bookingCode);
        PaymentResultDto paymentResult = booking != null && booking.getPayment() != null
                ? PaymentResultDto.from(booking, booking.getPayment())
                : null;

        if (bookingCode != null && !bookingCode.isBlank() && booking == null) {
            model.addAttribute("errorMessage", "예매번호에 해당하는 예매를 찾을 수 없습니다.");
        }

        model.addAttribute("booking", booking);
        model.addAttribute("paymentResult", paymentResult);
        model.addAttribute("canCancel", bookingService.canCancel(booking));
        return "booking/complete";
    }

    private void addBookingRequestAttributes(BookingRequestDto bookingRequest, RedirectAttributes redirectAttributes) {
        redirectAttributes.addAttribute("scheduleId", bookingRequest.getScheduleId());
        if (bookingRequest.getSeatCodes() != null && !bookingRequest.getSeatCodes().isEmpty()) {
            redirectAttributes.addAttribute("seatCodes", String.join(",", bookingRequest.getSeatCodes()));
        }
        redirectAttributes.addAttribute("adultCount", bookingRequest.getAdultCount());
        redirectAttributes.addAttribute("teenCount", bookingRequest.getTeenCount());
        redirectAttributes.addAttribute("seniorCount", bookingRequest.getSeniorCount());
        if (bookingRequest.getCustomerName() != null) {
            redirectAttributes.addAttribute("customerName", bookingRequest.getCustomerName());
        }
        if (bookingRequest.getCustomerPhone() != null) {
            redirectAttributes.addAttribute("customerPhone", bookingRequest.getCustomerPhone());
        }
        if (bookingRequest.getPaymentMethod() != null) {
            redirectAttributes.addAttribute("paymentMethod", bookingRequest.getPaymentMethod());
        }
    }

    private void addBookingRequestFlashAttributes(BookingRequestDto bookingRequest, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("bookingRequest", bookingRequest);
    }

    private Movie resolveSelectedMovie(List<Movie> movies, Long movieId) {
        if (movies.isEmpty()) {
            return null;
        }
        if (movieId == null) {
            return movies.get(0);
        }
        return movies.stream()
                .filter(movie -> movie.getId().equals(movieId))
                .findFirst()
                .orElse(movies.get(0));
    }

    private Theater resolveSelectedTheater(List<Theater> theaters, Long theaterId) {
        if (theaterId == null) {
            return null;
        }
        return theaters.stream()
                .filter(theater -> theater.getId().equals(theaterId))
                .findFirst()
                .orElse(null);
    }

    private LocalDate resolveSelectedDate(List<LocalDate> availableDates, LocalDate date) {
        if (availableDates.isEmpty()) {
            return date;
        }
        if (date != null && availableDates.contains(date)) {
            return date;
        }
        return availableDates.get(0);
    }

    private ScheduleViewDto resolveSelectedSchedule(List<ScheduleViewDto> schedules, Long scheduleId) {
        if (schedules.isEmpty()) {
            return null;
        }
        if (scheduleId == null) {
            return schedules.get(0);
        }
        return schedules.stream()
                .filter(schedule -> schedule.getScheduleId().equals(scheduleId))
                .findFirst()
                .orElse(schedules.get(0));
    }
}
