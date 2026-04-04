package com.cineflow.domain;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
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
@Table(
        name = "payments",
        indexes = {
                @Index(name = "idx_payments_status", columnList = "payment_status")
        }
)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method;

    @Column(nullable = false)
    private Integer amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus paymentStatus;

    private LocalDateTime paidAt;
    private LocalDateTime canceledAt;

    @Column(nullable = false, unique = true, length = 60)
    private String transactionId;

    @Column(length = 60)
    private String cancelTransactionId;

    public String getMethodLabel() {
        return method != null ? method.getLabel() : "-";
    }

    @Transient
    public String getPaymentStatusLabel() {
        return switch (paymentStatus) {
            case READY -> "Ready";
            case PAID -> "Paid";
            case CANCELED -> "Refunded";
            case FAILED -> "Failed";
        };
    }

    @Transient
    public String getPaymentStatusCssClass() {
        return switch (paymentStatus) {
            case READY -> "history-state-chip state-upcoming";
            case PAID -> "history-state-chip state-booked";
            case CANCELED -> "history-state-chip state-refunded";
            case FAILED -> "history-state-chip state-cancel";
        };
    }
}
