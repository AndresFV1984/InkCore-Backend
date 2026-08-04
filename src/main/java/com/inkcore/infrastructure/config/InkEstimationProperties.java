package com.inkcore.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "inkcore.ink-estimation")
public class InkEstimationProperties {

    /** Ancho de pliego por defecto (cm). */
    private double defaultWidthCm = 70.0;

    /** Alto de pliego por defecto (cm). */
    private double defaultHeightCm = 100.0;

    /** DPI de análisis por defecto (raster). PDF vectorial no depende de este DPI. */
    private int defaultDpi = 300;

    /**
     * Factor g/cm² de referencia al 100 % (eco API / override uniforme vía gramsPerCm2).
     */
    private double defaultGramsPerCm2AtFullCoverage = 0.00021;

    /**
     * Factores por canal (offset tipográfico libre, sin densitómetro de pago).
     * Si el cliente envía gramsPerCm2, se usa ese valor uniforme en todos los canales.
     */
    private ChannelGrams channelGramsPerCm2 = new ChannelGrams();

    /**
     * Techo absoluto de tamaño de archivo (bytes). Protección antiabuso.
     * Default 512 MiB.
     */
    private long absoluteMaxFileBytes = 512L * 1024L * 1024L;

    /**
     * Margen inferior vs tamaño teórico sin comprimir (0.01 = 1 %).
     */
    private double sizeToleranceMinRatio = 0.01;

    /**
     * Margen superior vs tamaño teórico sin comprimir (p. ej. 2.0 = 200 %).
     */
    private double sizeToleranceMaxRatio = 2.5;

    /** Perfil ICC para RGB→CMYK al analizar coberturas de proceso. */
    private String analysisIccProfile = "FOGRA39.icc";

    /**
     * Tope de píxeles al estimar cobertura de rasters (JPG/PNG/TIFF).
     * Imágenes mayores se reducen antes del ICC (estimación, no CTP).
     * Default BALANCED: 2_000_000.
     */
    private int maxAnalysisPixels = 2_000_000;

    /**
     * Lado máximo (px) al muestrear imágenes RGB embebidas en PDF.
     * Default BALANCED: 1024.
     */
    private int rgbImageMaxEdge = 1024;

    /**
     * Black Point Compensation (LittleCMS) en RGB→CMYK de estimación.
     * Independiente del flag de convert; default true.
     */
    private boolean blackPointCompensation = true;

    public double getDefaultWidthCm() {
        return defaultWidthCm;
    }

    public void setDefaultWidthCm(double defaultWidthCm) {
        this.defaultWidthCm = defaultWidthCm;
    }

    public double getDefaultHeightCm() {
        return defaultHeightCm;
    }

    public void setDefaultHeightCm(double defaultHeightCm) {
        this.defaultHeightCm = defaultHeightCm;
    }

    public int getDefaultDpi() {
        return defaultDpi;
    }

    public void setDefaultDpi(int defaultDpi) {
        this.defaultDpi = defaultDpi;
    }

    public double getDefaultGramsPerCm2AtFullCoverage() {
        return defaultGramsPerCm2AtFullCoverage;
    }

    public void setDefaultGramsPerCm2AtFullCoverage(double defaultGramsPerCm2AtFullCoverage) {
        this.defaultGramsPerCm2AtFullCoverage = defaultGramsPerCm2AtFullCoverage;
    }

    public ChannelGrams getChannelGramsPerCm2() {
        return channelGramsPerCm2;
    }

    public void setChannelGramsPerCm2(ChannelGrams channelGramsPerCm2) {
        this.channelGramsPerCm2 = channelGramsPerCm2 != null ? channelGramsPerCm2 : new ChannelGrams();
    }

    public long getAbsoluteMaxFileBytes() {
        return absoluteMaxFileBytes;
    }

    public void setAbsoluteMaxFileBytes(long absoluteMaxFileBytes) {
        this.absoluteMaxFileBytes = absoluteMaxFileBytes;
    }

    public double getSizeToleranceMinRatio() {
        return sizeToleranceMinRatio;
    }

    public void setSizeToleranceMinRatio(double sizeToleranceMinRatio) {
        this.sizeToleranceMinRatio = sizeToleranceMinRatio;
    }

    public double getSizeToleranceMaxRatio() {
        return sizeToleranceMaxRatio;
    }

    public void setSizeToleranceMaxRatio(double sizeToleranceMaxRatio) {
        this.sizeToleranceMaxRatio = sizeToleranceMaxRatio;
    }

    public String getAnalysisIccProfile() {
        return analysisIccProfile;
    }

    public void setAnalysisIccProfile(String analysisIccProfile) {
        this.analysisIccProfile = analysisIccProfile;
    }

    public int getMaxAnalysisPixels() {
        return maxAnalysisPixels;
    }

    public void setMaxAnalysisPixels(int maxAnalysisPixels) {
        this.maxAnalysisPixels = maxAnalysisPixels;
    }

    public int getRgbImageMaxEdge() {
        return rgbImageMaxEdge;
    }

    public void setRgbImageMaxEdge(int rgbImageMaxEdge) {
        this.rgbImageMaxEdge = rgbImageMaxEdge;
    }

    public boolean isBlackPointCompensation() {
        return blackPointCompensation;
    }

    public void setBlackPointCompensation(boolean blackPointCompensation) {
        this.blackPointCompensation = blackPointCompensation;
    }

    /**
     * Densidades por canal. Defaults relativos a ~0.00021 g/cm² offset Colombia.
     */
    public static class ChannelGrams {
        /** Cian */
        private double cyan = 0.00021;
        /** Magenta */
        private double magenta = 0.00021;
        /** Amarillo (suele depositar un poco menos) */
        private double yellow = 0.00020;
        /** Negro (suelen ser pastas algo más densas) */
        private double black = 0.00022;
        /** Spot / Pantone sólido */
        private double spot = 0.00025;

        public double getCyan() {
            return cyan;
        }

        public void setCyan(double cyan) {
            this.cyan = cyan;
        }

        public double getMagenta() {
            return magenta;
        }

        public void setMagenta(double magenta) {
            this.magenta = magenta;
        }

        public double getYellow() {
            return yellow;
        }

        public void setYellow(double yellow) {
            this.yellow = yellow;
        }

        public double getBlack() {
            return black;
        }

        public void setBlack(double black) {
            this.black = black;
        }

        public double getSpot() {
            return spot;
        }

        public void setSpot(double spot) {
            this.spot = spot;
        }
    }
}
