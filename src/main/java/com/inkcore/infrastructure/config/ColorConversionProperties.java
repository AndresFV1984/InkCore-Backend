package com.inkcore.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "inkcore.color-conversion")
public class ColorConversionProperties {

    /**
     * Nombre del perfil ICC de origen en classpath:/color-profiles/ (ej. sRGB.icc).
     */
    private String sourceIccProfile = "sRGB.icc";

    /**
     * Nombre del perfil ICC de destino CMYK (ej. FOGRA39.icc). Debe colocarse manualmente por licencia.
     */
    private String destinationIccProfile = "FOGRA39.icc";

    /**
     * Conversión PDF habilitada (Apache PDFBox + ICC, sin Ghostscript).
     * Se puede apagar con {@code false} si se desea rechazar PDF.
     */
    private boolean pdfEnabled = true;

    /**
     * Resolución de rasterizado de páginas PDF cuando no se puede extraer la imagen embebida (dpi).
     * Más alto = más nitidez en vectores/texto (más CPU/memoria). Default 600.
     */
    private int minImageResolutionDpi = 600;

    /**
     * Si true, en PDFs con una sola imagen a página completa se extrae a resolución nativa
     * (sin re-rasterizar), preservando nitidez y color.
     */
    private boolean preferEmbeddedPdfImages = true;

    /**
     * Empuje hacia blanco en RGB (0–0.15) antes de convertir a CMYK.
     * Default 0 (fidelidad CTP): no altera el original. Solo 0.02–0.05 si la prensa oscurece.
     */
    private float brightnessLift = 0f;

    /**
     * Refuerzo de saturación HSB en RGB (0–0.25) antes del CMYK.
     * Default 0 (fidelidad CTP): no altera el original. Valores &gt;0 son creativos, no colorimétricos.
     */
    private float vibranceBoost = 0f;

    /**
     * Si true, escala tinta CMYK tras soft-proof para igualar luma percibida.
     * Default false: la conversión ICC pura no se retoca (mejor para CTP/RIP).
     */
    private boolean softProofBrightnessMatch = false;

    private final IccCache iccCache = new IccCache();

    public String getSourceIccProfile() {
        return sourceIccProfile;
    }

    public void setSourceIccProfile(String sourceIccProfile) {
        this.sourceIccProfile = sourceIccProfile;
    }

    public String getDestinationIccProfile() {
        return destinationIccProfile;
    }

    public void setDestinationIccProfile(String destinationIccProfile) {
        this.destinationIccProfile = destinationIccProfile;
    }

    public boolean isPdfEnabled() {
        return pdfEnabled;
    }

    public void setPdfEnabled(boolean pdfEnabled) {
        this.pdfEnabled = pdfEnabled;
    }

    public int getMinImageResolutionDpi() {
        return minImageResolutionDpi;
    }

    public void setMinImageResolutionDpi(int minImageResolutionDpi) {
        this.minImageResolutionDpi = minImageResolutionDpi;
    }

    public boolean isPreferEmbeddedPdfImages() {
        return preferEmbeddedPdfImages;
    }

    public void setPreferEmbeddedPdfImages(boolean preferEmbeddedPdfImages) {
        this.preferEmbeddedPdfImages = preferEmbeddedPdfImages;
    }

    public float getBrightnessLift() {
        return brightnessLift;
    }

    public void setBrightnessLift(float brightnessLift) {
        this.brightnessLift = brightnessLift;
    }

    public float getVibranceBoost() {
        return vibranceBoost;
    }

    public void setVibranceBoost(float vibranceBoost) {
        this.vibranceBoost = vibranceBoost;
    }

    public boolean isSoftProofBrightnessMatch() {
        return softProofBrightnessMatch;
    }

    public void setSoftProofBrightnessMatch(boolean softProofBrightnessMatch) {
        this.softProofBrightnessMatch = softProofBrightnessMatch;
    }

    public IccCache getIccCache() {
        return iccCache;
    }

    public static class IccCache {
        /** memory | redis */
        private String type = "memory";
        private long ttlSeconds = 86_400;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public long getTtlSeconds() {
            return ttlSeconds;
        }

        public void setTtlSeconds(long ttlSeconds) {
            this.ttlSeconds = ttlSeconds;
        }
    }
}
