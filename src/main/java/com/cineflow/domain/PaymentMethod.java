package com.cineflow.domain;

public enum PaymentMethod {
    CARD,
    KAKAO_PAY,
    NAVER_PAY,
    TOSS;

    public String getLabel() {
        return switch (this) {
            case CARD -> "카드";
            case KAKAO_PAY -> "카카오페이";
            case NAVER_PAY -> "네이버페이";
            case TOSS -> "토스";
        };
    }
}
