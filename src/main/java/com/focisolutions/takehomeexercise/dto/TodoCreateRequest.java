package com.focisolutions.takehomeexercise.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Builder;

@Builder
@Schema(description = "Request body for creating a new Todo")
public record TodoCreateRequest(
        @NotBlank(message = "title must not be blank")
        @Size(max = 200, message = "title must be at most 200 characters")
        @Schema(description = "Short description of the task", example = "Buy milk") String title,
        @Size(max = 2000, message = "description must be at most 2000 characters")
        @Schema(description = "Longer explanation, optional", example = "2 litres, oat milk") String description,
        @Schema(description = "Due date, optional (ISO-8601)", example = "2026-07-10") LocalDate dueDate) {
}
