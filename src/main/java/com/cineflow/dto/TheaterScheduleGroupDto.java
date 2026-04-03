package com.cineflow.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TheaterScheduleGroupDto {

    private Long theaterId;
    private String theaterName;
    private String theaterLocation;
    private String theaterRegion;
    private String theaterDescription;
    private List<ScheduleViewDto> schedules;

    public static TheaterScheduleGroupDto from(List<ScheduleViewDto> schedules) {
        ScheduleViewDto firstSchedule = schedules.get(0);
        return TheaterScheduleGroupDto.builder()
                .theaterId(firstSchedule.getTheaterId())
                .theaterName(firstSchedule.getTheaterName())
                .theaterLocation(firstSchedule.getTheaterLocation())
                .theaterRegion(firstSchedule.getTheaterRegion())
                .theaterDescription(firstSchedule.getTheaterDescription())
                .schedules(schedules)
                .build();
    }
}
