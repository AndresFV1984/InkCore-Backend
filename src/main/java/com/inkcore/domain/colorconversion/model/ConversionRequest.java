package com.inkcore.domain.colorconversion.model;

import java.util.Locale;
import java.util.Objects;

/**
 * Entrada de dominio para conversión RGB → CMYK.
 * El aumento de peso (~20–40 %) al pasar de 3 a 4 canales es esperado; no comprimir con pérdida.
 * <p>
 * Ajustes creativos opcionales ({@code brightnessLift}, {@code vibranceBoost},
 * {@code softProofBrightnessMatch}): {@code null} = usar defaults del servidor (CTP: 0 / 0 / false).
 */
public final class ConversionRequest {

    public static final float BRIGHTNESS_LIFT_MIN = 0f;
    public static final float BRIGHTNESS_LIFT_MAX = 0.15f;
    public static final float VIBRANCE_BOOST_MIN = 0f;
    public static final float VIBRANCE_BOOST_MAX = 0.25f;

    private final byte[] fileBytes;
    private final String originalFileName;
    private final String mimeType;
    private final RenderingIntent renderingIntent;
    private final String destinationIccProfile;
    private final String userId;
    private final OutputFormat outputFormat;
    private final Float brightnessLift;
    private final Float vibranceBoost;
    private final Boolean softProofBrightnessMatch;

    public ConversionRequest(
            byte[] fileBytes,
            String originalFileName,
            String mimeType,
            RenderingIntent renderingIntent,
            String destinationIccProfile,
            String userId
    ) {
        this(fileBytes, originalFileName, mimeType, renderingIntent, destinationIccProfile, userId, null,
                null, null, null);
    }

    public ConversionRequest(
            byte[] fileBytes,
            String originalFileName,
            String mimeType,
            RenderingIntent renderingIntent,
            String destinationIccProfile,
            String userId,
            OutputFormat outputFormat
    ) {
        this(fileBytes, originalFileName, mimeType, renderingIntent, destinationIccProfile, userId, outputFormat,
                null, null, null);
    }

    public ConversionRequest(
            byte[] fileBytes,
            String originalFileName,
            String mimeType,
            RenderingIntent renderingIntent,
            String destinationIccProfile,
            String userId,
            OutputFormat outputFormat,
            Float brightnessLift,
            Float vibranceBoost,
            Boolean softProofBrightnessMatch
    ) {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new IllegalArgumentException("El archivo es obligatorio");
        }
        this.fileBytes = fileBytes;
        this.originalFileName = Objects.requireNonNullElse(originalFileName, "upload");
        this.mimeType = mimeType;
        this.renderingIntent = Objects.requireNonNullElse(renderingIntent, RenderingIntent.PERCEPTUAL);
        this.destinationIccProfile = destinationIccProfile;
        this.userId = userId;
        this.outputFormat = outputFormat;
        this.brightnessLift = validateBrightnessLift(brightnessLift);
        this.vibranceBoost = validateVibranceBoost(vibranceBoost);
        this.softProofBrightnessMatch = softProofBrightnessMatch;
    }

    public byte[] getFileBytes() {
        return fileBytes;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public RenderingIntent getRenderingIntent() {
        return renderingIntent;
    }

    public String getDestinationIccProfile() {
        return destinationIccProfile;
    }

    public String getUserId() {
        return userId;
    }

    public OutputFormat getOutputFormat() {
        return outputFormat;
    }

    /**
     * Override de brightness-lift, o {@code null} para default del servidor.
     */
    public Float getBrightnessLift() {
        return brightnessLift;
    }

    /**
     * Override de vibrance-boost, o {@code null} para default del servidor.
     */
    public Float getVibranceBoost() {
        return vibranceBoost;
    }

    /**
     * Override de soft-proof-brightness-match, o {@code null} para default del servidor.
     */
    public Boolean getSoftProofBrightnessMatch() {
        return softProofBrightnessMatch;
    }

    public long getOriginalSizeBytes() {
        return fileBytes.length;
    }

    public float resolveBrightnessLift(float serverDefault) {
        return brightnessLift != null ? brightnessLift : clamp(serverDefault, BRIGHTNESS_LIFT_MIN, BRIGHTNESS_LIFT_MAX);
    }

    public float resolveVibranceBoost(float serverDefault) {
        return vibranceBoost != null ? vibranceBoost : clamp(serverDefault, VIBRANCE_BOOST_MIN, VIBRANCE_BOOST_MAX);
    }

    public boolean resolveSoftProofBrightnessMatch(boolean serverDefault) {
        return softProofBrightnessMatch != null ? softProofBrightnessMatch : serverDefault;
    }

    public static Float parseBrightnessLift(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return validateBrightnessLift(Float.parseFloat(raw.trim().replace(',', '.')));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                    "brightnessLift inválido: use un número entre "
                            + BRIGHTNESS_LIFT_MIN + " y " + BRIGHTNESS_LIFT_MAX
            );
        }
    }

    public static Float parseVibranceBoost(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return validateVibranceBoost(Float.parseFloat(raw.trim().replace(',', '.')));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                    "vibranceBoost inválido: use un número entre "
                            + VIBRANCE_BOOST_MIN + " y " + VIBRANCE_BOOST_MAX
            );
        }
    }

    public static Boolean parseSoftProofBrightnessMatch(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "true", "1", "yes", "si", "sí" -> true;
            case "false", "0", "no" -> false;
            default -> throw new IllegalArgumentException(
                    "softProofBrightnessMatch inválido: use true o false"
            );
        };
    }

    private static Float validateBrightnessLift(Float value) {
        if (value == null) {
            return null;
        }
        if (Float.isNaN(value) || value < BRIGHTNESS_LIFT_MIN || value > BRIGHTNESS_LIFT_MAX) {
            throw new IllegalArgumentException(
                    "brightnessLift fuera de rango: permitido "
                            + BRIGHTNESS_LIFT_MIN + "–" + BRIGHTNESS_LIFT_MAX + " (recibido: " + value + ")"
            );
        }
        return value;
    }

    private static Float validateVibranceBoost(Float value) {
        if (value == null) {
            return null;
        }
        if (Float.isNaN(value) || value < VIBRANCE_BOOST_MIN || value > VIBRANCE_BOOST_MAX) {
            throw new IllegalArgumentException(
                    "vibranceBoost fuera de rango: permitido "
                            + VIBRANCE_BOOST_MIN + "–" + VIBRANCE_BOOST_MAX + " (recibido: " + value + ")"
            );
        }
        return value;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
