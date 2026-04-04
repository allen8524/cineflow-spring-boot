package com.cineflow.dto;

import com.cineflow.domain.Screen;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminScreenForm {

    @NotNull(message = "Please select a theater.")
    private Long theaterId;

    @NotBlank(message = "Please enter a screen name.")
    @Size(max = 50, message = "Screen name must be 50 characters or fewer.")
    private String name;

    @NotBlank(message = "Please enter a screen format.")
    @Size(max = 50, message = "Screen format must be 50 characters or fewer.")
    private String screenType;

    @NotNull(message = "Please enter total seat count.")
    @Min(value = 12, message = "Seat count must be at least 12.")
    @Max(value = 312, message = "Seat count must be 312 or fewer.")
    private Integer totalSeats;

    private boolean active = true;

    public static AdminScreenForm from(Screen screen) {
        AdminScreenForm form = new AdminScreenForm();
        form.setTheaterId(screen.getTheater().getId());
        form.setName(screen.getName());
        form.setScreenType(screen.getScreenType());
        form.setTotalSeats(screen.getTotalSeats());
        form.setActive(screen.isActive());
        return form;
    }
}
