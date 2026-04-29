package com.cineflow.dto;

import com.cineflow.domain.PaymentMethod;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class BookingSummaryDto {

    private static final String DEFAULT_POSTER_URL = "/images/uploads/movie-single.jpg";
    private static final String DEFAULT_MOVIE_TITLE = "영화 정보 준비 중";
    private static final String DEFAULT_SCREEN_NAME = "상영관 정보 준비 중";
    private static final String DEFAULT_SCREEN_TYPE = "상영 형식 준비 중";

    private Long scheduleId;
    private Long movieId;
    private Long theaterId;
    private String movieTitle;
    private String posterUrl;
    private String ageRating;
    private String theaterName;
    private String screenName;
    private String screenType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<String> seatCodes;
    private Integer adultCount;
    private Integer teenCount;
    private Integer seniorCount;
    private Integer peopleCount;
    private Integer totalPrice;
    private Integer basePrice;
    private Integer availableSeats;
    private String customerName;
    private String customerPhone;
    private PaymentMethod paymentMethod;

    public LocalDate getShowDate() {
        return startTime != null ? startTime.toLocalDate() : null;
    }

    public String getSeatNames() {
        return seatCodes != null && !seatCodes.isEmpty() ? String.join(", ", seatCodes) : "선택 없음";
    }

    public String getPeopleLabel() {
        return "성인 " + safeCount(adultCount) + "명 / 청소년 " + safeCount(teenCount) + "명 / 우대 " + safeCount(seniorCount) + "명";
    }

    public String getPaymentMethodLabel() {
        return paymentMethod != null ? paymentMethod.getLabel() : PaymentMethod.CARD.getLabel();
    }

    public String getMovieTitleText() {
        return movieTitle != null && !movieTitle.isBlank() ? movieTitle : DEFAULT_MOVIE_TITLE;
    }

    public String getPosterImageUrl() {
        return posterUrl != null && !posterUrl.isBlank() ? posterUrl : DEFAULT_POSTER_URL;
    }

    public String getPosterAltText() {
        return getMovieTitleText() + " 포스터";
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

    private int safeCount(Integer count) {
        return count == null ? 0 : Math.max(count, 0);
    }
}
