package com.focisolutions.takehomeexercise.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Builder;

@Builder
public record TodoCreateRequest(
        @NotBlank(message = "title must not be blank")
        @Size(max = 200, message = "title must be at most 200 characters") String title,
        @Size(max = 2000, message = "description must be at most 2000 characters") String description,
        LocalDate dueDate) {
}
