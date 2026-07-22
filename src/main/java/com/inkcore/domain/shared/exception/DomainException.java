package com.inkcore.domain.shared.exception;

/**
 * Excepción base de reglas de negocio del dominio.
 */
public abstract class DomainException extends RuntimeException {

    private final String code;

    protected DomainException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
