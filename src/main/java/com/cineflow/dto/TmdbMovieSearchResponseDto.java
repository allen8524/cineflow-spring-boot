package com.cineflow.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbMovieSearchResponseDto {

    private List<TmdbMovieSummaryDto> results = new ArrayList<>();
}
