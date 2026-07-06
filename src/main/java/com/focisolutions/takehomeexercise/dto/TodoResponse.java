package com.focisolutions.takehomeexercise.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Builder;

@Builder
@Schema(description = "A Todo item")
public record TodoResponse(
        @Schema(description = "Server-generated id", example = "1") Long id,
        @Schema(description = "Short description of the task", example = "Buy milk") String title,
        @Schema(description = "Longer explanation, may be null", example = "2 litres, oat milk") String description,
        @Schema(description = "Due date, may be null (ISO-8601)", example = "2026-07-10") LocalDate dueDate,
        @Schema(description = "Whether the task is completed", example = "false") boolean completed,
        @Schema(description = "When the Todo was created, immutable") Instant createdAt,
        @Schema(description = "When the Todo was last changed; equals createdAt at creation") Instant updatedAt) {
}
