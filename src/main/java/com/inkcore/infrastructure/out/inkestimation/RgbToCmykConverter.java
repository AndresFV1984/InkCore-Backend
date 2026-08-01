package com.inkcore.infrastructure.out.inkestimation;

@FunctionalInterface
interface RgbToCmykConverter {
    float[] toCmyk(float r, float g, float b);

    /**
     * Cobertura media CMYK (0–1) de una imagen RGB. Default: muestreo + toCmyk (lento).
     * {@link FastInkRgbToCmyk} lo sobrescribe con conversión por lote.
     */
    default double[] meanCmyk01FromRgbImage(java.awt.image.BufferedImage rgb) {
        if (rgb == null || rgb.getWidth() <= 0 || rgb.getHeight() <= 0) {
            return new double[]{0, 0, 0, 0};
        }
        int w = rgb.getWidth();
        int h = rgb.getHeight();
        int step = FastInkRgbToCmyk.sampleStep(w, h);
        double[] sum = new double[4];
        long counted = 0;
        for (int y = 0; y < h; y += step) {
            for (int x = 0; x < w; x += step) {
                int argb = rgb.getRGB(x, y);
                float[] cmyk = toCmyk(
                        ((argb >> 16) & 0xFF) / 255f,
                        ((argb >> 8) & 0xFF) / 255f,
                        (argb & 0xFF) / 255f
                );
                for (int i = 0; i < 4; i++) {
                    sum[i] += cmyk[i];
                }
                counted++;
            }
        }
        if (counted == 0) {
            return new double[]{0, 0, 0, 0};
        }
        return new double[]{sum[0] / counted, sum[1] / counted, sum[2] / counted, sum[3] / counted};
    }
}
