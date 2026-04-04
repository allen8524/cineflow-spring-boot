package com.cineflow.domain;

public enum SeatType {
    STANDARD,
    PREMIUM,
    COUPLE;

    public String getLabel() {
        return switch (this) {
            case STANDARD -> "Standard";
            case PREMIUM -> "Premium";
            case COUPLE -> "Couple";
        };
    }
}
