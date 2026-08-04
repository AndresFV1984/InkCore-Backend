package com.inkcore.infrastructure.out.colorconversion.lcms;

/**
 * Perfil ICC inválido, corrupto o no usable por LittleCMS.
 */
public class LcmsProfileException extends RuntimeException {

    public LcmsProfileException(String message) {
        super(message);
    }

    public LcmsProfileException(String message, Throwable cause) {
        super(message, cause);
    }
}
