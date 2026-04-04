package com.cineflow.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "movies",
        indexes = {
                @Index(name = "idx_movies_status", columnList = "status"),
                @Index(name = "idx_movies_release_date", columnList = "release_date"),
                @Index(name = "idx_movies_active_booking_open", columnList = "active, booking_open")
        }
)
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 300)
    private String shortDescription;

    @Column(length = 2000)
    private String description;

    @Column(length = 100)
    private String genre;

    @Column(length = 20)
    private String ageRating;

    private Integer runningTime;

    @Column(length = 255)
    private String posterUrl;

    private Double bookingRate;
    private Double score;
    private LocalDate releaseDate;

    @Column(nullable = false)
    @Builder.Default
    private boolean bookingOpen = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private MovieStatus status;

    @Builder.Default
    @OneToMany(mappedBy = "movie")
    private List<Schedule> schedules = new ArrayList<>();

    @Transient
    public String getAgeBadgeCssClass() {
        return switch (ageRating) {
            case "15" -> "age-badge age-15";
            case "19" -> "age-badge age-19";
            case "ALL" -> "age-badge all";
            case "12" -> "age-badge age-12";
            default -> "age-badge";
        };
    }

    @Transient
    public String getStatusLabel() {
        return status == MovieStatus.NOW_SHOWING ? "현재 상영중" : "개봉 예정";
    }

    @Transient
    public String getStatusCssClass() {
        return status == MovieStatus.NOW_SHOWING ? "now-showing" : "upcoming";
    }

    @Transient
    public String getMetricName() {
        return status == MovieStatus.NOW_SHOWING ? "예매율" : "사전 관심도";
    }

    @Transient
    public String getScoreMetricName() {
        return status == MovieStatus.NOW_SHOWING ? "평점" : "기대지수";
    }

    @Transient
    public String getBookingOpenLabel() {
        return bookingOpen ? "예매 가능" : "예매 마감";
    }

    @Transient
    public String getActiveLabel() {
        return active ? "노출 중" : "비노출";
    }
}
