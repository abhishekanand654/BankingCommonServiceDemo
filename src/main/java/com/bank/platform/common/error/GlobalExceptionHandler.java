package com.bank.platform.common.error;

import com.bank.platform.common.http.DownstreamException;
import com.bank.platform.common.web.CorrelationId;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> details = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err ->
                details.put(err.getField(), err.getDefaultMessage())
        );

        ApiError body = new ApiError(
                "VALIDATION_ERROR",
                "Request validation failed",
                CorrelationId.getOrCreate(),
                Instant.now(),
                details
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex) {
        ApiError body = new ApiError(
                "VALIDATION_ERROR",
                ex.getMessage(),
                CorrelationId.getOrCreate(),
                Instant.now(),
                null
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(DownstreamException.class)
    public ResponseEntity<ApiError> handleDownstream(DownstreamException ex) {
        Map<String, Object> details = new HashMap<>();
        details.put("target", ex.getTarget());
        if (ex.getDownstreamBody() != null) details.put("downstreamBody", ex.getDownstreamBody());

        ApiError body = new ApiError(
                "DOWNSTREAM_ERROR",
                ex.getMessage(),
                CorrelationId.getOrCreate(),
                Instant.now(),
                details
        );

        // Typically map to 502/504; using the exception’s status for visibility.
        int status = ex.getHttpStatus() <= 0 ? 502 : ex.getHttpStatus();
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(ForbiddenBusinessException.class)
    public ResponseEntity<ApiError> handleForbidden(ForbiddenBusinessException ex) {
        ApiError body = new ApiError(
                ex.getCode(),
                ex.getMessage(),
                CorrelationId.getOrCreate(),
                Instant.now(),
                ex.getDetails()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAny(Exception ex) {
        ApiError body = new ApiError(
                "INTERNAL_ERROR",
                "Unexpected error",
                CorrelationId.getOrCreate(),
                Instant.now(),
                null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}