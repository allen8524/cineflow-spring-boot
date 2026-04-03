package com.cineflow.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "movies")
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 300)
    private String shortDescription;

    @Column(length = 2000)
    private String description;

    private String genre;
    private String ageRating;
    private Integer runningTime;
    private String posterUrl;
    private Double bookingRate;
    private Double score;
    private LocalDate releaseDate;

    @Enumerated(EnumType.STRING)
    private MovieStatus status;

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
        return status == MovieStatus.NOW_SHOWING ? "상영중" : "개봉예정";
    }

    @Transient
    public String getStatusCssClass() {
        return status == MovieStatus.NOW_SHOWING ? "now-showing" : "upcoming";
    }

    @Transient
    public String getMetricName() {
        return status == MovieStatus.NOW_SHOWING ? "예매율" : "사전예매율";
    }

    @Transient
    public String getScoreMetricName() {
        return status == MovieStatus.NOW_SHOWING ? "평점" : "기대지수";
    }
}
