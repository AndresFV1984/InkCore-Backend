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
     * Empuje hacia blanco en RGB (0–0.20) antes de convertir a CMYK.
     * Default comercial 0.12. CTP: 0 vía env o qualityPreset=FIDELITY.
     */
    private float brightnessLift = 0.12f;

    /**
     * Refuerzo de saturación HSB en RGB (0–0.35) antes del CMYK.
     * Default comercial 0.28. CTP: 0.
     */
    private float vibranceBoost = 0.28f;

    /**
     * Si true, recupera brillo/croma tras soft-proof.
     * Default true (comercial). CTP: false.
     */
    private boolean softProofBrightnessMatch = true;

    /**
     * Black Point Compensation (LittleCMS). Recomendado true para acercarse a Photoshop
     * con intent Relative Colorimetric; también útil con Perceptual según perfil.
     */
    private boolean blackPointCompensation = true;

    /**
     * Override opcional a la nativa lcms2. Por defecto JNA carga desde classpath
     * ({@code win32-x86-64/lcms2.dll} / {@code linux-x86-64/liblcms2.so}).
     * Ej. {@code C:/Tools/lcms2.dll}. Vacío = classpath / PATH / sistema.
     */
    private String lcmsLibraryPath = "";

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

    public boolean isBlackPointCompensation() {
        return blackPointCompensation;
    }

    public void setBlackPointCompensation(boolean blackPointCompensation) {
        this.blackPointCompensation = blackPointCompensation;
    }

    public String getLcmsLibraryPath() {
        return lcmsLibraryPath;
    }

    public void setLcmsLibraryPath(String lcmsLibraryPath) {
        this.lcmsLibraryPath = lcmsLibraryPath;
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
