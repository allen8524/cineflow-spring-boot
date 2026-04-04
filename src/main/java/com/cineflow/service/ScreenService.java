package com.cineflow.service;

import com.cineflow.domain.Screen;
import com.cineflow.domain.Theater;
import com.cineflow.dto.AdminScreenForm;
import com.cineflow.repository.ScheduleRepository;
import com.cineflow.repository.ScreenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScreenService {

    private final ScreenRepository screenRepository;
    private final TheaterService theaterService;
    private final ScheduleRepository scheduleRepository;
    private final SeatService seatService;

    public List<Screen> getAllScreensForAdmin() {
        return screenRepository.findAllWithTheaterOrderByTheaterNameAscNameAsc();
    }

    public List<Screen> getActiveScreensForSchedule() {
        return screenRepository.findAllActiveWithTheaterOrderByTheaterNameAscNameAsc();
    }

    public Screen getScreenForAdmin(Long id) {
        return screenRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("상영관을 찾을 수 없습니다. id=" + id));
    }

    @Transactional
    public Screen createScreen(AdminScreenForm form) {
        Theater theater = theaterService.getTheaterForAdmin(form.getTheaterId());
        Screen screen = Screen.builder().build();
        applyForm(screen, form, theater);
        Screen savedScreen = screenRepository.save(screen);
        seatService.syncSeatTemplatesForScreen(savedScreen);
        return savedScreen;
    }

    @Transactional
    public Screen updateScreen(Long id, AdminScreenForm form) {
        Screen screen = getScreenForAdmin(id);
        Theater theater = theaterService.getTheaterForAdmin(form.getTheaterId());
        boolean schedulesExist = scheduleRepository.existsByScreenId(screen.getId());

        if (schedulesExist && !screen.getTheater().getId().equals(form.getTheaterId())) {
            throw new IllegalStateException("이미 회차가 연결된 상영관은 소속 극장을 변경할 수 없습니다.");
        }
        if (schedulesExist && !screen.getTotalSeats().equals(form.getTotalSeats())) {
            throw new IllegalStateException("이미 회차가 연결된 상영관은 총 좌석 수를 변경할 수 없습니다.");
        }

        applyForm(screen, form, theater);
        Screen savedScreen = screenRepository.save(screen);

        if (!schedulesExist) {
            seatService.syncSeatTemplatesForScreen(savedScreen);
        }

        return savedScreen;
    }

    @Transactional
    public void deactivateScreen(Long id) {
        Screen screen = getScreenForAdmin(id);
        screen.setActive(false);
        screenRepository.save(screen);
    }

    private void applyForm(Screen screen, AdminScreenForm form, Theater theater) {
        screen.setTheater(theater);
        screen.setName(trimToNull(form.getName()));
        screen.setScreenType(trimToNull(form.getScreenType()));
        screen.setTotalSeats(form.getTotalSeats());
        screen.setActive(form.isActive());
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
