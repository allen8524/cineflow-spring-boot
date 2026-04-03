package com.cineflow.dto;

import com.cineflow.domain.Booking;
import com.cineflow.domain.Payment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PaymentResultDto {

    private String bookingCode;
    private String paymentMethodLabel;
    private String paymentStatusLabel;
    private Integer amount;
    private LocalDateTime paidAt;
    private LocalDateTime canceledAt;
    private String transactionId;
    private String cancelTransactionId;

    public static PaymentResultDto from(Booking booking, Payment payment) {
        return PaymentResultDto.builder()
                .bookingCode(booking.getBookingCode())
                .paymentMethodLabel(payment != null ? payment.getMethodLabel() : "결제수단 확인 중")
                .paymentStatusLabel(payment != null ? payment.getPaymentStatusLabel() : "결제정보 없음")
                .amount(payment != null ? payment.getAmount() : booking.getTotalPrice())
                .paidAt(payment != null ? payment.getPaidAt() : booking.getCreatedAt())
                .canceledAt(payment != null ? payment.getCanceledAt() : null)
                .transactionId(payment != null ? payment.getTransactionId() : "-")
                .cancelTransactionId(payment != null && payment.getCancelTransactionId() != null ? payment.getCancelTransactionId() : "-")
                .build();
    }
}
