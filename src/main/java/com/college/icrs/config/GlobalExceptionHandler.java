package com.college.icrs.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationErrors(MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        ApiError apiError = ApiError.of(HttpStatus.BAD_REQUEST, "Validation failed", request, errors);
        return ResponseEntity.badRequest().body(apiError);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiError> handleNotFound(NoSuchElementException ex, WebRequest request) {
        ApiError apiError = ApiError.of(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiError);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiError> handleRuntime(RuntimeException ex, WebRequest request) {
        ApiError apiError = ApiError.of(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
    }

    public record ApiError(LocalDateTime timestamp, int status, String error, String message, String path,
                           Map<String, String> details) {
        static ApiError of(HttpStatus status, String message, WebRequest request, Map<String, String> details) {
            return new ApiError(
                    LocalDateTime.now(),
                    status.value(),
                    status.getReasonPhrase(),
                    message,
                    request.getDescription(false).replace("uri=", ""),
                    details
            );
        }
    }
}
