package com.cineflow.dto;

import com.cineflow.domain.MovieStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Getter
@Builder
@AllArgsConstructor
public class MovieViewDto {

    private static final DateTimeFormatter RELEASE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final String DEFAULT_GENRE_TEXT = "\uC7A5\uB974 \uC815\uBCF4 \uC5C5\uB370\uC774\uD2B8 \uC608\uC815";
    private static final String DEFAULT_RELEASE_DATE_TEXT = "\uAC1C\uBD09\uC77C \uC5C5\uB370\uC774\uD2B8 \uC608\uC815";
    private static final String DEFAULT_RUNNING_TIME_TEXT = "\uC0C1\uC601\uC2DC\uAC04 \uC5C5\uB370\uC774\uD2B8 \uC608\uC815";
    private static final String DEFAULT_AGE_RATING_TEXT = "\uAD00\uB78C\uB4F1\uAE09 \uC815\uBCF4 \uC5C5\uB370\uC774\uD2B8 \uC608\uC815";
    private static final String DEFAULT_METRIC_TEXT = "\uC9D1\uACC4\uC911";
    private static final String DEFAULT_TITLE_TEXT = "\uC601\uD654 \uC815\uBCF4 \uC900\uBE44 \uC911";
    private static final String DEFAULT_POSTER_URL = "/images/uploads/movie-single.jpg";

    private final Long id;
    private final String title;
    private final String shortDescription;
    private final String description;
    private final String overviewSnippet;
    private final String genre;
    private final String ageRating;
    private final Integer runningTime;
    private final String posterUrl;
    private final String backdropUrl;
    private final Double bookingRate;
    private final Double score;
    private final LocalDate releaseDate;
    private final MovieStatus status;
    private final boolean bookingOpen;
    private final boolean active;

    public String getTitleText() {
        return title != null && !title.isBlank() ? title : DEFAULT_TITLE_TEXT;
    }

    public String getPosterImageUrl() {
        return posterUrl != null && !posterUrl.isBlank() ? posterUrl : DEFAULT_POSTER_URL;
    }

    public String getPosterAltText() {
        return getTitleText() + " 포스터";
    }

    public String getAgeBadgeCssClass() {
        if (ageRating == null) {
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
        return ageRating != null ? ageRating : "?";
    }

    public String getStatusLabel() {
        return status == MovieStatus.NOW_SHOWING ? "\uD604\uC7AC \uC0C1\uC601\uC911" : "\uAC1C\uBD09 \uC608\uC815";
    }

    public String getStatusCssClass() {
        return status == MovieStatus.NOW_SHOWING ? "now-showing" : "upcoming";
    }

    public String getMetricName() {
        return status == MovieStatus.NOW_SHOWING ? "\uC608\uB9E4\uC728" : "\uC0AC\uC804 \uAD00\uC2EC\uB3C4";
    }

    public String getScoreMetricName() {
        return status == MovieStatus.NOW_SHOWING ? "\uD3C9\uC810" : "\uAE30\uB300 \uC9C0\uC218";
    }

    public String getBookingOpenLabel() {
        return bookingOpen ? "\uC608\uB9E4 \uAC00\uB2A5" : "\uC608\uB9E4 \uC624\uD508 \uC608\uC815";
    }

    public String getGenreText() {
        return genre != null ? genre : DEFAULT_GENRE_TEXT;
    }

    public String getReleaseDateText() {
        return releaseDate != null ? "\uAC1C\uBD09 " + RELEASE_DATE_FORMAT.format(releaseDate) : DEFAULT_RELEASE_DATE_TEXT;
    }

    public String getReleaseDateValueText() {
        return releaseDate != null ? RELEASE_DATE_FORMAT.format(releaseDate) : DEFAULT_RELEASE_DATE_TEXT;
    }

    public String getRunningTimeText() {
        return runningTime != null ? runningTime + "\uBD84" : DEFAULT_RUNNING_TIME_TEXT;
    }

    public String getAgeRatingText() {
        return ageRating != null ? ageRating + "\uC138 \uC774\uC0C1 \uAD00\uB78C\uAC00" : DEFAULT_AGE_RATING_TEXT;
    }

    public String getBookingRateText() {
        return formatMetric(bookingRate, true);
    }

    public String getScoreText() {
        return formatMetric(score, false);
    }

    public boolean isComingSoon() {
        return status == MovieStatus.COMING_SOON;
    }

    public boolean isNowShowing() {
        return status == MovieStatus.NOW_SHOWING;
    }

    private String formatMetric(Double value, boolean percent) {
        if (value == null) {
            return DEFAULT_METRIC_TEXT;
        }

        String formatted = String.format(Locale.ROOT, "%.1f", value);
        return percent ? formatted + "%" : formatted;
    }
}
