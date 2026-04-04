package com.cineflow.domain;

public enum PaymentMethod {
    CARD,
    KAKAO_PAY,
    NAVER_PAY,
    TOSS;

    public String getLabel() {
        return switch (this) {
            case CARD -> "Card";
            case KAKAO_PAY -> "Kakao Pay";
            case NAVER_PAY -> "Naver Pay";
            case TOSS -> "Toss";
        };
    }
}
