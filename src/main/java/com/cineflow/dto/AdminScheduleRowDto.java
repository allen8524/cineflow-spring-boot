package com.cineflow.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminScheduleRowDto {

    private Long scheduleId;
    private Long movieId;
    private Long theaterId;
    private String movieTitle;
    private String theaterName;
    private String screenName;
    private String screenType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer totalSeats;
    private Integer availableSeats;
    private Integer reservedSeats;
    private Integer occupancyRate;

    public static AdminScheduleRowDto from(ScheduleViewDto schedule) {
        int totalSeats = schedule.getTotalSeats() != null ? schedule.getTotalSeats() : 0;
        int availableSeats = schedule.getAvailableSeats() != null ? schedule.getAvailableSeats() : 0;
        int reservedSeats = Math.max(totalSeats - availableSeats, 0);
        int occupancyRate = totalSeats > 0 ? (int) Math.round((reservedSeats * 100.0) / totalSeats) : 0;

        return AdminScheduleRowDto.builder()
                .scheduleId(schedule.getScheduleId())
                .movieId(schedule.getMovieId())
                .theaterId(schedule.getTheaterId())
                .movieTitle(schedule.getMovieTitle())
                .theaterName(schedule.getTheaterName())
                .screenName(schedule.getScreenName())
                .screenType(schedule.getScreenType())
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .totalSeats(totalSeats)
                .availableSeats(availableSeats)
                .reservedSeats(reservedSeats)
                .occupancyRate(occupancyRate)
                .build();
    }
}
