package com.inkcore.infrastructure.out.colorconversion.lcms;

/**
 * LittleCMS nativo no disponible o no cargable en este host.
 */
public class LcmsNativeUnavailableException extends RuntimeException {

    public LcmsNativeUnavailableException(String message) {
        super(message);
    }

    public LcmsNativeUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
