package com.cineflow.dto;

import com.cineflow.domain.MovieStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@Getter
@Builder
@AllArgsConstructor
public class PublicMovieMetadataDto {

    private static final DateTimeFormatter RELEASE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final String DEFAULT_GENRE_TEXT = "\uC7A5\uB974 \uC815\uBCF4 \uC900\uBE44 \uC911";
    private static final String DEFAULT_RELEASE_DATE_TEXT = "\uAC1C\uBD09\uC77C \uCD94\uD6C4 \uC548\uB0B4";
    private static final String DEFAULT_RUNNING_TIME_TEXT = "\uC0C1\uC601\uC2DC\uAC04 \uCD94\uD6C4 \uC548\uB0B4";
    private static final String DEFAULT_AGE_RATING_TEXT = "\uAD00\uB78C\uB4F1\uAE09 \uCD94\uD6C4 \uC548\uB0B4";
    private static final String DEFAULT_METRIC_TEXT = "\uC9D1\uACC4\uC911";
    private static final String DEFAULT_TITLE_TEXT = "\uC601\uD654 \uC815\uBCF4 \uC900\uBE44 \uC911";
    private static final String DEFAULT_DESCRIPTION_TEXT = "\uD604\uC7AC \uC601\uD654 \uC18C\uAC1C\uB97C \uBD88\uB7EC\uC624\uB294 \uC911\uC785\uB2C8\uB2E4. \uC7A0\uC2DC \uD6C4 \uB2E4\uC2DC \uD655\uC778\uD574 \uC8FC\uC138\uC694.";
    private static final String DEFAULT_SHORT_DESCRIPTION_TEXT = "\uC601\uD654 \uC18C\uAC1C\uB97C \uC900\uBE44 \uC911\uC785\uB2C8\uB2E4.";
    private static final String DEFAULT_POSTER_URL = "/images/uploads/movie-single.jpg";
    private static final String DEFAULT_BACKDROP_URL = "/images/uploads/slider-bg.jpg";
    private static final int HERO_COPY_LIMIT = 220;
    private static final int OVERVIEW_SNIPPET_LIMIT = 150;

    private final Long localMovieId;
    private final Long tmdbId;
    private final String title;
    private final String originalTitle;
    private final String overview;
    private final LocalDate releaseDate;
    private final Integer runtimeMinutes;
    private final List<String> genres;
    private final String posterUrl;
    private final String backdropUrl;
    private final String ageRating;
    private final boolean bookingOpen;
    private final boolean active;
    private final MovieStatus status;
    private final Double bookingRate;
    private final Double score;
    private final String shortDescription;
    private final boolean liveMetadata;

    public Long getId() {
        return localMovieId != null ? localMovieId : tmdbId;
    }

    public Long getDetailRouteId() {
        return getId();
    }

    public Long getBookingMovieId() {
        return localMovieId;
    }

    public boolean hasLocalMovieLink() {
        return localMovieId != null;
    }

    public boolean hasBookingLink() {
        return localMovieId != null && active;
    }

    public boolean isBookable() {
        return hasBookingLink() && bookingOpen;
    }

    public String getDescription() {
        return overview;
    }

    public String getTitleText() {
        return firstNonBlank(title, DEFAULT_TITLE_TEXT);
    }

    public String getDescriptionText() {
        return firstNonBlank(overview, shortDescription, DEFAULT_DESCRIPTION_TEXT);
    }

    public String getShortDescriptionText() {
        return firstNonBlank(shortDescription, overview, DEFAULT_SHORT_DESCRIPTION_TEXT);
    }

    public String getHeroCopyText() {
        return firstNonBlank(getHeroCopy(), DEFAULT_SHORT_DESCRIPTION_TEXT);
    }

    public String getOverviewSnippetText() {
        return firstNonBlank(getOverviewSnippet(), DEFAULT_SHORT_DESCRIPTION_TEXT);
    }

    public String getPosterImageUrl() {
        return firstNonBlank(posterUrl, DEFAULT_POSTER_URL);
    }

    public String getBackdropImageUrl() {
        return firstNonBlank(backdropUrl, DEFAULT_BACKDROP_URL);
    }

    public String getPosterAltText() {
        return getTitleText() + " 포스터";
    }

    public String getHeroCopy() {
        return abbreviate(firstNonBlank(overview, shortDescription), HERO_COPY_LIMIT);
    }

    public String getOverviewSnippet() {
        return abbreviate(firstNonBlank(shortDescription, overview), OVERVIEW_SNIPPET_LIMIT);
    }

    public boolean isOriginalTitleVisible() {
        String normalizedOriginalTitle = trimToNull(originalTitle);
        String normalizedTitle = trimToNull(title);

        if (normalizedOriginalTitle == null) {
            return false;
        }
        if (normalizedTitle == null) {
            return true;
        }

        return !normalizedOriginalTitle.equalsIgnoreCase(normalizedTitle);
    }

    public String getGenreText() {
        if (genres == null || genres.isEmpty()) {
            return DEFAULT_GENRE_TEXT;
        }

        String joinedGenres = genres.stream()
                .map(this::trimToNull)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.joining(" \u00B7 "));

        return joinedGenres.isBlank() ? DEFAULT_GENRE_TEXT : joinedGenres;
    }

    public String getReleaseDateText() {
        return releaseDate != null ? "\uAC1C\uBD09 " + RELEASE_DATE_FORMAT.format(releaseDate) : DEFAULT_RELEASE_DATE_TEXT;
    }

    public String getReleaseDateValueText() {
        return releaseDate != null ? RELEASE_DATE_FORMAT.format(releaseDate) : DEFAULT_RELEASE_DATE_TEXT;
    }

    public String getRunningTimeText() {
        return runtimeMinutes != null ? runtimeMinutes + "\uBD84" : DEFAULT_RUNNING_TIME_TEXT;
    }

    public String getAgeRatingText() {
        return ageRating != null ? ageRating + "\uC138 \uC774\uC0C1 \uAD00\uB78C\uAC00" : DEFAULT_AGE_RATING_TEXT;
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
        if (status == MovieStatus.NOW_SHOWING) {
            return "\uD604\uC7AC \uC0C1\uC601\uC911";
        }
        if (status == MovieStatus.COMING_SOON) {
            return "\uAC1C\uBD09 \uC608\uC815";
        }
        return "\uC0C1\uC601 \uC815\uBCF4 \uC5C5\uB370\uC774\uD2B8 \uC608\uC815";
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
        if (!hasBookingLink()) {
            return "예매 정보 준비 중";
        }
        return bookingOpen ? "\uC608\uB9E4 \uAC00\uB2A5" : "\uC608\uB9E4 \uC624\uD508 \uC608\uC815";
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

    private String abbreviate(String value, int maxLength) {
        String normalized = trimToNull(value);
        if (normalized == null || normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }

    private String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            String normalized = trimToNull(candidate);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
