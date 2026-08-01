package com.inkcore.infrastructure.out.inkestimation;

import com.inkcore.infrastructure.out.colorconversion.IccProfileLoader;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.util.HashMap;
import java.util.Map;

/**
 * RGB→CMYK orientado a estimación de tinta (rápido): perfiles ICC cargados una vez,
 * conversión por ColorSpace (vectores) + ColorConvertOp por imagen (raster),
 * caché por RGB empaquetado. No aplica vibrance/soft-proof (innecesario para cobertura %).
 */
final class FastInkRgbToCmyk implements RgbToCmykConverter {

    /** Máximo lado al muestrear imágenes RGB embebidas en PDF. */
    static final int DEFAULT_IMAGE_MAX_EDGE = 512;

    /** Tope de píxeles al estimar raster de alta resolución. */
    static final int DEFAULT_RASTER_MAX_PIXELS = 1_000_000;

    private final ICC_ColorSpace srgbCs;
    private final ICC_ColorSpace cmykCs;
    private final ColorConvertOp imageOp;
    private final Map<Integer, float[]> colorCache = new HashMap<>(4096);
    private final int imageMaxEdge;

    FastInkRgbToCmyk(IccProfileLoader loader, String destinationIccProfile, int imageMaxEdge) {
        ICC_Profile srgb = ICC_Profile.getInstance(loader.loadProfileBytes("sRGB.icc"));
        ICC_Profile cmyk = ICC_Profile.getInstance(loader.loadProfileBytes(destinationIccProfile));
        this.srgbCs = new ICC_ColorSpace(srgb);
        this.cmykCs = new ICC_ColorSpace(cmyk);
        this.imageOp = new ColorConvertOp(srgbCs, cmykCs, null);
        this.imageMaxEdge = Math.max(64, imageMaxEdge);
    }

    FastInkRgbToCmyk(IccProfileLoader loader, String destinationIccProfile) {
        this(loader, destinationIccProfile, DEFAULT_IMAGE_MAX_EDGE);
    }

    @Override
    public float[] toCmyk(float r, float g, float b) {
        int ri = clamp255(Math.round(r * 255f));
        int gi = clamp255(Math.round(g * 255f));
        int bi = clamp255(Math.round(b * 255f));
        int key = (ri << 16) | (gi << 8) | bi;
        float[] cached = colorCache.get(key);
        if (cached != null) {
            return cached.clone();
        }
        float[] xyz = srgbCs.toCIEXYZ(new float[]{ri / 255f, gi / 255f, bi / 255f});
        float[] cmyk = cmykCs.fromCIEXYZ(xyz);
        for (int i = 0; i < 4; i++) {
            cmyk[i] = clamp01(cmyk[i]);
        }
        if (colorCache.size() < 65_536) {
            colorCache.put(key, cmyk);
        }
        return cmyk.clone();
    }

    @Override
    public double[] meanCmyk01FromRgbImage(BufferedImage rgb) {
        return meanCmyk01(rgb);
    }

    /**
     * Cobertura media C/M/Y/K (0–1) de una imagen RGB, con downscale + una sola conversión ICC.
     */
    double[] meanCmyk01(BufferedImage rgb) {
        if (rgb == null || rgb.getWidth() <= 0 || rgb.getHeight() <= 0) {
            return new double[]{0, 0, 0, 0};
        }
        BufferedImage scaled = scaleDown(rgb, imageMaxEdge);
        BufferedImage working = toIntRgb(scaled);
        BufferedImage cmyk = createCmykImage(working.getWidth(), working.getHeight());
        imageOp.filter(working, cmyk);
        return meanChannels01(cmyk.getRaster());
    }

