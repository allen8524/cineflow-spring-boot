package com.cineflow.dto;

import com.cineflow.domain.Theater;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminTheaterForm {

    @NotBlank(message = "Please enter a theater name.")
    @Size(max = 100, message = "Theater name must be 100 characters or fewer.")
    private String name;

    @NotBlank(message = "Please enter location information.")
    @Size(max = 255, message = "Location must be 255 characters or fewer.")
    private String location;

    @NotBlank(message = "Please enter a region.")
    @Size(max = 100, message = "Region must be 100 characters or fewer.")
    private String region;

    @Size(max = 1000, message = "Description must be 1000 characters or fewer.")
    private String description;

    private boolean active = true;

    public static AdminTheaterForm from(Theater theater) {
        AdminTheaterForm form = new AdminTheaterForm();
        form.setName(theater.getName());
        form.setLocation(theater.getLocation());
        form.setRegion(theater.getRegion());
        form.setDescription(theater.getDescription());
        form.setActive(theater.isActive());
        return form;
    }
}
