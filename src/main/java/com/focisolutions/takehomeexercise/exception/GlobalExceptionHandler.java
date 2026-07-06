package com.focisolutions.takehomeexercise.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.List;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(TodoNotFoundException.class)
    ResponseEntity<Object> handleTodoNotFound(final TodoNotFoundException exception, final WebRequest request) {
        final ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        setInstance(problemDetail, request);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<Object> handleOptimisticLockingFailure(final ObjectOptimisticLockingFailureException exception,
            final WebRequest request) {
        final ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "The todo was modified by another request in the meantime; reload it and try again.");
        setInstance(problemDetail, request);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<Object> handleConstraintViolation(final ConstraintViolationException exception, final WebRequest request) {
        final List<String> errors = exception.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .toList();
        final ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        problemDetail.setProperty("errors", errors);
        setInstance(problemDetail, request);
        return ResponseEntity.badRequest().body(problemDetail);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(final MethodArgumentNotValidException ex,
            final HttpHeaders headers, final HttpStatusCode status, final WebRequest request) {
        final ResponseEntity<Object> response = super.handleMethodArgumentNotValid(ex, headers, status, request);
        final ProblemDetail problemDetail = (ProblemDetail) response.getBody();
        final List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .toList();
        problemDetail.setProperty("errors", errors);
        setInstance(problemDetail, request);
        return response;
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(final HandlerMethodValidationException ex,
            final HttpHeaders headers, final HttpStatusCode status, final WebRequest request) {
        final ResponseEntity<Object> response = super.handleHandlerMethodValidationException(ex, headers, status, request);
        final ProblemDetail problemDetail = (ProblemDetail) response.getBody();
        final List<String> errors = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream())
                .map(MessageSourceResolvable::getDefaultMessage)
                .toList();
        problemDetail.setProperty("errors", errors);
        setInstance(problemDetail, request);
        return response;
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(final TypeMismatchException ex, final HttpHeaders headers,
            final HttpStatusCode status, final WebRequest request) {
        final String detail = "%s: invalid value '%s'".formatted(ex.getPropertyName(), ex.getValue());
        final ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setProperty("errors", List.of(detail));
        setInstance(problemDetail, request);
        return handleExceptionInternal(ex, problemDetail, headers, status, request);
    }

    private void setInstance(final ProblemDetail problemDetail, final WebRequest request) {
        if (request instanceof final NativeWebRequest nativeWebRequest) {
            final HttpServletRequest servletRequest = nativeWebRequest.getNativeRequest(HttpServletRequest.class);
            if (servletRequest != null) {
                problemDetail.setInstance(URI.create(servletRequest.getRequestURI()));
            }
        }
    }
}
