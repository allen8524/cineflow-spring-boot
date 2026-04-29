package com.cineflow.controller;

import com.cineflow.domain.Booking;
import com.cineflow.domain.BookingStatus;
import com.cineflow.domain.MovieStatus;
import com.cineflow.dto.MovieViewDto;
import com.cineflow.dto.ScheduleViewDto;
import com.cineflow.service.BookingService;
import com.cineflow.service.MovieService;
import com.cineflow.service.ScheduleService;
import com.cineflow.service.SeatService;
import com.cineflow.service.TheaterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class BookingControllerPageTest {

    @Mock
    private BookingService bookingService;

    @Mock
    private MovieService movieService;

    @Mock
    private TheaterService theaterService;

    @Mock
    private ScheduleService scheduleService;

    @Mock
    private SeatService seatService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new BookingController(bookingService, movieService, theaterService, scheduleService, seatService)
                )
                .build();
    }

    @Test
    void bookingRendersSuccessfullyWithBookableMovies() throws Exception {
        MovieViewDto movie = MovieViewDto.builder()
                .id(1L)
                .title("Booking Movie")
                .status(MovieStatus.NOW_SHOWING)
                .bookingOpen(true)
                .active(true)
                .build();

        when(movieService.getBookableMovieViews()).thenReturn(List.of(movie));
        when(theaterService.getTheatersForMovie(1L)).thenReturn(List.of());
        when(scheduleService.getAvailableDates(1L, null)).thenReturn(List.of());

        mockMvc.perform(get("/booking"))
                .andExpect(status().isOk())
                .andExpect(view().name("booking/quick"))
                .andExpect(model().attributeExists("movies"))
                .andExpect(model().attributeExists("selectedMovie"))
                .andExpect(model().attributeExists("schedules"));
    }

    @Test
    void bookingPrefersMovieWithAvailableSchedulesByDefault() throws Exception {
        MovieViewDto firstMovie = MovieViewDto.builder()
                .id(7L)
                .title("No Schedule Movie")
                .status(MovieStatus.NOW_SHOWING)
                .bookingOpen(true)
                .active(true)
                .build();
        MovieViewDto secondMovie = MovieViewDto.builder()
                .id(1L)
                .title("Available Movie")
                .status(MovieStatus.NOW_SHOWING)
                .bookingOpen(true)
                .active(true)
                .build();
        LocalDate availableDate = LocalDate.of(2026, 4, 29);

        when(movieService.getBookableMovieViews()).thenReturn(List.of(firstMovie, secondMovie));
        when(scheduleService.getAvailableDates(7L, null)).thenReturn(List.of());
        when(scheduleService.getAvailableDates(1L, null)).thenReturn(List.of(availableDate));
        when(theaterService.getTheatersForMovie(1L)).thenReturn(List.of());
        when(scheduleService.getSchedulesForMovieAndTheaterAndDate(1L, null, availableDate)).thenReturn(List.of());

        mockMvc.perform(get("/booking"))
                .andExpect(status().isOk())
                .andExpect(view().name("booking/quick"))
                .andExpect(model().attribute("selectedMovie", hasProperty("id", is(1L))))
                .andExpect(model().attribute("selectedDate", is(availableDate)));
    }

    @Test
    void legacyBookingRedirectsToCanonicalRoute() throws Exception {
        mockMvc.perform(get("/booking.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/booking"));
    }

    @Test
    void legacySeatRedirectsToCanonicalRoute() throws Exception {
        mockMvc.perform(get("/booking-seat.html").param("scheduleId", "10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/booking/seat?scheduleId=10"));
    }

    @Test
    void legacyPaymentRedirectsToCanonicalRoute() throws Exception {
        mockMvc.perform(get("/booking-payment.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/booking/payment"));
    }

    @Test
    void legacyCompleteRedirectsToCanonicalRoute() throws Exception {
        mockMvc.perform(get("/booking-complete.html").param("bookingCode", "CF20260429-1940-ABCD"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/booking/complete?bookingCode=CF20260429-1940-ABCD"));
    }

    @Test
    void legacyHistoryRedirectsToCanonicalRouteWithLookupParams() throws Exception {
        mockMvc.perform(get("/booking-history.html")
                        .param("bookingCode", "CF20260429-1940-ABCD")
                        .param("customerPhone", "010-1234-5678"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/booking/history?bookingCode=CF20260429-1940-ABCD&customerPhone=010-1234-5678"));
    }

    @Test
    void seatRedirectsToBookingWhenScheduleIdIsMissing() throws Exception {
        mockMvc.perform(get("/booking/seat"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/booking"));
    }

    @Test
    void seatRedirectsToBookingWhenScheduleIdIsInvalid() throws Exception {
        when(scheduleService.findScheduleView(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/booking/seat").param("scheduleId", "999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/booking"));
    }

    @Test
    void seatRendersSuccessfullyWhenScheduleExists() throws Exception {
        ScheduleViewDto schedule = ScheduleViewDto.builder()
                .scheduleId(77L)
                .movieId(1L)
                .movieTitle("Booking Movie")
                .theaterId(3L)
                .theaterName("CineFlow 강남")
                .screenName("1관")
                .screenType("IMAX")
                .startTime(LocalDateTime.of(2026, 4, 29, 19, 40))
                .endTime(LocalDateTime.of(2026, 4, 29, 21, 40))
                .price(22000)
                .availableSeats(100)
                .totalSeats(120)
                .build();

        when(scheduleService.findScheduleView(77L)).thenReturn(Optional.of(schedule));
        when(seatService.getSeatLayout(77L)).thenReturn(List.of());
        when(seatService.getReservedSeatCodes(77L)).thenReturn(List.of());

        mockMvc.perform(get("/booking/seat").param("scheduleId", "77"))
                .andExpect(status().isOk())
                .andExpect(view().name("booking/seat"))
                .andExpect(model().attributeExists("selectedSchedule"))
                .andExpect(model().attributeExists("seatRows"))
                .andExpect(model().attributeExists("baseSeatPrice"));
    }

    @Test
    void completeRendersSuccessfullyWithReservationNumberAlias() throws Exception {
        Booking booking = sampleBooking("CF20260429-1940-ABCD", "01012345678");

        when(bookingService.getAccessibleBookingByCodeOrLatest("CF20260429-1940-ABCD", null)).thenReturn(booking);

        mockMvc.perform(get("/booking/complete").param("reservationNumber", "CF20260429-1940-ABCD"))
                .andExpect(status().isOk())
                .andExpect(view().name("booking/complete"))
                .andExpect(model().attribute("booking", booking))
                .andExpect(model().attributeExists("historyUrl"));
    }

    @Test
    void completeRendersSuccessfullyWithBookingIdAlias() throws Exception {
        Booking booking = sampleBooking("CF20260429-1940-ABCD", "01012345678");

        when(bookingService.findAccessibleBookingById(5L, null)).thenReturn(Optional.of(booking));

        mockMvc.perform(get("/booking/complete").param("bookingId", "5"))
                .andExpect(status().isOk())
                .andExpect(view().name("booking/complete"))
                .andExpect(model().attribute("booking", booking));
    }

    @Test
    void completeRendersEmptyStateWhenLookupIsMissing() throws Exception {
        mockMvc.perform(get("/booking/complete"))
                .andExpect(status().isOk())
                .andExpect(view().name("booking/complete"))
                .andExpect(model().attributeDoesNotExist("errorMessage"))
                .andExpect(model().attribute("booking", org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void historyRendersGuestLookupWhenPhoneMatches() throws Exception {
        Booking booking = sampleBooking("CF20260429-1940-ABCD", "01012345678");

        when(bookingService.findAccessibleBookingByCode("CF20260429-1940-ABCD", null)).thenReturn(Optional.of(booking));

        mockMvc.perform(get("/booking/history")
                        .param("bookingCode", "CF20260429-1940-ABCD")
                        .param("customerPhone", "010-1234-5678"))
                .andExpect(status().isOk())
                .andExpect(view().name("booking/history"))
                .andExpect(model().attribute("currentBookingCount", 1))
                .andExpect(model().attribute("viewMode", "guest"))
                .andExpect(model().attribute("isGuestLookup", true));
    }

    @Test
    void historyShowsMessageWhenGuestPhoneDoesNotMatch() throws Exception {
        Booking booking = sampleBooking("CF20260429-1940-ABCD", "01012345678");

        when(bookingService.findAccessibleBookingByCode("CF20260429-1940-ABCD", null)).thenReturn(Optional.of(booking));

        mockMvc.perform(get("/booking/history")
                        .param("bookingCode", "CF20260429-1940-ABCD")
                        .param("customerPhone", "010-0000-0000"))
                .andExpect(status().isOk())
                .andExpect(view().name("booking/history"))
                .andExpect(model().attribute("currentBookingCount", 0))
                .andExpect(model().attributeExists("errorMessage"));
    }

    @Test
    void myBookingsRedirectsToLoginWhenUserIsMissing() throws Exception {
        mockMvc.perform(get("/mypage/bookings"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    private Booking sampleBooking(String bookingCode, String customerPhone) {
        return Booking.builder()
                .bookingCode(bookingCode)
                .customerName("Codex Tester")
                .customerPhone(customerPhone)
                .movieTitle("Booking Movie")
                .theaterName("CineFlow Gangnam")
                .screenName("1관")
                .screenType("IMAX")
                .seatNames("A1")
                .peopleCount(1)
                .totalPrice(22000)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .status(BookingStatus.BOOKED)
                .build();
    }
}
