package com.inkcore.infrastructure.out.colorconversion;

import java.awt.color.ColorSpace;

/**
 * ColorSpace CMYK ligero solo para <em>escribir</em> / buffers ya convertidos.
 * Evita colgar TwelveMonkeys/ImageIO al serializar perfiles ICC grandes (FOGRA ~200KB)
 * desde un {@link java.awt.color.ICC_ColorSpace} en el ColorModel.
 * <p>
 * Los valores de píxel ya fueron convertidos con el perfil ICC real (LittleCMS);
 * este espacio no ejecuta la conversión de producción.
 */
public final class DeviceCmykColorSpace extends ColorSpace {

    public static final DeviceCmykColorSpace INSTANCE = new DeviceCmykColorSpace();

    private DeviceCmykColorSpace() {
        super(TYPE_CMYK, 4);
    }

    @Override
    public float[] toRGB(float[] cmyk) {
        float c = clamp01(cmyk[0]);
        float m = clamp01(cmyk[1]);
        float y = clamp01(cmyk[2]);
        float k = clamp01(cmyk[3]);
        float invK = 1f - k;
        return new float[]{(1f - c) * invK, (1f - m) * invK, (1f - y) * invK};
    }

    @Override
    public float[] fromRGB(float[] rgb) {
        float r = clamp01(rgb[0]);
        float g = clamp01(rgb[1]);
        float b = clamp01(rgb[2]);
        float k = 1f - Math.max(r, Math.max(g, b));
        if (k >= 1f - 1e-6f) {
            return new float[]{0f, 0f, 0f, 1f};
        }
        float invK = 1f - k;
        return new float[]{(1f - r - k) / invK, (1f - g - k) / invK, (1f - b - k) / invK, k};
    }

    @Override
    public float[] toCIEXYZ(float[] colorvalue) {
        return ColorSpace.getInstance(CS_sRGB).toCIEXYZ(toRGB(colorvalue));
    }

    @Override
    public float[] fromCIEXYZ(float[] colorvalue) {
        return fromRGB(ColorSpace.getInstance(CS_sRGB).fromCIEXYZ(colorvalue));
    }

    private static float clamp01(float v) {
        if (v < 0f) {
            return 0f;
        }
        return Math.min(1f, v);
    }
}
