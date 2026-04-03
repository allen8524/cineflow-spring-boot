package com.cineflow.dto;

import com.cineflow.domain.PaymentMethod;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class BookingRequestDto {

    private Long scheduleId;
    private List<String> seatCodes = new ArrayList<>();
    private Integer adultCount;
    private Integer teenCount;
    private Integer seniorCount;
    private String customerName;
    private String customerPhone;
    private PaymentMethod paymentMethod;

    public int getPeopleCount() {
        return safeCount(adultCount) + safeCount(teenCount) + safeCount(seniorCount);
    }

    private int safeCount(Integer count) {
        return count == null ? 0 : Math.max(count, 0);
    }
}
