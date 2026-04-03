package com.cineflow.domain;

public enum SeatType {
    STANDARD,
    PREMIUM,
    COUPLE;

    public String getLabel() {
        return switch (this) {
            case STANDARD -> "일반석";
            case PREMIUM -> "프리미엄석";
            case COUPLE -> "커플석";
        };
    }
}
