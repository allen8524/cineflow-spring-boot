package com.cineflow.service;

import com.cineflow.domain.Booking;
import com.cineflow.domain.Schedule;
import com.cineflow.domain.ScheduleSeat;
import com.cineflow.domain.Screen;
import com.cineflow.domain.SeatTemplate;
import com.cineflow.domain.SeatType;
import com.cineflow.dto.SeatRowDto;
import com.cineflow.dto.SeatViewDto;
import com.cineflow.repository.ScheduleSeatRepository;
import com.cineflow.repository.SeatTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private static final int SEATS_PER_ROW = 12;
    private static final Comparator<String> SEAT_CODE_COMPARATOR = Comparator
            .comparing((String seatCode) -> seatCode.replaceAll("\\d", ""))
            .thenComparingInt(seatCode -> Integer.parseInt(seatCode.replaceAll("\\D", "")));

    private final ScheduleSeatRepository scheduleSeatRepository;
    private final SeatTemplateRepository seatTemplateRepository;

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

    public List<SeatTemplate> getActiveSeatTemplatesForScreen(Long screenId) {
        return seatTemplateRepository.findByScreenIdAndActiveTrueOrderBySeatRowAscSeatNumberAsc(screenId);
    }

    public long countActiveSeatTemplatesForScreen(Long screenId) {
        return seatTemplateRepository.countByScreenIdAndActiveTrue(screenId);
    }

    public void syncSeatTemplatesForScreen(Screen screen) {
        if (screen == null || screen.getId() == null) {
            throw new IllegalArgumentException("상영관 정보가 없어 좌석 템플릿을 생성할 수 없습니다.");
        }

        List<SeatTemplate> existingTemplates = seatTemplateRepository.findByScreenIdOrderBySeatRowAscSeatNumberAsc(screen.getId());
        if (!existingTemplates.isEmpty()) {
            seatTemplateRepository.deleteAllInBatch(existingTemplates);
            seatTemplateRepository.flush();
        }

        seatTemplateRepository.saveAll(buildSeatTemplates(screen));
    }

    public void rebuildScheduleSeatsForSchedule(Schedule schedule) {
        if (schedule == null || schedule.getId() == null) {
            throw new IllegalArgumentException("회차 정보가 없어 좌석 상태를 생성할 수 없습니다.");
        }

        List<SeatTemplate> seatTemplates = getActiveSeatTemplatesForScreen(schedule.getScreen().getId());
        if (seatTemplates.isEmpty()) {
            throw new IllegalStateException("활성 좌석 템플릿이 없어 회차를 생성할 수 없습니다.");
        }

        scheduleSeatRepository.deleteByScheduleId(schedule.getId());
        scheduleSeatRepository.flush();

        List<ScheduleSeat> scheduleSeats = seatTemplates.stream()
                .map(seatTemplate -> ScheduleSeat.builder()
                        .schedule(schedule)
                        .seatTemplate(seatTemplate)
                        .reserved(false)
                        .held(false)
                        .holdExpiresAt(null)
                        .priceOverride(resolveSeatPrice(schedule.getPrice(), seatTemplate.getSeatType()))
                        .build())
                .toList();

        scheduleSeatRepository.saveAll(scheduleSeats);
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
            throw new IllegalArgumentException("좌석을 1개 이상 선택해 주세요.");
        }
        if (normalizedSeatCodes.size() != expectedCount) {
            throw new IllegalArgumentException("선택한 좌석 수와 인원이 일치해야 합니다.");
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
            throw new IllegalArgumentException("선택한 좌석 수와 인원이 일치해야 합니다.");
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
        return resolveSeatPrice(scheduleSeat.getSchedule().getPrice(), scheduleSeat.getSeatTemplate().getSeatType());
    }

    public int resolveSeatPrice(int schedulePrice, SeatType seatType) {
        return switch (seatType) {
            case PREMIUM -> schedulePrice + PREMIUM_SURCHARGE;
            case COUPLE -> schedulePrice + COUPLE_SURCHARGE;
            case STANDARD -> schedulePrice;
        };
    }

    private List<SeatTemplate> buildSeatTemplates(Screen screen) {
        int totalSeats = screen.getTotalSeats() != null ? screen.getTotalSeats() : 0;
        int totalRows = (int) Math.ceil(totalSeats / (double) SEATS_PER_ROW);
        int remainingSeats = totalSeats;
        List<SeatTemplate> seatTemplates = new ArrayList<>();

        for (int rowIndex = 0; rowIndex < totalRows; rowIndex++) {
            int seatsInRow = Math.min(SEATS_PER_ROW, remainingSeats);
            char rowChar = (char) ('A' + rowIndex);

            for (int seatNumber = 1; seatNumber <= seatsInRow; seatNumber++) {
                seatTemplates.add(SeatTemplate.builder()
                        .screen(screen)
                        .seatRow(String.valueOf(rowChar))
                        .seatNumber(seatNumber)
                        .seatCode(rowChar + String.valueOf(seatNumber))
                        .seatType(resolveSeatType(rowIndex, totalRows, seatNumber, seatsInRow))
                        .active(true)
                        .build());
            }

            remainingSeats -= seatsInRow;
        }

        return seatTemplates;
    }

    private SeatType resolveSeatType(int rowIndex, int totalRows, int seatNumber, int seatsInRow) {
        if (rowIndex == 0 && seatNumber >= Math.max(2, (seatsInRow / 2) - 1) && seatNumber <= Math.min(seatsInRow, (seatsInRow / 2) + 2)) {
            return SeatType.PREMIUM;
        }
        if (rowIndex == totalRows - 1 && seatsInRow >= 8 && seatNumber >= 4 && seatNumber <= Math.min(seatsInRow, 9)) {
            return SeatType.COUPLE;
        }
        return SeatType.STANDARD;
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
