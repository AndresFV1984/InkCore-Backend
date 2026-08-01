package com.inkcore.infrastructure.out.inkestimation;

import org.apache.pdfbox.pdmodel.graphics.color.PDColorSpace;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceN;
import org.apache.pdfbox.pdmodel.graphics.color.PDIndexed;
import org.apache.pdfbox.pdmodel.graphics.color.PDPattern;
import org.apache.pdfbox.pdmodel.graphics.color.PDSeparation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Resuelve nombres de tintas spot desde espacios PDF (incluye Indexed/Pattern anidados).
 * <p>
 * Importante: {@link PDSeparation#getName()} devuelve siempre {@code "Separation"};
 * el nombre real del colorante es {@link PDSeparation#getColorantName()}.
 */
final class PdfSpotColorSpaces {

    private PdfSpotColorSpaces() {
    }

    /**
     * Nombre literal del colorante Separation (incluye Cyan/All si vienen así en el PDF).
     * Nunca usar {@link PDSeparation#getName()} (== {@code "Separation"}).
     */
    static String separationColorantName(PDSeparation sep) {
        if (sep == null) {
            return null;
        }
        String name = sep.getColorantName();
        if (name == null) {
            return null;
        }
        name = name.trim();
        if (name.isEmpty() || "Separation".equalsIgnoreCase(name)) {
            return null;
        }
        return name;
    }

    /**
     * Desenvuelve Indexed / Pattern hasta el espacio útil para tinta.
     */
    static PDColorSpace unwrap(PDColorSpace cs) throws IOException {
        if (cs == null) {
            return null;
        }
        if (cs instanceof PDIndexed indexed) {
            return unwrap(indexed.getBaseColorSpace());
        }
        if (cs instanceof PDPattern pattern) {
            PDColorSpace underlying = pattern.getUnderlyingColorSpace();
            if (underlying != null) {
                return unwrap(underlying);
            }
        }
        return cs;
    }

    /**
     * Extrae colorantes spot con <b>nombre de referencia literal del PDF</b>.
     * @return nombre exacto → swatch opcional
     */
    static Map<String, String> extractReportableSpots(PDColorSpace cs) throws IOException {
        Map<String, String> out = new LinkedHashMap<>();
        PDColorSpace base = unwrap(cs);
        if (base instanceof PDSeparation sep) {
            String inkName = SpotColorantNames.exactSpotReference(separationColorantName(sep));
            if (inkName != null) {
                out.put(inkName, swatchFromSeparation(sep));
            }
        } else if (base instanceof PDDeviceN deviceN) {
            List<String> names = deviceN.getColorantNames();
            if (names != null) {
                for (String raw : names) {
                    String inkName = SpotColorantNames.exactSpotReference(raw);
                    if (inkName != null) {
                        out.putIfAbsent(inkName, "#808080");
                    }
                }
            }
        }
        return out;
    }

    static List<String> reportableColorantNames(PDColorSpace cs) throws IOException {
        return new ArrayList<>(extractReportableSpots(cs).keySet());
    }

    static String swatchFromSeparation(PDSeparation sep) {
        try {
            float[] rgb = sep.toRGB(new float[]{1f});
            int r = clamp255(Math.round(rgb[0] * 255f));
            int g = clamp255(Math.round(rgb[1] * 255f));
            int b = clamp255(Math.round(rgb[2] * 255f));
            return String.format("#%02X%02X%02X", r, g, b);
        } catch (Exception ex) {
            return "#808080";
        }
    }

    private static int clamp255(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
