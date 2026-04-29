package com.cineflow.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "bookings",
        indexes = {
                @Index(name = "idx_bookings_status", columnList = "status"),
                @Index(name = "idx_bookings_schedule_id", columnList = "schedule_id"),
                @Index(name = "idx_bookings_start_time", columnList = "start_time"),
                @Index(name = "idx_bookings_user_id", columnList = "user_id")
        }
)
public class Booking {

    private static final String DEFAULT_POSTER_URL = "/images/uploads/movie-single.jpg";
    private static final String DEFAULT_MOVIE_TITLE = "영화 정보 준비 중";
    private static final String DEFAULT_THEATER_NAME = "극장 정보 준비 중";
    private static final String DEFAULT_SCREEN_NAME = "상영관 정보 준비 중";
    private static final String DEFAULT_SCREEN_TYPE = "상영 형식 준비 중";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");
    private static final DateTimeFormatter DATE_TIME_WITH_DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd (E) HH:mm", Locale.KOREAN);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final NumberFormat WON_FORMATTER = NumberFormat.getIntegerInstance(Locale.KOREA);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String bookingCode;

    @Column(length = 100)
    private String customerName;

    @Column(length = 30)
    private String customerPhone;

    @Column(length = 200)
    private String movieTitle;

    @Column(length = 255)
    private String posterUrl;

    @Column(length = 20)
    private String ageRating;

    @Column(length = 100)
    private String theaterName;

    @Column(length = 50)
    private String screenName;

    @Column(length = 50)
    private String screenType;

    @Column(length = 255)
    private String seatNames;

    private Integer peopleCount;
    private Integer totalPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @Column(length = 500)
    private String cancelReason;

    private LocalDateTime canceledAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private Schedule schedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private BookingStatus status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder.Default
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookingSeat> bookingSeats = new ArrayList<>();

    @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private Payment payment;

    @Transient
    public boolean isCanceled() {
        return status == BookingStatus.CANCELED;
    }

    @Transient
    public boolean isPastShowTime() {
        return startTime != null && !startTime.isAfter(LocalDateTime.now());
    }

    @Transient
    public boolean isCancelable() {
        return status == BookingStatus.BOOKED && startTime != null && startTime.isAfter(LocalDateTime.now());
    }

    @Transient
    public String getStateLabel() {
        if (status == BookingStatus.CANCELED) {
            return "취소 완료";
        }
        if (status == BookingStatus.USED) {
            return "관람 완료";
        }
        if (isPastShowTime()) {
            return "상영 종료";
        }
        if (status == BookingStatus.SOON) {
            return "곧 상영";
        }
        if (status == BookingStatus.UPCOMING) {
            return "상영 예정";
        }
        return "예매 완료";
    }

    @Transient
    public String getStateCssClass() {
        if (status == BookingStatus.CANCELED) {
            return "history-state-chip state-cancel";
        }
        if (status == BookingStatus.USED || isPastShowTime()) {
            return "history-state-chip state-done";
        }
        if (status == BookingStatus.SOON) {
            return "history-state-chip state-soon";
        }
        if (status == BookingStatus.UPCOMING) {
            return "history-state-chip state-upcoming";
        }
        return "history-state-chip state-booked";
    }

    @Transient
    public String getCancelAvailabilityLabel() {
        if (isCanceled()) {
            return "취소 완료";
        }
        if (isCancelable()) {
            return "취소 가능";
        }
        return "취소 불가";
    }

    @Transient
    public String getCancelReasonOrDefault() {
        return cancelReason != null && !cancelReason.isBlank() ? cancelReason : "사유 없음";
    }

    @Transient
    public String getAgeBadgeCssClass() {
        if (ageRating == null || ageRating.isBlank()) {
            return "age-badge";
        }

        return switch (ageRating) {
            case "15" -> "age-badge age-15";
            case "19" -> "age-badge age-19";
            case "12" -> "age-badge age-12";
            case "ALL" -> "age-badge all";
            default -> "age-badge";
        };
    }

    @Transient
    public String getAgeBadgeText() {
        return ageRating != null && !ageRating.isBlank() ? ageRating : "?";
    }

    @Transient
    public String getPosterImageUrl() {
        return posterUrl != null && !posterUrl.isBlank() ? posterUrl : DEFAULT_POSTER_URL;
    }

    @Transient
    public String getPosterAltText() {
        return getMovieTitleText() + " 포스터";
    }

    @Transient
    public String getMovieTitleText() {
        return movieTitle != null && !movieTitle.isBlank() ? movieTitle : DEFAULT_MOVIE_TITLE;
    }

    @Transient
    public String getTheaterNameText() {
        return theaterName != null && !theaterName.isBlank() ? theaterName : DEFAULT_THEATER_NAME;
    }

    @Transient
    public String getScreenNameText() {
        return screenName != null && !screenName.isBlank() ? screenName : DEFAULT_SCREEN_NAME;
    }

    @Transient
    public String getScreenTypeText() {
        return screenType != null && !screenType.isBlank() ? screenType : DEFAULT_SCREEN_TYPE;
    }

    @Transient
    public String getScreenDisplayText() {
        return getScreenNameText() + " (" + getScreenTypeText() + ")";
    }

    @Transient
    public String getVenueDisplayText() {
        return getTheaterNameText() + " | " + getScreenDisplayText();
    }

    @Transient
    public String getSeatNamesText() {
        return seatNames != null && !seatNames.isBlank() ? seatNames : "좌석 정보 없음";
    }

    @Transient
    public String getPeopleCountText() {
        return Math.max(peopleCount != null ? peopleCount : 0, 0) + "명";
    }

    @Transient
    public String getTotalPriceText() {
        int safeTotalPrice = Math.max(totalPrice != null ? totalPrice : 0, 0);
        return WON_FORMATTER.format(safeTotalPrice) + "원";
    }

    @Transient
    public String getCustomerNameText() {
        return customerName != null && !customerName.isBlank() ? customerName : "예매자 정보 없음";
    }

    @Transient
    public String getCustomerPhoneText() {
        return customerPhone != null && !customerPhone.isBlank() ? customerPhone : "연락처 정보 없음";
    }

    @Transient
    public String getPaymentMethodLabel() {
        return payment != null ? payment.getMethodLabel() : "결제수단 정보 없음";
    }

    @Transient
    public String getPaymentStatusLabel() {
        return payment != null ? payment.getPaymentStatusLabel() : "결제 정보 없음";
    }

    @Transient
    public String getShowStartDateTimeText() {
        return startTime != null ? DATE_TIME_FORMATTER.format(startTime) : "상영 시간 미정";
    }

    @Transient
    public String getShowStartDateTimeWithDayText() {
        return startTime != null ? DATE_TIME_WITH_DAY_FORMATTER.format(startTime) : "상영 시간 미정";
    }

    @Transient
    public String getShowStartTimeText() {
        return startTime != null ? TIME_FORMATTER.format(startTime) : "--:--";
    }

    @Transient
    public String getShowEndTimeText() {
        return endTime != null ? TIME_FORMATTER.format(endTime) : "--:--";
    }

    @Transient
    public String getCreatedAtText() {
        return createdAt != null ? DATE_TIME_FORMATTER.format(createdAt) : "-";
    }

    @Transient
    public String getCanceledAtText() {
        return canceledAt != null ? DATE_TIME_FORMATTER.format(canceledAt) : "-";
    }
}
