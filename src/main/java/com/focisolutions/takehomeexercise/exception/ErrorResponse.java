package com.focisolutions.takehomeexercise.exception;

import java.util.List;

record ErrorResponse(String message, List<String> details) {

    static ErrorResponse of(final String message) {
        return new ErrorResponse(message, List.of());
    }

    static ErrorResponse of(final String message, final List<String> details) {
        return new ErrorResponse(message, details);
    }
}
