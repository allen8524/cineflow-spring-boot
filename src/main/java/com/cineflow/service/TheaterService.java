package com.cineflow.service;

import com.cineflow.domain.Theater;
import com.cineflow.dto.AdminTheaterForm;
import com.cineflow.repository.TheaterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TheaterService {

    private final TheaterRepository theaterRepository;

    public List<Theater> getAllTheaters() {
        return theaterRepository.findAllByActiveTrueOrderByRegionAscNameAsc();
    }

    public List<Theater> getAllTheatersForAdmin() {
        return theaterRepository.findAllByOrderByRegionAscNameAsc();
    }

    public List<Theater> getTheatersForMovie(Long movieId) {
        if (movieId == null) {
            return getAllTheaters();
        }
        return theaterRepository.findDistinctTheatersByMovieId(movieId);
    }

    public Theater getTheaterOrNull(Long theaterId) {
        if (theaterId == null) {
            return null;
        }
        return theaterRepository.findById(theaterId)
                .filter(Theater::isActive)
                .orElse(null);
    }

    public Theater getTheaterForAdmin(Long theaterId) {
        return theaterRepository.findById(theaterId)
                .orElseThrow(() -> new IllegalArgumentException("극장을 찾을 수 없습니다. id=" + theaterId));
    }

    @Transactional
    public Theater createTheater(AdminTheaterForm form) {
        Theater theater = Theater.builder().build();
        applyForm(theater, form);
        return theaterRepository.save(theater);
    }

    @Transactional
    public Theater updateTheater(Long id, AdminTheaterForm form) {
        Theater theater = getTheaterForAdmin(id);
        applyForm(theater, form);
        return theaterRepository.save(theater);
    }

    @Transactional
    public void deactivateTheater(Long id) {
        Theater theater = getTheaterForAdmin(id);
        theater.setActive(false);
        theaterRepository.save(theater);
    }

    private void applyForm(Theater theater, AdminTheaterForm form) {
        theater.setName(trimToNull(form.getName()));
        theater.setLocation(trimToNull(form.getLocation()));
        theater.setRegion(trimToNull(form.getRegion()));
        theater.setDescription(trimToNull(form.getDescription()));
        theater.setActive(form.isActive());
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
