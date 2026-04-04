package com.cineflow.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "theaters",
        indexes = {
                @Index(name = "idx_theaters_region", columnList = "region"),
                @Index(name = "idx_theaters_active", columnList = "active")
        }
)
public class Theater {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 255)
    private String location;

    @Column(nullable = false, length = 100)
    private String region;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Builder.Default
    @OneToMany(mappedBy = "theater")
    private List<Screen> screens = new ArrayList<>();
}
