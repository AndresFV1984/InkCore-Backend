package com.inkcore.infrastructure.in.rest.errors;

import com.inkcore.infrastructure.in.rest.envelope.ApiErrorEnvelope;
import com.inkcore.infrastructure.in.rest.envelope.ApiResponseFactory;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sustituye la página Whitelabel / JSON por defecto de Spring Boot
 * por el envelope {@link ApiErrorEnvelope} del proyecto.
 * No se documenta en OpenAPI/Swagger.
 */
@Hidden
@RestController
public class ApiErrorController implements ErrorController {

    private final ApiResponseFactory responseFactory;

    public ApiErrorController(ApiResponseFactory responseFactory) {
        this.responseFactory = responseFactory;
    }

    @Hidden
    @Operation(hidden = true)
    @RequestMapping("${server.error.path:/error}")
    public ResponseEntity<ApiErrorEnvelope> error(HttpServletRequest request) {
        HttpStatus status = resolveStatus(request);
        return responseFactory.error(request, status, defaultMessage(status), null);
    }

    private static HttpStatus resolveStatus(HttpServletRequest request) {
        Object code = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (code instanceof Integer statusCode) {
            HttpStatus resolved = HttpStatus.resolve(statusCode);
            if (resolved != null) {
                return resolved;
            }
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private static String defaultMessage(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "Recurso no encontrado";
            case UNAUTHORIZED -> "No autenticado";
            case FORBIDDEN -> "No tiene permiso para acceder a este recurso";
            case METHOD_NOT_ALLOWED -> "Método HTTP no permitido";
            case BAD_REQUEST -> "Solicitud inválida";
            default -> status.is5xxServerError()
                    ? "Error interno del servidor"
                    : status.getReasonPhrase();
        };
    }
}
