package com.cineflow.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SeatRowDto {

    private String rowLabel;
    private List<SeatViewDto> seats;
}
