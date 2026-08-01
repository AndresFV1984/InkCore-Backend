package com.inkcore.infrastructure.out.inkestimation;

import java.util.Locale;

/**
 * Reglas de nombres de colorantes Separation/DeviceN para estimación de tinta.
 * <p>
 * El nombre de referencia se toma <b>literal</b> del PDF ({@code getColorantName()} /
 * lista DeviceN). No se inventa, no se traduce y no se antepone "Pantone ".
 */
final class SpotColorantNames {

    private SpotColorantNames() {
    }

    /**
     * Nombre de referencia exacto tal como está en el PDF, o {@code null} si no es
     * una tinta spot reportable (All/None/CMYK/técnicos/vacío).
     * Solo hace {@code trim()}; no cambia mayúsculas ni el texto.
     */
    static String exactSpotReference(String rawFromPdf) {
        if (rawFromPdf == null) {
            return null;
        }
        String name = rawFromPdf.trim();
        if (name.isEmpty()) {
            return null;
        }
        // PDFBox a veces expone el tipo en lugar del colorante
        if ("Separation".equalsIgnoreCase(name) || "DeviceN".equalsIgnoreCase(name)) {
            return null;
        }
        if (!isReportableSpotColorant(name)) {
            return null;
        }
        return name;
    }

    static boolean isReportableSpotColorant(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        return !isProcessName(name) && !isSpecialPdfColorant(name) && !isTechnicalNonInkColorant(name);
    }

    static boolean isProcessName(String name) {
        String n = normalize(name);
        return n.equals("cyan") || n.equals("magenta") || n.equals("yellow")
                || n.equals("black") || n.equals("c") || n.equals("m") || n.equals("y") || n.equals("k")
                || n.equals("cian") || n.equals("negro") || n.equals("amarillo");
    }

    /** PDF spec: Separation names All / None. También el literal de PDSeparation.getName(). */
    static boolean isSpecialPdfColorant(String name) {
        String n = normalize(name);
        return n.equals("all") || n.equals("none") || n.equals("separation") || n.equals("devicen");
    }

    static boolean isTechnicalNonInkColorant(String name) {
        String n = normalize(name);
        if (n.contains("registration") || n.equals("reg") || n.equals("registro")) {
            return true;
        }
        if (n.contains("varnish") || n.contains("barniz") || n.contains("coating") || n.contains("aq ")) {
            return true;
        }
        if (n.contains("dieline") || n.contains("die line") || n.contains("die-line")
                || n.contains("cutcontour") || n.contains("cut contour") || n.contains("thru-cut")
                || n.contains("troquel") || n.contains("kiss cut") || n.contains("perforat")) {
            return true;
        }
        if (n.contains("emboss") || n.contains("deboss") || n.contains("foil") || n.contains("stamp")) {
            return true;
        }
        return false;
    }

    /** Heurística: nombre tipográfico Pantone / PMS (solo informativa; no renombra). */
    static boolean looksLikePantone(String name) {
        if (name == null) {
            return false;
        }
        String n = normalize(name);
        return n.contains("pantone") || n.startsWith("pms ") || n.contains(" pms ") || n.matches(".*\\bpms\\b.*");
    }

    static int processIndex(String name) {
        String n = normalize(name);
        if (n.equals("c") || n.equals("cyan") || n.equals("cian")) {
            return 0;
        }
        if (n.equals("m") || n.equals("magenta")) {
            return 1;
        }
        if (n.equals("y") || n.equals("yellow") || n.equals("amarillo")) {
            return 2;
        }
        if (n.equals("k") || n.equals("black") || n.equals("negro")) {
            return 3;
        }
        return -1;
    }

    static String normalize(String name) {
        return name.trim().toLowerCase(Locale.ROOT).replace('_', ' ').replace('-', ' ');
    }
}
