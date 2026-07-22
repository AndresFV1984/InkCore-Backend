package com.inkcore.infrastructure.in.rest.envelope;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class ApiResponseFactory {

    private final ObjectMapper objectMapper;

    public ApiResponseFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public <T> ResponseEntity<ApiSuccessEnvelope<T>> success(
            HttpServletRequest request,
            HttpStatus status,
            T data
    ) {
        ApiSuccessEnvelope<T> body = new ApiSuccessEnvelope<>(
                ApiHeaders.basic(resolveCorrelationId(request), status.value()),
                Instant.now(),
                data
        );
        return ResponseEntity.status(status).body(body);
    }

    /**
     * Login/refresh: tokens en envelope.headers + Authorization HTTP.
     */
    public <T> ResponseEntity<ApiSuccessEnvelope<T>> okWithTokens(
            HttpServletRequest request,
            T data,
            String accessToken,
            long accessExpiresInSeconds,
            String refreshToken,
            long refreshExpiresInSeconds,
            String code,
            String description
    ) {
        String bearer = "Bearer " + accessToken;
        ApiSuccessEnvelope<T> body = new ApiSuccessEnvelope<>(
                ApiHeaders.withTokens(
                        resolveCorrelationId(request),
                        bearer,
                        accessExpiresInSeconds,
                        refreshToken,
                        refreshExpiresInSeconds,
                        code,
                        description
                ),
                Instant.now(),
                data
        );
        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.AUTHORIZATION, bearer)
                .body(body);
    }

    /**
     * Respuesta estándar autenticada (list/get/etc.): sin {@code token} ni {@code status} en el envelope.
     */
    public <T> ResponseEntity<ApiSuccessEnvelope<T>> okStandard(
            HttpServletRequest request,
            T data
    ) {
        ApiSuccessEnvelope<T> body = new ApiSuccessEnvelope<>(
                ApiHeaders.standard(resolveCorrelationId(request)),
                Instant.now(),
                data
        );
        return ResponseEntity.status(HttpStatus.OK).body(body);
    }

    /**
     * Alta de recurso: HTTP 201, code/description de negocio; sin tokens en headers.
     */
    public <T> ResponseEntity<ApiSuccessEnvelope<T>> created(
            HttpServletRequest request,
            String code,
            String description,
            T data
    ) {
        ApiSuccessEnvelope<T> body = new ApiSuccessEnvelope<>(
                ApiHeaders.created(resolveCorrelationId(request), code, description),
                Instant.now(),
                data
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    public ResponseEntity<ApiErrorEnvelope> error(
            HttpServletRequest request,
            HttpStatus status,
            String message,
            List<String> errors
    ) {
        return ResponseEntity.status(status).body(buildError(request, status, message, errors));
    }

    /** Escribe el envelope de error directo en la respuesta (p. ej. filtros de seguridad). */
    public void writeError(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus status,
            String message
    ) throws IOException {
        response.setStatus(status.value());
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), buildError(request, status, message, null));
    }

    private ApiErrorEnvelope buildError(
            HttpServletRequest request,
            HttpStatus status,
            String message,
            List<String> errors
    ) {
        return new ApiErrorEnvelope(
                ApiHeaders.basic(resolveCorrelationId(request), status.value()),
                Instant.now(),
                resolvePath(request),
                message,
                errors
        );
    }

    private String resolvePath(HttpServletRequest request) {
        Object errorPath = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        if (errorPath instanceof String uri && !uri.isBlank()) {
            return uri;
        }
        return request.getRequestURI();
    }

    private String resolveCorrelationId(HttpServletRequest request) {
        String header = request.getHeader("X-Correlation-Id");
        return header != null && !header.isBlank() ? header : UUID.randomUUID().toString();
    }
}