    /**
     * Prepara un raster grande para estimación: downscale por tope de píxeles + ICC si no es CMYK.
     * CMYK nativo no se remuestrea por Graphics2D (perdería canales); el mean ya usa step.
     */
    BufferedImage prepareRasterForCoverage(BufferedImage source, boolean alreadyCmyk, int maxPixels) {
        if (alreadyCmyk) {
            return source;
        }
        BufferedImage scaled = scaleToMaxPixels(source, maxPixels);
        BufferedImage working = toIntRgb(scaled);
        BufferedImage cmyk = createCmykImage(working.getWidth(), working.getHeight());
        imageOp.filter(working, cmyk);
        return cmyk;
    }

    private static BufferedImage scaleDown(BufferedImage source, int maxEdge) {
        int w = source.getWidth();
        int h = source.getHeight();
        int max = Math.max(w, h);
        if (max <= maxEdge) {
            return source;
        }
        double scale = (double) maxEdge / max;
        int nw = Math.max(1, (int) Math.round(w * scale));
        int nh = Math.max(1, (int) Math.round(h * scale));
        return scaleTo(source, nw, nh);
    }

    static BufferedImage scaleToMaxPixels(BufferedImage source, int maxPixels) {
        long pixels = (long) source.getWidth() * source.getHeight();
        if (pixels <= maxPixels) {
            return source;
        }
        double scale = Math.sqrt((double) maxPixels / pixels);
        int nw = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int nh = Math.max(1, (int) Math.round(source.getHeight() * scale));
        return scaleTo(source, nw, nh);
    }

    private static BufferedImage scaleTo(BufferedImage source, int nw, int nh) {
        BufferedImage out = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(source, 0, 0, nw, nh, null);
        } finally {
            g.dispose();
        }
        return out;
    }

    private static BufferedImage toIntRgb(BufferedImage source) {
        if (source.getType() == BufferedImage.TYPE_INT_RGB || source.getType() == BufferedImage.TYPE_INT_ARGB) {
            if (source.getType() == BufferedImage.TYPE_INT_RGB) {
                return source;
            }
        }
        BufferedImage rgb = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        try {
            g.drawImage(source, 0, 0, null);
        } finally {
            g.dispose();
        }
        return rgb;
    }

    private BufferedImage createCmykImage(int w, int h) {
        int[] bits = {8, 8, 8, 8};
        ComponentColorModel model = new ComponentColorModel(
                cmykCs, bits, false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE
        );
        WritableRaster raster = model.createCompatibleWritableRaster(w, h);
        return new BufferedImage(model, raster, false, null);
    }

    static double[] meanChannels01(WritableRaster raster) {
        int w = raster.getWidth();
        int h = raster.getHeight();
        int bands = Math.min(4, raster.getNumBands());
        boolean ushort = raster.getDataBuffer().getDataType() == DataBuffer.TYPE_USHORT;
        double max = ushort ? 65535.0 : 255.0;
        int step = sampleStep(w, h);
        double[] sum = new double[4];
        long counted = 0;
        int[] pixel = new int[Math.max(4, bands)];
        for (int y = 0; y < h; y += step) {
            for (int x = 0; x < w; x += step) {
                raster.getPixel(x, y, pixel);
                for (int b = 0; b < 4; b++) {
                    sum[b] += (b < bands ? pixel[b] : 0) / max;
                }
                counted++;
            }
        }
        if (counted == 0) {
            return new double[]{0, 0, 0, 0};
        }
        return new double[]{sum[0] / counted, sum[1] / counted, sum[2] / counted, sum[3] / counted};
    }

    static int sampleStep(int w, int h) {
        long pixels = (long) w * h;
        if (pixels <= 250_000L) {
            return 1;
        }
        if (pixels <= 1_000_000L) {
            return 2;
        }
        if (pixels <= 4_000_000L) {
            return 4;
        }
        return Math.max(8, (int) Math.ceil(Math.sqrt(pixels / 250_000.0)));
    }

    private static float clamp01(float v) {
        if (v < 0f) {
            return 0f;
        }
        if (v > 1f) {
            return 1f;
        }
        return v;
    }

    private static int clamp255(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
