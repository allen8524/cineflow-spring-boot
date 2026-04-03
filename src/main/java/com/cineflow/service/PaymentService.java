package com.cineflow.service;

import com.cineflow.domain.Booking;
import com.cineflow.domain.Payment;
import com.cineflow.domain.PaymentMethod;
import com.cineflow.domain.PaymentStatus;
import com.cineflow.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final DateTimeFormatter TRANSACTION_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final PaymentRepository paymentRepository;

    public Payment processSuccessfulPayment(Booking booking, PaymentMethod method, int amount) {
        Payment payment = Payment.builder()
                .booking(booking)
                .method(method != null ? method : PaymentMethod.CARD)
                .amount(amount)
                .paymentStatus(PaymentStatus.PAID)
                .paidAt(LocalDateTime.now())
                .transactionId(generateTransactionId())
                .build();

        return paymentRepository.save(payment);
    }

    public Payment cancelPayment(Booking booking) {
        Payment payment = booking != null ? booking.getPayment() : null;
        if (payment == null) {
            throw new IllegalStateException("결제 정보가 없어 취소를 진행할 수 없습니다.");
        }
        if (payment.getPaymentStatus() == PaymentStatus.CANCELED) {
            throw new IllegalStateException("이미 취소된 결제입니다.");
        }
        if (payment.getPaymentStatus() != PaymentStatus.PAID) {
            throw new IllegalStateException("결제 완료 상태의 예매만 취소할 수 있습니다.");
        }

        payment.setPaymentStatus(PaymentStatus.CANCELED);
        payment.setCanceledAt(LocalDateTime.now());
        payment.setCancelTransactionId(generateCancelTransactionId());
        return paymentRepository.save(payment);
    }

    public Optional<Payment> findByBookingId(Long bookingId) {
        if (bookingId == null) {
            return Optional.empty();
        }
        return paymentRepository.findByBookingId(bookingId);
    }

    private String generateTransactionId() {
        return "PAY-" + TRANSACTION_FORMATTER.format(LocalDateTime.now())
                + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
    }

    private String generateCancelTransactionId() {
        return "CAN-" + TRANSACTION_FORMATTER.format(LocalDateTime.now())
                + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
    }
}
