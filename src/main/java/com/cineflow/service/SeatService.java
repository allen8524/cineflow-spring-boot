package com.cineflow.service;

import com.cineflow.domain.Booking;
import com.cineflow.domain.ScheduleSeat;
import com.cineflow.dto.SeatRowDto;
import com.cineflow.dto.SeatViewDto;
import com.cineflow.repository.ScheduleSeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SeatService {

    private static final int PREMIUM_SURCHARGE = 3000;
    private static final int COUPLE_SURCHARGE = 5000;
    private static final Comparator<String> SEAT_CODE_COMPARATOR = Comparator
            .comparing((String seatCode) -> seatCode.replaceAll("\\d", ""))
            .thenComparingInt(seatCode -> Integer.parseInt(seatCode.replaceAll("\\D", "")));

    private final ScheduleSeatRepository scheduleSeatRepository;

    public List<SeatRowDto> getSeatLayout(Long scheduleId) {
        Map<String, List<SeatViewDto>> groupedRows = scheduleSeatRepository
                .findByScheduleIdOrderBySeatTemplateSeatRowAscSeatTemplateSeatNumberAsc(scheduleId)
                .stream()
                .map(scheduleSeat -> SeatViewDto.from(scheduleSeat, resolveSeatPrice(scheduleSeat)))
                .collect(Collectors.groupingBy(
                        SeatViewDto::getSeatRow,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return groupedRows.entrySet().stream()
                .map(entry -> SeatRowDto.builder()
                        .rowLabel(entry.getKey())
                        .seats(entry.getValue())
                        .build())
                .toList();
    }

    public List<String> getReservedSeatCodes(Long scheduleId) {
        return scheduleSeatRepository.findByScheduleIdAndReservedTrueOrderBySeatTemplateSeatRowAscSeatTemplateSeatNumberAsc(scheduleId)
                .stream()
                .map(scheduleSeat -> scheduleSeat.getSeatTemplate().getSeatCode())
                .toList();
    }

    public long countReservedSeats(Long scheduleId) {
        return scheduleSeatRepository.countByScheduleIdAndReservedTrue(scheduleId);
    }

    public long countAvailableSeats(Long scheduleId) {
        return scheduleSeatRepository.countByScheduleIdAndReservedFalse(scheduleId);
    }

    public List<String> normalizeSeatCodes(Collection<String> seatCodes) {
        if (seatCodes == null) {
            return List.of();
        }
        return seatCodes.stream()
                .filter(StringUtils::hasText)
                .flatMap(seatCode -> Arrays.stream(seatCode.split(",")))
                .map(String::trim)
                .map(seatCode -> seatCode.toUpperCase(Locale.ROOT))
                .filter(StringUtils::hasText)
                .distinct()
                .sorted(SEAT_CODE_COMPARATOR)
                .toList();
    }

    public void validateSeatSelection(Long scheduleId, Collection<String> seatCodes, int expectedCount) {
        List<String> normalizedSeatCodes = normalizeSeatCodes(seatCodes);
        if (normalizedSeatCodes.isEmpty()) {
            throw new IllegalArgumentException("좌석은 1개 이상 선택해 주세요.");
        }
        if (normalizedSeatCodes.size() != expectedCount) {
            throw new IllegalArgumentException("선택한 좌석 수와 관람 인원이 일치해야 합니다.");
        }

        List<ScheduleSeat> scheduleSeats = scheduleSeatRepository.findByScheduleIdAndSeatTemplateSeatCodeIn(scheduleId, normalizedSeatCodes);
        if (scheduleSeats.size() != normalizedSeatCodes.size()) {
            throw new IllegalArgumentException("선택한 좌석 정보를 다시 확인해 주세요.");
        }

        if (scheduleSeats.stream().anyMatch(this::isUnavailable)) {
            throw new IllegalStateException("이미 예약된 좌석이 포함되어 있습니다. 좌석을 다시 선택해 주세요.");
        }
    }

    public int calculateTotalPrice(Long scheduleId, Collection<String> seatCodes) {
        List<String> normalizedSeatCodes = normalizeSeatCodes(seatCodes);
        return scheduleSeatRepository.findByScheduleIdAndSeatTemplateSeatCodeIn(scheduleId, normalizedSeatCodes)
                .stream()
                .mapToInt(this::resolveSeatPrice)
                .sum();
    }

    public List<ScheduleSeat> lockSeatsForReservation(Long scheduleId, Collection<String> seatCodes, int expectedCount) {
        List<String> normalizedSeatCodes = normalizeSeatCodes(seatCodes);
        if (normalizedSeatCodes.size() != expectedCount) {
            throw new IllegalArgumentException("선택한 좌석 수와 관람 인원이 일치해야 합니다.");
        }

        List<ScheduleSeat> scheduleSeats = scheduleSeatRepository.findForUpdateByScheduleIdAndSeatCodes(scheduleId, normalizedSeatCodes);
        if (scheduleSeats.size() != normalizedSeatCodes.size()) {
            throw new IllegalArgumentException("선택한 좌석 정보가 유효하지 않습니다.");
        }
        if (scheduleSeats.stream().anyMatch(this::isUnavailable)) {
            throw new IllegalStateException("이미 예약된 좌석이 있어 예매를 완료할 수 없습니다. 좌석을 다시 선택해 주세요.");
        }
        return scheduleSeats;
    }

    public void releaseReservedSeatsForBooking(Booking booking) {
        if (booking == null || booking.getSchedule() == null) {
            return;
        }

        List<String> seatCodes = extractSeatCodes(booking);
        if (seatCodes.isEmpty()) {
            throw new IllegalStateException("복구할 좌석 정보가 없습니다.");
        }

        List<ScheduleSeat> scheduleSeats = scheduleSeatRepository.findForUpdateByScheduleIdAndSeatCodes(
                booking.getSchedule().getId(),
                seatCodes
        );

        if (scheduleSeats.size() != seatCodes.size()) {
            throw new IllegalStateException("복구할 좌석 정보를 모두 찾지 못했습니다.");
        }

        scheduleSeats.forEach(scheduleSeat -> {
            scheduleSeat.setReserved(false);
            scheduleSeat.setHeld(false);
            scheduleSeat.setHoldExpiresAt(null);
        });
        scheduleSeatRepository.saveAll(scheduleSeats);
    }

    public List<String> extractSeatCodes(Booking booking) {
        if (booking == null) {
            return List.of();
        }
        if (booking.getBookingSeats() != null && !booking.getBookingSeats().isEmpty()) {
            return booking.getBookingSeats().stream()
                    .map(bookingSeat -> bookingSeat.getSeatCode())
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .map(seatCode -> seatCode.toUpperCase(Locale.ROOT))
                    .sorted(SEAT_CODE_COMPARATOR)
                    .toList();
        }
        return normalizeSeatCodes(booking.getSeatNames() == null ? List.of() : List.of(booking.getSeatNames()));
    }

    public int resolveSeatPrice(ScheduleSeat scheduleSeat) {
        if (scheduleSeat.getPriceOverride() != null) {
            return scheduleSeat.getPriceOverride();
        }

        return switch (scheduleSeat.getSeatTemplate().getSeatType()) {
            case PREMIUM -> scheduleSeat.getSchedule().getPrice() + PREMIUM_SURCHARGE;
            case COUPLE -> scheduleSeat.getSchedule().getPrice() + COUPLE_SURCHARGE;
            case STANDARD -> scheduleSeat.getSchedule().getPrice();
        };
    }

    private boolean isUnavailable(ScheduleSeat scheduleSeat) {
        if (scheduleSeat.isReserved()) {
            return true;
        }
        if (!scheduleSeat.isHeld()) {
            return false;
        }
        return scheduleSeat.getHoldExpiresAt() == null || scheduleSeat.getHoldExpiresAt().isAfter(LocalDateTime.now());
    }
}
