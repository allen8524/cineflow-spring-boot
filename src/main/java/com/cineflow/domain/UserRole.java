package com.cineflow.domain;

public enum UserRole {
    USER,
    ADMIN;

    public String getAuthority() {
        return "ROLE_" + name();
    }
}
