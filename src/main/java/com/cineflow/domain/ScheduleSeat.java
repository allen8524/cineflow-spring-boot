package com.cineflow.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
        name = "schedule_seats",
        uniqueConstraints = @UniqueConstraint(columnNames = {"schedule_id", "seat_template_id"}),
        indexes = {
                @Index(name = "idx_schedule_seats_schedule_id", columnList = "schedule_id"),
                @Index(name = "idx_schedule_seats_schedule_reserved", columnList = "schedule_id,reserved")
        }
)
public class ScheduleSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_template_id", nullable = false)
    private SeatTemplate seatTemplate;

    @Column(nullable = false)
    private boolean reserved;

    @Column(nullable = false)
    private boolean held;

    private LocalDateTime holdExpiresAt;

    private Integer priceOverride;
}
