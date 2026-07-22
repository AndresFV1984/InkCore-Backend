package com.inkcore.infrastructure.in.rest.errors;

import com.inkcore.domain.client.exception.ClientAlreadyExistsException;
import com.inkcore.domain.shared.exception.DomainException;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import com.inkcore.domain.user.exception.AccountDisabledException;
import com.inkcore.domain.user.exception.AccountLockedException;
import com.inkcore.domain.user.exception.InvalidCredentialsException;
import com.inkcore.domain.user.exception.InvalidRefreshTokenException;
import com.inkcore.domain.user.exception.PasswordExpiredException;
import com.inkcore.domain.user.exception.UserAlreadyExistsException;
import com.inkcore.domain.user.exception.UserLockedException;
import com.inkcore.domain.user.exception.UserNotFoundException;
import com.inkcore.infrastructure.in.rest.envelope.ApiErrorEnvelope;
import com.inkcore.infrastructure.in.rest.envelope.ApiResponseFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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
        return responseFactory.error(request, HttpStatus.BAD_REQUEST, "Datos inválidos", errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorEnvelope> handleUnreadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        return responseFactory.error(request, HttpStatus.BAD_REQUEST, "Solicitud inválida", null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorEnvelope> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request
    ) {
        return responseFactory.error(request, HttpStatus.METHOD_NOT_ALLOWED, "Método HTTP no permitido", null);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorEnvelope> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException ex,
            HttpServletRequest request
    ) {
        return responseFactory.error(request, HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content-Type no soportado", null);
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ApiErrorEnvelope> handleNotFoundRoute(
            Exception ex,
            HttpServletRequest request
    ) {
        return responseFactory.error(request, HttpStatus.NOT_FOUND, "Recurso no encontrado", null);
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

    @ExceptionHandler(UserLockedException.class)
    public ResponseEntity<ApiErrorEnvelope> handleUserLocked(
            UserLockedException ex,
            HttpServletRequest request
    ) {
        return responseFactory.error(
                request,
                HttpStatus.UNAUTHORIZED,
                ex.getMessage(),
                List.of(ex.getCode(), "remainingMinutes=" + ex.getRemainingMinutes())
        );
    }

    @ExceptionHandler({AccountLockedException.class})
    public ResponseEntity<ApiErrorEnvelope> handleAccountLocked(
            AccountLockedException ex,
            HttpServletRequest request
    ) {
        return responseFactory.error(
                request,
                HttpStatus.UNAUTHORIZED,
                ex.getMessage(),
                List.of(ex.getCode())
        );
    }

    @ExceptionHandler(PasswordExpiredException.class)
    public ResponseEntity<ApiErrorEnvelope> handlePasswordExpired(
            PasswordExpiredException ex,
            HttpServletRequest request
    ) {
        return responseFactory.error(
                request,
                HttpStatus.UNAUTHORIZED,
                ex.getMessage(),
                List.of(ex.getCode())
        );
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ApiErrorEnvelope> handleInvalidRefreshToken(
            InvalidRefreshTokenException ex,
            HttpServletRequest request
    ) {
        return responseFactory.error(
                request,
                HttpStatus.UNAUTHORIZED,
                ex.getMessage(),
                List.of(ex.getCode(), "Requiere nueva autenticación")
        );
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiErrorEnvelope> handleUserNotFound(
            UserNotFoundException ex,
            HttpServletRequest request
    ) {
        return responseFactory.error(
                request,
                HttpStatus.NOT_FOUND,
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

    @ExceptionHandler(org.springframework.dao.DataAccessException.class)
    public ResponseEntity<ApiErrorEnvelope> handleDataAccess(
            org.springframework.dao.DataAccessException ex,
            HttpServletRequest request
    ) {
        log.error("Data access error on {}", request.getRequestURI(), ex);
        return responseFactory.error(
                request,
                HttpStatus.SERVICE_UNAVAILABLE,
                "Servicio de datos no disponible",
                null
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorEnvelope> handleGeneric(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Unhandled error on {}", request.getRequestURI(), ex);
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
