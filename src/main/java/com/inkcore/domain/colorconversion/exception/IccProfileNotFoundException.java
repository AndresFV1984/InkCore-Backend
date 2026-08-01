package com.inkcore.domain.colorconversion.exception;

import com.inkcore.domain.shared.exception.DomainException;

public class IccProfileNotFoundException extends DomainException {

    public IccProfileNotFoundException(String profileName) {
        super(
                "ICC_PROFILE_NOT_FOUND",
                "Perfil ICC no encontrado: " + profileName
                        + ". Colóquelo en classpath:/color-profiles/ (ej. FOGRA39.icc, sRGB.icc)."
        );
    }
}
