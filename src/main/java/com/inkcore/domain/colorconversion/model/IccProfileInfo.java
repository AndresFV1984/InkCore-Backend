package com.inkcore.domain.colorconversion.model;

import java.util.List;

/**
 * Metadatos de un perfil ICC de destino para CTP/impresión.
 */
public record IccProfileInfo(
        String fileName,
        String title,
        String useCase,
        String paperClass,
        List<String> aliases,
        boolean available
) {
}
