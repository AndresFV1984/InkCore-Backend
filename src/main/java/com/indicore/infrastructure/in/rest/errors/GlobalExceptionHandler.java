package com.indicore.infrastructure.in.rest.errors;

import com.indicore.domain.client.exception.ClientAlreadyExistsException;
import com.indicore.domain.shared.exception.DomainException;
import com.indicore.domain.shared.exception.ResourceNotFoundException;
import com.indicore.domain.user.exception.AccountDisabledException;
import com.indicore.domain.user.exception.AccountLockedException;
import com.indicore.domain.user.exception.InvalidCredentialsException;
import com.indicore.domain.user.exception.UserAlreadyExistsException;
import com.indicore.infrastructure.in.rest.envelope.ApiErrorEnvelope;
import com.indicore.infrastructure.in.rest.envelope.ApiResponseFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final ApiResponseFactory responseFactory;

    public GlobalExceptionHandler(ApiResponseFactory responseFactory) {
        this.responseFactory = responseFactory;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorEnvelope> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .toList();
        return responseFactory.error(request, HttpStatus.BAD_REQUEST, "Datos invÃ¡lidos", errors);
    }

    @ExceptionHandler(ClientAlreadyExistsException.class)
    public ResponseEntity<ApiErrorEnvelope> handleClientExists(
            ClientAlreadyExistsException ex,
            HttpServletRequest request
    ) {
        return responseFactory.error(
                request,
                HttpStatus.CONFLICT,
                ex.getMessage(),
                List.of(ex.getCode())
        );
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiErrorEnvelope> handleUserExists(
            UserAlreadyExistsException ex,
            HttpServletRequest request
    ) {
        return responseFactory.error(
                request,
                HttpStatus.CONFLICT,
                ex.getMessage(),
                List.of(ex.getCode())
        );
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorEnvelope> handleInvalidCredentials(
            InvalidCredentialsException ex,
            HttpServletRequest request
    ) {
        return responseFactory.error(
                request,
                HttpStatus.UNAUTHORIZED,
                ex.getMessage(),
                List.of(ex.getCode())
        );
    }

    @ExceptionHandler(AccountDisabledException.class)
    public ResponseEntity<ApiErrorEnvelope> handleDisabled(
            AccountDisabledException ex,
            HttpServletRequest request
    ) {
        return responseFactory.error(
                request,
                HttpStatus.FORBIDDEN,
                ex.getMessage(),
                List.of(ex.getCode())
        );
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ApiErrorEnvelope> handleLocked(
            AccountLockedException ex,
            HttpServletRequest request
    ) {
        return responseFactory.error(
                request,
                HttpStatus.FORBIDDEN,
                ex.getMessage(),
                List.of(ex.getCode())
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorEnvelope> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request
    ) {
        return responseFactory.error(
                request,
                HttpStatus.FORBIDDEN,
                "No tiene permiso para acceder a este recurso",
                null
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorEnvelope> handleNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request
    ) {
        return responseFactory.error(
                request,
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                List.of(ex.getCode())
        );
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiErrorEnvelope> handleDomain(
            DomainException ex,
            HttpServletRequest request
    ) {
        return responseFactory.error(
                request,
                HttpStatus.UNPROCESSABLE_ENTITY,
                ex.getMessage(),
                List.of(ex.getCode())
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorEnvelope> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        return responseFactory.error(
                request,
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                null
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorEnvelope> handleGeneric(
            Exception ex,
            HttpServletRequest request
    ) {
        return responseFactory.error(
                request,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Error interno del servidor",
                null
        );
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }
}
