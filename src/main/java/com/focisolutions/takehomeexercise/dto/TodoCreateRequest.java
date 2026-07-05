package com.focisolutions.takehomeexercise.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import lombok.Builder;

@Builder
public record TodoCreateRequest(
        @NotBlank(message = "title must not be blank") String title,
        String description,
        LocalDate dueDate) {
}
