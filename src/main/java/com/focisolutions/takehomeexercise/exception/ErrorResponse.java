package com.focisolutions.takehomeexercise.exception;

import java.util.List;
import lombok.Builder;

@Builder
record ErrorResponse(String message, List<String> details) {
}
