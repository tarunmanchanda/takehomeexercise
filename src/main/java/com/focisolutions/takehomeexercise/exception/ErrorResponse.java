package com.focisolutions.takehomeexercise.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;

/**
 * Standard error response body returned for {@code 400}/{@code 404} outcomes.
 */
@Builder
@Schema(description = "Standard error response")
public record ErrorResponse(
        @Schema(description = "Human-readable error summary", example = "Validation failed") String message,
        @Schema(description = "Per-field or per-violation detail messages, empty if not applicable")
        List<String> details) {
}
