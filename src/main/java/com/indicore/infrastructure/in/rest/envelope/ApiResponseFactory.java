package com.indicore.infrastructure.in.rest.envelope;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class ApiResponseFactory {

    public <T> ResponseEntity<ApiSuccessEnvelope<T>> success(
            HttpServletRequest request,
            HttpStatus status,
            T data
    ) {
        ApiSuccessEnvelope<T> body = new ApiSuccessEnvelope<>(
                new ApiHeaders(resolveCorrelationId(request), status.value()),
                Instant.now(),
                data
        );
        return ResponseEntity.status(status).body(body);
    }

    public ResponseEntity<ApiErrorEnvelope> error(
            HttpServletRequest request,
            HttpStatus status,
            String message,
            List<String> errors
    ) {
        ApiErrorEnvelope body = new ApiErrorEnvelope(
                new ApiHeaders(resolveCorrelationId(request), status.value()),
                Instant.now(),
                request.getRequestURI(),
                message,
                errors
        );
        return ResponseEntity.status(status).body(body);
    }

    private String resolveCorrelationId(HttpServletRequest request) {
        String header = request.getHeader("X-Correlation-Id");
        return header != null && !header.isBlank() ? header : UUID.randomUUID().toString();
    }
}
