package com.cineflow.service;

import com.cineflow.domain.Schedule;
import com.cineflow.dto.ScheduleViewDto;
import com.cineflow.dto.TheaterScheduleGroupDto;
import com.cineflow.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private static final Comparator<ScheduleViewDto> SCHEDULE_COMPARATOR = Comparator
            .comparing(ScheduleViewDto::getShowDate)
            .thenComparing(ScheduleViewDto::getTheaterName, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(ScheduleViewDto::getStartTime);

    private final ScheduleRepository scheduleRepository;

    public List<ScheduleViewDto> getAllScheduleViews() {
        return sortAndMap(scheduleRepository.findByActiveTrueOrderByStartTimeAsc());
    }

    public List<ScheduleViewDto> getSchedulesForMovie(Long movieId) {
        return sortAndMap(scheduleRepository.findByMovieIdAndActiveTrueOrderByStartTimeAsc(movieId));
    }

    public List<ScheduleViewDto> getSchedulesForMovieAndDate(Long movieId, LocalDate date) {
        if (date == null) {
            return getSchedulesForMovie(movieId);
        }
        return sortAndMap(scheduleRepository.findByMovieIdAndStartTimeBetweenAndActiveTrueOrderByStartTimeAsc(
                movieId,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay()
        ));
    }

    public List<ScheduleViewDto> getSchedulesForTheater(Long theaterId) {
        return sortAndMap(scheduleRepository.findByScreenTheaterIdAndActiveTrueOrderByStartTimeAsc(theaterId));
    }

    public List<ScheduleViewDto> getSchedulesForMovieAndTheater(Long movieId, Long theaterId) {
        if (theaterId == null) {
            return getSchedulesForMovie(movieId);
        }
        return sortAndMap(scheduleRepository.findByMovieIdAndScreenTheaterIdAndActiveTrueOrderByStartTimeAsc(movieId, theaterId));
    }

    public List<ScheduleViewDto> getSchedulesForMovieAndTheaterAndDate(Long movieId, Long theaterId, LocalDate date) {
        if (movieId == null) {
            return List.of();
        }
        if (theaterId == null && date == null) {
            return getSchedulesForMovie(movieId);
        }
        if (theaterId == null) {
            return getSchedulesForMovieAndDate(movieId, date);
        }
        if (date == null) {
            return getSchedulesForMovieAndTheater(movieId, theaterId);
        }
        return sortAndMap(scheduleRepository.findByMovieIdAndScreenTheaterIdAndStartTimeBetweenAndActiveTrueOrderByStartTimeAsc(
                movieId,
                theaterId,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay()
        ));
    }

    public List<ScheduleViewDto> getSchedulesForAdmin(Long movieId, Long theaterId, LocalDate date) {
        return getAllScheduleViews().stream()
                .filter(schedule -> movieId == null || schedule.getMovieId().equals(movieId))
                .filter(schedule -> theaterId == null || schedule.getTheaterId().equals(theaterId))
                .filter(schedule -> date == null || schedule.getShowDate().equals(date))
                .toList();
    }

    public List<LocalDate> getAvailableDates(Long movieId, Long theaterId) {
        return getSchedulesForMovieAndTheaterAndDate(movieId, theaterId, null).stream()
                .map(ScheduleViewDto::getShowDate)
                .distinct()
                .toList();
    }

    public List<TheaterScheduleGroupDto> getTheaterScheduleGroupsByMovie(Long movieId) {
        Map<Long, List<ScheduleViewDto>> groupedSchedules = getSchedulesForMovie(movieId).stream()
                .collect(Collectors.groupingBy(
                        ScheduleViewDto::getTheaterId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return groupedSchedules.values().stream()
                .map(TheaterScheduleGroupDto::from)
                .toList();
    }

    public Optional<ScheduleViewDto> findScheduleView(Long scheduleId) {
        if (scheduleId == null) {
            return Optional.empty();
        }
        return scheduleRepository.findByIdAndActiveTrue(scheduleId).map(ScheduleViewDto::from);
    }

    public ScheduleViewDto getDefaultScheduleView() {
        return scheduleRepository.findByActiveTrueOrderByStartTimeAsc().stream()
                .findFirst()
                .map(ScheduleViewDto::from)
                .orElse(null);
    }

    private List<ScheduleViewDto> sortAndMap(Collection<Schedule> schedules) {
        return schedules.stream()
                .map(ScheduleViewDto::from)
                .sorted(SCHEDULE_COMPARATOR)
                .toList();
    }
}
