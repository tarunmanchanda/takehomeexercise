package com.focisolutions.takehomeexercise.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record TodoUpdateRequest(
        @NotBlank(message = "title must not be blank") String title,
        String description,
        LocalDate dueDate) {
}
