package com.inkcore.domain.colorconversion.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Preset de calidad creativa sobre la conversión ICC.
 * Los overrides explícitos ({@code brightnessLift}, {@code vibranceBoost},
 * {@code softProofBrightnessMatch}) tienen prioridad sobre el preset.
 */
@Schema(
        name = "QualityPreset",
        description = """
                Preset de calidad para POST /color-conversions/convert.
                FIDELITY = CTP puro (lift 0 / vibrance 0 / softProof false).
                COMMERCIAL = lift 0.12 / vibrance 0.28 / softProof true (recomendado fotos).
                VIVID = lift 0.16 / vibrance 0.35 / softProof true (comida / punch).
                Overrides explícitos (brightnessLift, vibranceBoost, softProofBrightnessMatch) tienen prioridad.
                """
)
public enum QualityPreset {
    @Schema(description = "Fidelidad CTP: lift=0, vibrance=0, softProof=false")
    FIDELITY,

    @Schema(description = "Comercial: lift=0.12, vibrance=0.28, softProof=true")
    COMMERCIAL,

    @Schema(description = "Vívido: lift=0.16, vibrance=0.35, softProof=true")
    VIVID;

    public float brightnessLift() {
        return switch (this) {
            case FIDELITY -> 0f;
            case COMMERCIAL -> 0.12f;
            case VIVID -> 0.16f;
        };
    }

    public float vibranceBoost() {
        return switch (this) {
            case FIDELITY -> 0f;
            case COMMERCIAL -> 0.28f;
            case VIVID -> 0.35f;
        };
    }

    public boolean softProofBrightnessMatch() {
        return this != FIDELITY;
    }

    public static QualityPreset fromParam(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        return switch (normalized) {
            case "FIDELITY", "CTP", "DEFAULT", "NONE" -> FIDELITY;
            case "COMMERCIAL", "COMERCIAL", "PHOTO", "FOTO" -> COMMERCIAL;
            case "VIVID", "VIVIDO", "VÍVIDO", "MAX" -> VIVID;
            default -> throw new IllegalArgumentException(
                    "qualityPreset inválido: use FIDELITY, COMMERCIAL o VIVID"
            );
        };
    }
}
