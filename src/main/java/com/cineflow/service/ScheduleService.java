package com.cineflow.service;

import com.cineflow.domain.Movie;
import com.cineflow.domain.Schedule;
import com.cineflow.domain.Screen;
import com.cineflow.dto.AdminScheduleForm;
import com.cineflow.dto.ScheduleViewDto;
import com.cineflow.dto.TheaterScheduleGroupDto;
import com.cineflow.repository.BookingRepository;
import com.cineflow.repository.MovieRepository;
import com.cineflow.repository.ScheduleRepository;
import com.cineflow.repository.ScreenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
@Transactional(readOnly = true)
public class ScheduleService {

    private static final Comparator<ScheduleViewDto> SCHEDULE_COMPARATOR = Comparator
            .comparing(ScheduleViewDto::getShowDate)
            .thenComparing(ScheduleViewDto::getTheaterName, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(ScheduleViewDto::getStartTime);

    private final ScheduleRepository scheduleRepository;
    private final MovieRepository movieRepository;
    private final ScreenRepository screenRepository;
    private final BookingRepository bookingRepository;
    private final SeatService seatService;

    public List<ScheduleViewDto> getAllScheduleViews() {
        return sortAndMapPublic(scheduleRepository.findByActiveTrueOrderByStartTimeAsc());
    }

    public List<ScheduleViewDto> getSchedulesForMovie(Long movieId) {
        return sortAndMapPublic(scheduleRepository.findByMovieIdAndActiveTrueOrderByStartTimeAsc(movieId));
    }

    public List<ScheduleViewDto> getSchedulesForMovieAndDate(Long movieId, LocalDate date) {
        if (date == null) {
            return getSchedulesForMovie(movieId);
        }
        return sortAndMapPublic(scheduleRepository.findByMovieIdAndStartTimeBetweenAndActiveTrueOrderByStartTimeAsc(
                movieId,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay()
        ));
    }

    public List<ScheduleViewDto> getSchedulesForTheater(Long theaterId) {
        return sortAndMapPublic(scheduleRepository.findByScreenTheaterIdAndActiveTrueOrderByStartTimeAsc(theaterId));
    }

    public List<ScheduleViewDto> getSchedulesForMovieAndTheater(Long movieId, Long theaterId) {
        if (theaterId == null) {
            return getSchedulesForMovie(movieId);
        }
        return sortAndMapPublic(scheduleRepository.findByMovieIdAndScreenTheaterIdAndActiveTrueOrderByStartTimeAsc(movieId, theaterId));
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
        return sortAndMapPublic(scheduleRepository.findByMovieIdAndScreenTheaterIdAndStartTimeBetweenAndActiveTrueOrderByStartTimeAsc(
                movieId,
                theaterId,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay()
        ));
    }

    public List<ScheduleViewDto> getSchedulesForAdmin(Long movieId, Long theaterId, LocalDate date) {
        return sortAndMapAdmin(scheduleRepository.findAllByOrderByStartTimeAsc()).stream()
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
        return scheduleRepository.findByIdAndActiveTrue(scheduleId)
                .filter(this::isPublicVisible)
                .map(ScheduleViewDto::from);
    }

    public Optional<ScheduleViewDto> findAdminScheduleView(Long scheduleId) {
        if (scheduleId == null) {
            return Optional.empty();
        }
        return scheduleRepository.findById(scheduleId)
                .filter(Schedule::isActive)
                .map(ScheduleViewDto::from);
    }

    public Schedule getScheduleForAdmin(Long scheduleId) {
        return scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("회차를 찾을 수 없습니다. id=" + scheduleId));
    }

    public ScheduleViewDto getDefaultScheduleView() {
        return scheduleRepository.findByActiveTrueOrderByStartTimeAsc().stream()
                .filter(this::isPublicVisible)
                .findFirst()
                .map(ScheduleViewDto::from)
                .orElse(null);
    }

    @Transactional
    public Schedule createSchedule(AdminScheduleForm form) {
        Movie movie = movieRepository.findById(form.getMovieId())
                .filter(Movie::isActive)
                .orElseThrow(() -> new IllegalArgumentException("선택한 영화를 찾을 수 없습니다."));
        Screen screen = screenRepository.findById(form.getScreenId())
                .filter(Screen::isActive)
                .filter(candidate -> candidate.getTheater() != null && candidate.getTheater().isActive())
                .orElseThrow(() -> new IllegalArgumentException("선택한 상영관을 찾을 수 없습니다."));

        validateScheduleForm(form, null);

        long totalSeats = seatService.countActiveSeatTemplatesForScreen(screen.getId());
        if (totalSeats == 0) {
            throw new IllegalStateException("활성 좌석 템플릿이 없는 상영관에는 회차를 등록할 수 없습니다.");
        }

        Schedule schedule = scheduleRepository.save(Schedule.builder()
                .movie(movie)
                .screen(screen)
                .startTime(form.getStartTime())
                .endTime(form.getEndTime())
                .price(form.getPrice())
                .availableSeats((int) totalSeats)
                .active(true)
                .build());

        seatService.rebuildScheduleSeatsForSchedule(schedule);
        return scheduleRepository.save(schedule);
    }

