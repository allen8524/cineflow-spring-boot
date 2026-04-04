package com.cineflow.dto;

import com.cineflow.domain.Schedule;
import lombok.Builder;
import lombok.Getter;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class ScheduleViewDto {

    private Long scheduleId;
    private Long movieId;
    private String movieTitle;
    private String posterUrl;
    private String ageRating;
    private Long theaterId;
    private String theaterName;
    private String theaterLocation;
    private String theaterRegion;
    private String theaterDescription;
    private String screenName;
    private String screenType;
    private Integer totalSeats;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer price;
    private Integer availableSeats;

    public static ScheduleViewDto from(Schedule schedule) {
        return ScheduleViewDto.builder()
                .scheduleId(schedule.getId())
                .movieId(schedule.getMovie().getId())
                .movieTitle(schedule.getMovie().getTitle())
                .posterUrl(schedule.getMovie().getPosterUrl())
                .ageRating(schedule.getMovie().getAgeRating())
                .theaterId(schedule.getScreen().getTheater().getId())
                .theaterName(schedule.getScreen().getTheater().getName())
                .theaterLocation(schedule.getScreen().getTheater().getLocation())
                .theaterRegion(schedule.getScreen().getTheater().getRegion())
                .theaterDescription(schedule.getScreen().getTheater().getDescription())
                .screenName(schedule.getScreen().getName())
                .screenType(schedule.getScreen().getScreenType())
                .totalSeats(schedule.getScreen().getTotalSeats())
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .price(schedule.getPrice())
                .availableSeats(schedule.getAvailableSeats())
                .build();
    }

    public LocalDate getShowDate() {
        return startTime.toLocalDate();
    }

    public int getRunningTime() {
        return Math.toIntExact(Duration.between(startTime, endTime).toMinutes());
    }

    public String getScreenDisplayName() {
        return screenName + "(" + screenType + ")";
    }

    public String getScreenSummary() {
        return screenName + " | " + screenType;
    }

    public String getSeatAvailabilityLabel() {
        if (availableSeats == null) {
            return "Checking";
        }
        if (availableSeats <= 20) {
            return "Selling Fast";
        }
        if (availableSeats <= 50) {
            return "Limited";
        }
        return "Available";
    }
}
