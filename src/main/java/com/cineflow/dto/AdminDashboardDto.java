package com.cineflow.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminDashboardDto {

    private long totalMovies;
    private long totalSchedules;
    private long totalBookings;
    private long upcomingBookings;
    private long canceledBookings;
    private long totalRevenue;
    private long todaySchedules;
    private long todayBookings;
}
