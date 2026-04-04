package com.cineflow.controller;

import com.cineflow.domain.Booking;
import com.cineflow.domain.Movie;
import com.cineflow.domain.PaymentMethod;
import com.cineflow.domain.Theater;
import com.cineflow.domain.User;
import com.cineflow.dto.BookingRequestDto;
import com.cineflow.dto.BookingSummaryDto;
import com.cineflow.dto.PaymentResultDto;
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
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.ArrayList;
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
    public String history(
            @RequestParam(required = false) String bookingCode,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            Model model
    ) {
        User currentUser = extractUser(authenticatedUser);
        return renderHistoryPage(currentUser, bookingCode, model, false);
    }

    @GetMapping("/mypage/bookings")
    public String myBookings(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            Model model
    ) {
        User currentUser = extractUser(authenticatedUser);
        return renderHistoryPage(currentUser, null, model, true);
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
    public String paymentPost(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @ModelAttribute BookingRequestDto bookingRequest,
            RedirectAttributes redirectAttributes
    ) {
        User currentUser = extractUser(authenticatedUser);
        applyCurrentUserDefaults(bookingRequest, currentUser);
        try {
            bookingService.createBookingSummary(bookingRequest, currentUser);
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
    public String payment(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @ModelAttribute("bookingRequest") BookingRequestDto bookingRequest,
            Model model
    ) {
        User currentUser = extractUser(authenticatedUser);
        applyCurrentUserDefaults(bookingRequest, currentUser);

        BookingSummaryDto bookingSummary = null;
        if (bookingRequest.getScheduleId() != null && bookingRequest.getSeatCodes() != null && !bookingRequest.getSeatCodes().isEmpty()) {
            try {
                bookingSummary = bookingService.createBookingSummary(bookingRequest, currentUser);
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
    public String completePost(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @ModelAttribute BookingRequestDto bookingRequest,
            RedirectAttributes redirectAttributes
    ) {
        User currentUser = extractUser(authenticatedUser);
        applyCurrentUserDefaults(bookingRequest, currentUser);
        try {
            Booking booking = bookingService.completeBooking(bookingRequest, currentUser);
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
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(required = false) String bookingCode,
            @RequestParam(required = false) String cancelReason,
            @RequestParam(required = false) String redirectTo,
            RedirectAttributes redirectAttributes
    ) {
        User currentUser = extractUser(authenticatedUser);
        try {
            Booking booking = bookingService.cancelBooking(bookingCode, cancelReason, currentUser);
            redirectAttributes.addFlashAttribute("successMessage", booking.getBookingCode() + " 예매가 취소되었습니다.");
            if (redirectTo != null && !redirectTo.isBlank()) {
                return "redirect:" + redirectTo;
            }
            redirectAttributes.addAttribute("bookingCode", booking.getBookingCode());
            return "redirect:/booking/complete";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            if (bookingCode != null && !bookingCode.isBlank()) {
                redirectAttributes.addAttribute("bookingCode", bookingCode);
                return "redirect:/booking/complete";
            }
            if (redirectTo != null && !redirectTo.isBlank()) {
                return "redirect:" + redirectTo;
            }
            return currentUser != null ? "redirect:/mypage/bookings" : "redirect:/booking/history";
        }
    }

    @GetMapping({"/booking/complete", "/booking-complete.html"})
    public String complete(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(required = false) String bookingCode,
            Model model
    ) {
        User currentUser = extractUser(authenticatedUser);
        Booking booking = bookingService.getAccessibleBookingByCodeOrLatest(bookingCode, currentUser);
        PaymentResultDto paymentResult = booking != null && booking.getPayment() != null
                ? PaymentResultDto.from(booking, booking.getPayment())
                : null;

        if (StringUtils.hasText(bookingCode) && booking == null) {
            model.addAttribute("errorMessage", "예매번호에 해당하는 예매를 찾을 수 없거나 접근 권한이 없습니다.");
        }

        model.addAttribute("booking", booking);
        model.addAttribute("paymentResult", paymentResult);
        model.addAttribute("canCancel", bookingService.canCancel(booking));
        model.addAttribute("historyUrl", resolveHistoryUrl(currentUser, booking));
        return "booking/complete";
    }

    private String renderHistoryPage(User currentUser, String bookingCode, Model model, boolean myPage) {
        List<Booking> currentBookings;
        List<Booking> pastBookings;
        List<Booking> canceledBookings;
        Booking previewBooking;
        String historyModeLabel;

        if (currentUser != null) {
            currentBookings = bookingService.getCurrentBookingsForUser(currentUser);
            pastBookings = bookingService.getPastBookingsForUser(currentUser);
            canceledBookings = bookingService.getCanceledBookingsForUser(currentUser);
            previewBooking = !currentBookings.isEmpty()
                    ? currentBookings.get(0)
                    : bookingService.getAccessibleBookingByCodeOrLatest(null, currentUser);
            historyModeLabel = currentUser.getName() + "님의 예매내역";
            model.addAttribute("guestLookupMode", false);
        } else if (StringUtils.hasText(bookingCode)) {
            Booking guestBooking = bookingService.findAccessibleBookingByCode(bookingCode, null).orElse(null);
            currentBookings = new ArrayList<>();
            pastBookings = new ArrayList<>();
            canceledBookings = new ArrayList<>();

            if (guestBooking == null) {
                model.addAttribute("errorMessage", "예매번호에 해당하는 비회원 예매를 찾을 수 없습니다.");
            } else if (guestBooking.getStatus() == com.cineflow.domain.BookingStatus.CANCELED) {
                canceledBookings.add(guestBooking);
            } else if (guestBooking.getStartTime() != null && guestBooking.getStartTime().isAfter(java.time.LocalDateTime.now())) {
                currentBookings.add(guestBooking);
            } else {
                pastBookings.add(guestBooking);
            }

            previewBooking = guestBooking;
            historyModeLabel = "비회원 예매 조회";
            model.addAttribute("guestLookupMode", true);
        } else {
            currentBookings = List.of();
            pastBookings = List.of();
            canceledBookings = List.of();
            previewBooking = null;
            historyModeLabel = "비회원 예매 조회";
            model.addAttribute("guestLookupMode", true);
            model.addAttribute("guestLookupRequired", true);
        }

        model.addAttribute("currentBookings", currentBookings);
        model.addAttribute("pastBookings", pastBookings);
        model.addAttribute("canceledBookings", canceledBookings);
        model.addAttribute("previewBooking", previewBooking);
        model.addAttribute("currentBookingCount", currentBookings.size());
        model.addAttribute("todayBookingCount", currentBookings.stream().filter(booking -> booking.getStartTime() != null && booking.getStartTime().toLocalDate().equals(LocalDate.now())).count());
        model.addAttribute("pastBookingCount", pastBookings.size());
        model.addAttribute("canceledBookingCount", canceledBookings.size());
        model.addAttribute("isMyPage", currentUser != null || myPage);
        model.addAttribute("historyModeLabel", historyModeLabel);
        model.addAttribute("lookupBookingCode", bookingCode);
        return "booking/history";
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

    private User extractUser(AuthenticatedUser authenticatedUser) {
        return authenticatedUser != null ? authenticatedUser.getUser() : null;
    }

    private void applyCurrentUserDefaults(BookingRequestDto bookingRequest, User currentUser) {
        if (bookingRequest == null || currentUser == null) {
            return;
        }
        if (!StringUtils.hasText(bookingRequest.getCustomerName())) {
            bookingRequest.setCustomerName(currentUser.getName());
        }
        if (!StringUtils.hasText(bookingRequest.getCustomerPhone())) {
            bookingRequest.setCustomerPhone(currentUser.getPhone());
        }
    }

    private String resolveHistoryUrl(User currentUser, Booking booking) {
        if (currentUser != null) {
            return "/mypage/bookings";
        }
        if (booking != null) {
            return "/booking/history?bookingCode=" + booking.getBookingCode();
        }
        return "/booking/history";
    }
}
