package com.cineflow.service;

import com.cineflow.domain.Booking;
import com.cineflow.domain.BookingSeat;
import com.cineflow.domain.BookingStatus;
import com.cineflow.domain.Payment;
import com.cineflow.domain.PaymentStatus;
import com.cineflow.domain.Schedule;
import com.cineflow.domain.ScheduleSeat;
import com.cineflow.domain.User;
import com.cineflow.dto.BookingRequestDto;
import com.cineflow.dto.BookingSummaryDto;
import com.cineflow.repository.BookingRepository;
import com.cineflow.repository.BookingSeatRepository;
import com.cineflow.repository.ScheduleRepository;
import com.cineflow.repository.ScheduleSeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService {

    private static final DateTimeFormatter BOOKING_CODE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleSeatRepository scheduleSeatRepository;
    private final SeatService seatService;
    private final PaymentService paymentService;

    public List<Booking> getCurrentBookings() {
        return bookingRepository.findAllByOrderByStartTimeAsc().stream()
                .filter(booking -> booking.getStatus() != BookingStatus.CANCELED)
                .filter(this::isUpcomingBooking)
                .toList();
    }

    public List<Booking> getPastBookings() {
        return bookingRepository.findAllByOrderByStartTimeDesc().stream()
                .filter(booking -> booking.getStatus() != BookingStatus.CANCELED)
                .filter(this::isPastBooking)
                .toList();
    }

    public List<Booking> getCanceledBookings() {
        return bookingRepository.findAllByStatusOrderByCanceledAtDesc(BookingStatus.CANCELED);
    }

    public List<Booking> getCurrentBookingsForUser(User user) {
        if (user == null || user.getId() == null) {
            return List.of();
        }
        return bookingRepository.findAllByUserIdOrderByStartTimeAsc(user.getId()).stream()
                .filter(booking -> booking.getStatus() != BookingStatus.CANCELED)
                .filter(this::isUpcomingBooking)
                .toList();
    }

    public List<Booking> getPastBookingsForUser(User user) {
        if (user == null || user.getId() == null) {
            return List.of();
        }
        return bookingRepository.findAllByUserIdOrderByStartTimeDesc(user.getId()).stream()
                .filter(booking -> booking.getStatus() != BookingStatus.CANCELED)
                .filter(this::isPastBooking)
                .toList();
    }

    public List<Booking> getCanceledBookingsForUser(User user) {
        if (user == null || user.getId() == null) {
            return List.of();
        }
        return bookingRepository.findAllByUserIdAndStatusOrderByCanceledAtDesc(user.getId(), BookingStatus.CANCELED);
    }

    public List<Booking> getBookingsForAdmin(BookingStatus status, Long movieId, Long theaterId, LocalDate date) {
        return bookingRepository.findAllByOrderByStartTimeDesc().stream()
                .filter(booking -> status == null || booking.getStatus() == status)
                .filter(booking -> movieId == null || (booking.getSchedule() != null && booking.getSchedule().getMovie().getId().equals(movieId)))
                .filter(booking -> theaterId == null || (booking.getSchedule() != null && booking.getSchedule().getScreen().getTheater().getId().equals(theaterId)))
                .filter(booking -> date == null || (booking.getStartTime() != null && booking.getStartTime().toLocalDate().equals(date)))
                .toList();
    }

    public List<Booking> getBookingsForSchedule(Long scheduleId) {
        if (scheduleId == null) {
            return List.of();
        }
        return bookingRepository.findAllByScheduleIdOrderByCreatedAtDesc(scheduleId);
    }

    public Optional<Booking> findBookingByCode(String bookingCode) {
        if (!StringUtils.hasText(bookingCode)) {
            return Optional.empty();
        }
        return bookingRepository.findByBookingCode(bookingCode.trim());
    }

    public Optional<Booking> findAccessibleBookingByCode(String bookingCode, User actor) {
        return findBookingByCode(bookingCode)
                .filter(booking -> canAccessBooking(booking, actor));
    }

    public Booking getAccessibleBookingByCodeOrLatest(String bookingCode, User actor) {
        if (StringUtils.hasText(bookingCode)) {
            return findAccessibleBookingByCode(bookingCode, actor).orElse(null);
        }
        if (actor == null || actor.getId() == null) {
            return null;
        }
        return bookingRepository.findTopByUserIdOrderByCreatedAtDesc(actor.getId()).orElse(null);
    }

    public long getTotalRevenue() {
        return bookingRepository.findAllByOrderByStartTimeDesc().stream()
                .filter(booking -> booking.getPayment() != null)
                .filter(booking -> booking.getPayment().getPaymentStatus() == PaymentStatus.PAID)
                .mapToLong(booking -> booking.getTotalPrice() != null ? booking.getTotalPrice() : 0)
                .sum();
    }

    public BookingSummaryDto createBookingSummary(BookingRequestDto request) {
        return createBookingSummary(request, null);
    }

    public BookingSummaryDto createBookingSummary(BookingRequestDto request, User bookingUser) {
        Schedule schedule = getScheduleOrThrow(request != null ? request.getScheduleId() : null);
        List<String> normalizedSeatCodes = seatService.normalizeSeatCodes(request.getSeatCodes());
        String customerName = resolveCustomerName(request, bookingUser);
        String customerPhone = resolveCustomerPhone(request, bookingUser);

        validateRequest(request, false, customerName, customerPhone);
        seatService.validateSeatSelection(request.getScheduleId(), normalizedSeatCodes, request.getPeopleCount());

        int totalPrice = seatService.calculateTotalPrice(request.getScheduleId(), normalizedSeatCodes);

        return BookingSummaryDto.builder()
                .scheduleId(schedule.getId())
                .movieId(schedule.getMovie().getId())
                .theaterId(schedule.getScreen().getTheater().getId())
                .movieTitle(schedule.getMovie().getTitle())
                .posterUrl(schedule.getMovie().getPosterUrl())
                .ageRating(schedule.getMovie().getAgeRating())
                .theaterName(schedule.getScreen().getTheater().getName())
                .screenName(schedule.getScreen().getName())
                .screenType(schedule.getScreen().getScreenType())
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .seatCodes(normalizedSeatCodes)
                .adultCount(safeCount(request.getAdultCount()))
                .teenCount(safeCount(request.getTeenCount()))
                .seniorCount(safeCount(request.getSeniorCount()))
                .peopleCount(request.getPeopleCount())
                .totalPrice(totalPrice)
                .basePrice(schedule.getPrice())
                .availableSeats(schedule.getAvailableSeats())
                .customerName(customerName)
                .customerPhone(customerPhone)
                .paymentMethod(request.getPaymentMethod())
                .build();
    }

    @Transactional
    public Booking completeBooking(BookingRequestDto request) {
        return completeBooking(request, null);
    }

    @Transactional
    public Booking completeBooking(BookingRequestDto request, User bookingUser) {
        Schedule schedule = getScheduleOrThrow(request != null ? request.getScheduleId() : null);
        List<String> normalizedSeatCodes = seatService.normalizeSeatCodes(request.getSeatCodes());
        String customerName = resolveCustomerName(request, bookingUser);
        String customerPhone = resolveCustomerPhone(request, bookingUser);

        validateRequest(request, true, customerName, customerPhone);

        List<ScheduleSeat> lockedSeats = seatService.lockSeatsForReservation(
                request.getScheduleId(),
                normalizedSeatCodes,
                request.getPeopleCount()
        );

        int totalPrice = lockedSeats.stream()
                .mapToInt(seatService::resolveSeatPrice)
                .sum();

        Booking booking = bookingRepository.save(Booking.builder()
                .bookingCode(generateBookingCode(schedule.getStartTime()))
                .customerName(customerName)
                .customerPhone(customerPhone)
                .movieTitle(schedule.getMovie().getTitle())
                .posterUrl(schedule.getMovie().getPosterUrl())
                .ageRating(schedule.getMovie().getAgeRating())
                .theaterName(schedule.getScreen().getTheater().getName())
                .screenName(schedule.getScreen().getName())
                .screenType(schedule.getScreen().getScreenType())
                .seatNames(String.join(", ", normalizedSeatCodes))
                .peopleCount(request.getPeopleCount())
                .totalPrice(totalPrice)
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .schedule(schedule)
                .user(bookingUser)
                .status(BookingStatus.BOOKED)
                .build());

        List<BookingSeat> bookingSeats = lockedSeats.stream()
                .map(scheduleSeat -> BookingSeat.builder()
                        .booking(booking)
                        .seatCode(scheduleSeat.getSeatTemplate().getSeatCode())
                        .seatRow(scheduleSeat.getSeatTemplate().getSeatRow())
                        .seatNumber(scheduleSeat.getSeatTemplate().getSeatNumber())
                        .seatType(scheduleSeat.getSeatTemplate().getSeatType())
                        .price(seatService.resolveSeatPrice(scheduleSeat))
                        .build())
                .toList();
        bookingSeatRepository.saveAll(bookingSeats);
        booking.setBookingSeats(bookingSeats);

        lockedSeats.forEach(scheduleSeat -> {
            scheduleSeat.setReserved(true);
            scheduleSeat.setHeld(false);
            scheduleSeat.setHoldExpiresAt(null);
        });
        scheduleSeatRepository.saveAll(lockedSeats);

        refreshScheduleAvailability(schedule);

        Payment payment = paymentService.processSuccessfulPayment(booking, request.getPaymentMethod(), totalPrice);
        booking.setPayment(payment);
        booking.setUpdatedAt(LocalDateTime.now());

        return booking;
    }

    @Transactional
    public Booking cancelBooking(String bookingCode, String cancelReason) {
        return cancelBooking(bookingCode, cancelReason, null);
    }

    @Transactional
    public Booking cancelBooking(String bookingCode, String cancelReason, User actor) {
        Booking booking = findBookingByCode(bookingCode)
                .orElseThrow(() -> new IllegalArgumentException("예매번호에 해당하는 예매를 찾을 수 없습니다."));

        validateCancellationAccess(booking, actor);
        validateCancelable(booking);

        Payment payment = paymentService.cancelPayment(booking);

        if (booking.getSchedule() != null) {
            seatService.releaseReservedSeatsForBooking(booking);
            refreshScheduleAvailability(booking.getSchedule());
        }

        booking.setStatus(BookingStatus.CANCELED);
        booking.setCancelReason(trimToNull(cancelReason));
        booking.setCanceledAt(LocalDateTime.now());
        booking.setUpdatedAt(LocalDateTime.now());
        booking.setPayment(payment);

        return bookingRepository.save(booking);
    }

    public boolean canCancel(Booking booking) {
        return booking != null && booking.isCancelable();
    }

    public boolean canAccessBooking(Booking booking, User actor) {
        if (booking == null) {
            return false;
        }
        if (actor != null && actor.getRole() != null && actor.getRole().name().equals("ADMIN")) {
            return true;
        }
        if (booking.getUser() == null) {
            return actor == null;
        }
        return actor != null && actor.getId() != null && actor.getId().equals(booking.getUser().getId());
    }

    private void refreshScheduleAvailability(Schedule schedule) {
        schedule.setAvailableSeats((int) scheduleSeatRepository.countByScheduleIdAndReservedFalse(schedule.getId()));
        scheduleRepository.save(schedule);
    }

    private void validateCancellationAccess(Booking booking, User actor) {
        if (booking.getUser() == null) {
            return;
        }
        if (!canAccessBooking(booking, actor)) {
            throw new IllegalStateException("본인 예매만 확인하거나 취소할 수 있습니다.");
        }
    }

    private void validateCancelable(Booking booking) {
        if (booking.getStatus() == BookingStatus.CANCELED) {
            throw new IllegalStateException("이미 취소된 예매입니다.");
        }
        if (booking.getStatus() != BookingStatus.BOOKED) {
            throw new IllegalStateException("예매완료 상태의 예매만 취소할 수 있습니다.");
        }
        if (booking.getStartTime() == null) {
            throw new IllegalStateException("상영 시작 시간이 없어 취소할 수 없습니다.");
        }
        if (!booking.getStartTime().isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("상영 시작 전까지만 취소할 수 있습니다.");
        }
        if (booking.getPayment() == null) {
            throw new IllegalStateException("결제 정보가 없어 취소를 진행할 수 없습니다.");
        }
    }

    private boolean isUpcomingBooking(Booking booking) {
        return booking.getStartTime() != null && booking.getStartTime().isAfter(LocalDateTime.now());
    }

    private boolean isPastBooking(Booking booking) {
        return booking.getStartTime() != null && !booking.getStartTime().isAfter(LocalDateTime.now());
    }

    private void validateRequest(BookingRequestDto request, boolean requireCustomerInfo, String customerName, String customerPhone) {
        if (request == null || request.getScheduleId() == null) {
            throw new IllegalArgumentException("상영 회차 정보가 없습니다.");
        }
        if (request.getPeopleCount() <= 0) {
            throw new IllegalArgumentException("관람 인원을 1명 이상 선택해 주세요.");
        }
        if (seatService.normalizeSeatCodes(request.getSeatCodes()).size() != request.getPeopleCount()) {
            throw new IllegalArgumentException("선택한 좌석 수와 관람 인원이 일치해야 합니다.");
        }
        if (requireCustomerInfo && !StringUtils.hasText(customerName)) {
            throw new IllegalArgumentException("예매자 이름을 입력해 주세요.");
        }
        if (requireCustomerInfo && !StringUtils.hasText(customerPhone)) {
            throw new IllegalArgumentException("연락처를 입력해 주세요.");
        }
    }

    private Schedule getScheduleOrThrow(Long scheduleId) {
        return scheduleRepository.findByIdAndActiveTrue(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("선택한 상영시간표를 찾을 수 없습니다."));
    }

    private String generateBookingCode(LocalDateTime startTime) {
        String prefix = "CF" + BOOKING_CODE_TIME_FORMATTER.format(startTime) + "-";
        String bookingCode;
        do {
            bookingCode = prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 4).toUpperCase(Locale.ROOT);
        } while (bookingRepository.findByBookingCode(bookingCode).isPresent());
        return bookingCode;
    }

    private int safeCount(Integer count) {
        return count == null ? 0 : Math.max(count, 0);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String resolveCustomerName(BookingRequestDto request, User bookingUser) {
        String customerName = trimToNull(request != null ? request.getCustomerName() : null);
        if (customerName == null && bookingUser != null) {
            return trimToNull(bookingUser.getName());
        }
        return customerName;
    }

    private String resolveCustomerPhone(BookingRequestDto request, User bookingUser) {
        String customerPhone = trimToNull(request != null ? request.getCustomerPhone() : null);
        if (customerPhone == null && bookingUser != null) {
            return trimToNull(bookingUser.getPhone());
        }
        return customerPhone;
    }
}
