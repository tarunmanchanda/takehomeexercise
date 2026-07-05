package com.focisolutions.takehomeexercise.exception;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ErrorResponse> handleHandlerMethodValidation(final HandlerMethodValidationException exception) {
        final List<String> details = exception.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream())
                .map(MessageSourceResolvable::getDefaultMessage)
                .toList();
        final ErrorResponse errorResponse = ErrorResponse.builder()
                .message("Validation failed")
                .details(details)
                .build();
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ErrorResponse> handleConstraintViolation(final ConstraintViolationException exception) {
        final List<String> details = exception.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .toList();
        final ErrorResponse errorResponse = ErrorResponse.builder()
                .message("Validation failed")
                .details(details)
                .build();
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErrorResponse> handleTypeMismatch(final MethodArgumentTypeMismatchException exception) {
        final String detail = "%s: invalid value '%s'".formatted(exception.getName(), exception.getValue());
        final ErrorResponse errorResponse = ErrorResponse.builder()
                .message("Validation failed")
                .details(List.of(detail))
                .build();
        return ResponseEntity.badRequest().body(errorResponse);
    }
}