    @Transactional
    public Schedule updateSchedule(Long id, AdminScheduleForm form) {
        Schedule schedule = getScheduleForAdmin(id);

        if (bookingRepository.existsByScheduleId(id)) {
            throw new IllegalStateException("이미 예매가 존재하는 회차는 수정할 수 없습니다.");
        }

        Movie movie = movieRepository.findById(form.getMovieId())
                .filter(Movie::isActive)
                .orElseThrow(() -> new IllegalArgumentException("선택한 영화를 찾을 수 없습니다."));
        Screen screen = screenRepository.findById(form.getScreenId())
                .filter(Screen::isActive)
                .filter(candidate -> candidate.getTheater() != null && candidate.getTheater().isActive())
                .orElseThrow(() -> new IllegalArgumentException("선택한 상영관을 찾을 수 없습니다."));

        validateScheduleForm(form, id);

        long totalSeats = seatService.countActiveSeatTemplatesForScreen(screen.getId());
        if (totalSeats == 0) {
            throw new IllegalStateException("활성 좌석 템플릿이 없는 상영관에는 회차를 등록할 수 없습니다.");
        }

        schedule.setMovie(movie);
        schedule.setScreen(screen);
        schedule.setStartTime(form.getStartTime());
        schedule.setEndTime(form.getEndTime());
        schedule.setPrice(form.getPrice());
        schedule.setAvailableSeats((int) totalSeats);
        schedule.setActive(true);

        Schedule savedSchedule = scheduleRepository.save(schedule);
        seatService.rebuildScheduleSeatsForSchedule(savedSchedule);
        return scheduleRepository.save(savedSchedule);
    }

    @Transactional
    public void deactivateSchedule(Long id) {
        Schedule schedule = getScheduleForAdmin(id);
        if (bookingRepository.existsByScheduleId(id)) {
            throw new IllegalStateException("이미 예매가 연결된 회차는 삭제할 수 없습니다. 필요하면 상영 종료 후 비활성 정책으로 관리해 주세요.");
        }
        schedule.setActive(false);
        scheduleRepository.save(schedule);
    }

    private void validateScheduleForm(AdminScheduleForm form, Long scheduleId) {
        if (form.getStartTime() == null || form.getEndTime() == null) {
            throw new IllegalArgumentException("상영 시작 시각과 종료 시각을 모두 입력해 주세요.");
        }
        if (!form.getEndTime().isAfter(form.getStartTime())) {
            throw new IllegalArgumentException("종료 시각은 시작 시각보다 늦어야 합니다.");
        }

        boolean overlapped = scheduleId == null
                ? scheduleRepository.existsByScreenIdAndActiveTrueAndStartTimeLessThanAndEndTimeGreaterThan(
                form.getScreenId(),
                form.getEndTime(),
                form.getStartTime()
        )
                : scheduleRepository.existsByScreenIdAndActiveTrueAndIdNotAndStartTimeLessThanAndEndTimeGreaterThan(
                form.getScreenId(),
                scheduleId,
                form.getEndTime(),
                form.getStartTime()
        );

        if (overlapped) {
            throw new IllegalStateException("같은 상영관에서 시간이 겹치는 회차는 등록할 수 없습니다.");
        }
    }

    private List<ScheduleViewDto> sortAndMapPublic(Collection<Schedule> schedules) {
        return schedules.stream()
                .filter(this::isPublicVisible)
                .map(ScheduleViewDto::from)
                .sorted(SCHEDULE_COMPARATOR)
                .toList();
    }

    private List<ScheduleViewDto> sortAndMapAdmin(Collection<Schedule> schedules) {
        return schedules.stream()
                .filter(Schedule::isActive)
                .map(ScheduleViewDto::from)
                .sorted(SCHEDULE_COMPARATOR)
                .toList();
    }

    private boolean isPublicVisible(Schedule schedule) {
        if (schedule == null || !schedule.isActive()) {
            return false;
        }
        if (schedule.getMovie() == null || !schedule.getMovie().isActive() || !schedule.getMovie().isBookingOpen()) {
            return false;
        }
        if (schedule.getScreen() == null || !schedule.getScreen().isActive()) {
            return false;
        }
        return schedule.getScreen().getTheater() != null && schedule.getScreen().getTheater().isActive();
    }
}
