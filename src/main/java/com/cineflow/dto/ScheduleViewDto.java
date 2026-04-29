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

    private static final String DEFAULT_POSTER_URL = "/images/uploads/movie-single.jpg";
    private static final String DEFAULT_MOVIE_TITLE = "영화 정보 준비 중";
    private static final String DEFAULT_SCREEN_NAME = "상영관 정보 준비 중";
    private static final String DEFAULT_SCREEN_TYPE = "상영 형식 준비 중";

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
        return startTime != null ? startTime.toLocalDate() : null;
    }

    public int getRunningTime() {
        if (startTime == null || endTime == null) {
            return 0;
        }
        return Math.toIntExact(Duration.between(startTime, endTime).toMinutes());
    }

    public String getScreenDisplayName() {
        return getScreenNameText() + "(" + getScreenTypeText() + ")";
    }

    public String getScreenSummary() {
        return getScreenNameText() + " | " + getScreenTypeText();
    }

    public String getSeatAvailabilityLabel() {
        if (availableSeats == null) {
            return "확인 중";
        }
        if (availableSeats <= 20) {
            return "매진 임박";
        }
        if (availableSeats <= 50) {
            return "여석 적음";
        }
        return "예매 가능";
    }

    public String getPosterImageUrl() {
        return posterUrl != null && !posterUrl.isBlank() ? posterUrl : DEFAULT_POSTER_URL;
    }

    public String getPosterAltText() {
        return getMovieTitleText() + " 포스터";
    }

    public String getMovieTitleText() {
        return movieTitle != null && !movieTitle.isBlank() ? movieTitle : DEFAULT_MOVIE_TITLE;
    }

    public String getScreenNameText() {
        return screenName != null && !screenName.isBlank() ? screenName : DEFAULT_SCREEN_NAME;
    }

    public String getScreenTypeText() {
        return screenType != null && !screenType.isBlank() ? screenType : DEFAULT_SCREEN_TYPE;
    }

    public String getAgeBadgeCssClass() {
        if (ageRating == null || ageRating.isBlank()) {
            return "age-badge";
        }

        return switch (ageRating) {
            case "15" -> "age-badge age-15";
            case "19" -> "age-badge age-19";
            case "ALL" -> "age-badge all";
            case "12" -> "age-badge age-12";
            default -> "age-badge";
        };
    }

    public String getAgeBadgeText() {
        return ageRating != null && !ageRating.isBlank() ? ageRating : "?";
    }
}
