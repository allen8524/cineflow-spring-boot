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
        return startTime.toLocalDate();
    }

    public String getSeatNames() {
        return String.join(", ", seatCodes);
    }

    public String getPeopleLabel() {
        return "성인 " + adultCount + "명 / 청소년 " + teenCount + "명 / 우대 " + seniorCount + "명";
    }

    public String getPaymentMethodLabel() {
        return paymentMethod != null ? paymentMethod.getLabel() : PaymentMethod.CARD.getLabel();
    }
}
