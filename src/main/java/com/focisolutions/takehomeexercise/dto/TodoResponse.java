package com.focisolutions.takehomeexercise.dto;

import java.time.Instant;
import java.time.LocalDate;

public record TodoResponse(
        Long id,
        String title,
        String description,
        LocalDate dueDate,
        boolean completed,
        Instant createdAt) {
}
