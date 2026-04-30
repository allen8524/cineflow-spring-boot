package com.cineflow.dto;

import com.cineflow.domain.Schedule;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Setter
public class AdminScheduleForm {

    @NotNull(message = "Please select a movie.")
    private Long movieId;

    @NotNull(message = "Please select a theater.")
    private Long theaterId;

    @NotNull(message = "Please select a screen.")
    private Long screenId;

    @NotNull(message = "Please select a start time.")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime startTime;

    @NotNull(message = "Please select an end time.")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime endTime;

    @NotNull(message = "Please enter a price.")
    @Min(value = 0, message = "Price must be zero or greater.")
    private Integer price;

    public static AdminScheduleForm from(Schedule schedule) {
        AdminScheduleForm form = new AdminScheduleForm();
        form.setMovieId(schedule.getMovie().getId());
        form.setTheaterId(schedule.getScreen().getTheater().getId());
        form.setScreenId(schedule.getScreen().getId());
        form.setStartTime(schedule.getStartTime());
        form.setEndTime(schedule.getEndTime());
        form.setPrice(schedule.getPrice());
        return form;
    }
}
