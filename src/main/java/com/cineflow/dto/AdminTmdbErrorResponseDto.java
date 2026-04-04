package com.cineflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AdminTmdbErrorResponseDto {

    private final int status;
    private final String message;
}
