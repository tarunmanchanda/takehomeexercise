package com.focisolutions.takehomeexercise.exception;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(TodoNotFoundException.class)
    ResponseEntity<ErrorResponse> handleTodoNotFound(final TodoNotFoundException exception) {
        final ErrorResponse errorResponse = ErrorResponse.builder()
                .message(exception.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidationFailure(final MethodArgumentNotValidException exception) {
        final List<String> details = exception.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .toList();
        final ErrorResponse errorResponse = ErrorResponse.builder()
                .message("Validation failed")
                .details(details)
                .build();
        return ResponseEntity.badRequest().body(errorResponse);
    }
}
