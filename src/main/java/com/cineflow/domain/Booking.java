package com.cineflow.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String bookingCode;

    private String customerName;
    private String customerPhone;
    private String movieTitle;
    private String posterUrl;
    private String ageRating;
    private String theaterName;
    private String screenName;
    private String screenType;
    private String seatNames;
    private Integer peopleCount;
    private Integer totalPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    @Transient
    public String getStateLabel() {
        return switch (status) {
            case BOOKED -> "예매완료";
            case UPCOMING -> "상영예정";
            case USED -> "관람완료";
            case CANCELED -> "취소완료";
            case SOON -> "곧 상영";
        };
    }

    @Transient
    public String getStateCssClass() {
        return switch (status) {
            case BOOKED -> "history-state-chip state-booked";
            case UPCOMING -> "history-state-chip state-upcoming";
            case USED -> "history-state-chip state-done";
            case CANCELED -> "history-state-chip state-cancel";
            case SOON -> "history-state-chip state-soon";
        };
    }

    @Transient
    public String getAgeBadgeCssClass() {
        return switch (ageRating) {
            case "15" -> "age-badge age-15";
            case "19" -> "age-badge age-19";
            case "12" -> "age-badge age-12";
            default -> "age-badge";
        };
    }
}
