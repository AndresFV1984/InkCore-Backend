package com.inkcore.infrastructure.out.inkestimation;

import com.inkcore.domain.colorconversion.model.RenderingIntent;
import com.inkcore.domain.inkestimation.exception.InkEstimationFailedException;
import com.inkcore.infrastructure.out.colorconversion.IccProfileLoader;
import com.inkcore.infrastructure.out.colorconversion.lcms.LcmsRgbToCmykSession;
import com.inkcore.infrastructure.out.colorconversion.lcms.LittleCmsColorConverter;
import com.inkcore.infrastructure.out.colorconversion.lcms.LcmsNativeUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.util.HashMap;
import java.util.Map;

/**
 * RGB→CMYK orientado a estimación: sesión LittleCMS reutilizable + downscale + BPC.
 * La media CMYK muestrea los mismos puntos que antes, pero solo convierte esos píxeles (misma calidad, menos CPU).
 */
final class FastInkRgbToCmyk implements RgbToCmykConverter, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(FastInkRgbToCmyk.class);

    static final int DEFAULT_IMAGE_MAX_EDGE = 1024;
    static final int DEFAULT_RASTER_MAX_PIXELS = 2_000_000;

    private final LcmsRgbToCmykSession session;
    private final Map<Integer, float[]> colorCache = new HashMap<>(4096);
    private final int imageMaxEdge;
    private boolean closed;

    FastInkRgbToCmyk(
            IccProfileLoader loader,
            LittleCmsColorConverter littleCms,
            String destinationIccProfile,
            int imageMaxEdge,
            boolean blackPointCompensation
    ) {
        if (littleCms == null || !littleCms.isNativeAvailable()) {
            throw new LcmsNativeUnavailableException(
                    "LittleCMS (lcms2) no está disponible para estimación de tinta. "
                            + "Verifique que las nativas estén empaquetadas en el JAR."
            );
        }
        this.imageMaxEdge = Math.max(64, imageMaxEdge);
        byte[] srgbIcc;
        byte[] cmykIcc;
        try {
            srgbIcc = loader.loadProfileBytes("sRGB.icc");
            cmykIcc = loader.loadProfileBytes(destinationIccProfile);
        } catch (Exception ex) {
            throw new InkEstimationFailedException(
                    "Perfiles ICC no disponibles para estimación: " + ex.getMessage(), ex
            );
        }
        if (srgbIcc == null || srgbIcc.length == 0 || cmykIcc == null || cmykIcc.length == 0) {
            throw new InkEstimationFailedException(
                    "Perfiles ICC vacíos o ausentes para estimación (sRGB / " + destinationIccProfile + ")"
            );
        }
        this.session = littleCms.openRgbToCmykSession(
                srgbIcc, cmykIcc, RenderingIntent.PERCEPTUAL, blackPointCompensation
        );
        log.debug(
                "Estimación RGB→CMYK lista (sesión LittleCMS, BPC={}, maxEdge={})",
                blackPointCompensation, this.imageMaxEdge
        );
    }

    FastInkRgbToCmyk(
            IccProfileLoader loader,
            LittleCmsColorConverter littleCms,
            String destinationIccProfile,
            int imageMaxEdge
    ) {
        this(loader, littleCms, destinationIccProfile, imageMaxEdge, true);
    }

    FastInkRgbToCmyk(IccProfileLoader loader, LittleCmsColorConverter littleCms, String destinationIccProfile) {
        this(loader, littleCms, destinationIccProfile, DEFAULT_IMAGE_MAX_EDGE, true);
    }

    String colorEngine() {
        return LittleCmsColorConverter.ENGINE_LITTLECMS;
    }

    boolean isLcmsReady() {
        return true;
    }

    /**
     * CMYK 0–1. El array devuelto es de solo lectura (caché interna); no mutar.
     */
    @Override
    public float[] toCmyk(float r, float g, float b) {
        ensureOpen();
        int ri = clamp255(Math.round(r * 255f));
        int gi = clamp255(Math.round(g * 255f));
        int bi = clamp255(Math.round(b * 255f));
        int key = (ri << 16) | (gi << 8) | bi;
        float[] cached = colorCache.get(key);
        if (cached != null) {
            return cached;
        }
        float[] cmyk = session.rgbToCmyk01(ri / 255f, gi / 255f, bi / 255f);
        if (colorCache.size() < 65_536) {
            colorCache.put(key, cmyk);
        }
        return cmyk;
    }

    @Override
    public double[] meanCmyk01FromRgbImage(BufferedImage rgb) {
        return meanCmyk01(rgb, true);
    }

    /**
     * Media CMYK 0–1. Si {@code applyMaxEdge}, reduce al lado máximo configurado (PDF embebidas).
     * Misma grilla de muestreo que {@link #meanChannels01}: convierte solo esos píxeles.
     */
    double[] meanCmyk01(BufferedImage rgb, boolean applyMaxEdge) {
        ensureOpen();
        if (rgb == null || rgb.getWidth() <= 0 || rgb.getHeight() <= 0) {
            return new double[]{0, 0, 0, 0};
        }
        BufferedImage scaled = applyMaxEdge ? scaleDown(rgb, imageMaxEdge) : rgb;
        BufferedImage working = toIntRgb(scaled);
        return meanFromRgbSamples(working);
    }

    /**
     * Prepara raster RGB para cobertura: solo escala (sin convertir toda la imagen).
     * Usar {@link #meanCmyk01(BufferedImage, boolean)} para la media.
     */
    BufferedImage prepareRgbForCoverage(BufferedImage source, int maxPixels) {
        return toIntRgb(scaleToMaxPixels(source, maxPixels));
    }

    private double[] meanFromRgbSamples(BufferedImage rgb) {
        int w = rgb.getWidth();
        int h = rgb.getHeight();
        int step = sampleStep(w, h);
        int samplesX = (w + step - 1) / step;
        int samplesY = (h + step - 1) / step;
        int count = samplesX * samplesY;
        if (count <= 0) {
            return new double[]{0, 0, 0, 0};
        }
        byte[] in = new byte[count * 3];
        int o = 0;
        for (int y = 0; y < h; y += step) {
            for (int x = 0; x < w; x += step) {
                int px = rgb.getRGB(x, y);
                in[o++] = (byte) ((px >> 16) & 0xff);
                in[o++] = (byte) ((px >> 8) & 0xff);
                in[o++] = (byte) (px & 0xff);
            }
        }
        byte[] out = new byte[count * 4];
        session.transformRgb8ToCmyk8(in, out, count);
        double sumC = 0;
        double sumM = 0;
        double sumY = 0;
        double sumK = 0;
        for (int i = 0; i < count; i++) {
            int base = i * 4;
            sumC += (out[base] & 0xff) / 255.0;
            sumM += (out[base + 1] & 0xff) / 255.0;
            sumY += (out[base + 2] & 0xff) / 255.0;
            sumK += (out[base + 3] & 0xff) / 255.0;
        }
        return new double[]{sumC / count, sumM / count, sumY / count, sumK / count};
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        session.close();
        colorCache.clear();
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("FastInkRgbToCmyk ya cerrado");
        }
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
        if (source.getType() == BufferedImage.TYPE_INT_RGB) {
            return source;
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

    static double[] meanChannels01(WritableRaster raster) {
        int w = raster.getWidth();
        int h = raster.getHeight();
        int bands = Math.min(4, raster.getNumBands());
        int step = sampleStep(w, h);
        double[] sum = new double[4];
        long counted = 0;
        int[] px = new int[Math.max(4, bands)];
        int max = raster.getDataBuffer().getDataType() == DataBuffer.TYPE_USHORT ? 65535 : 255;
        for (int y = 0; y < h; y += step) {
            for (int x = 0; x < w; x += step) {
                raster.getPixel(x, y, px);
                for (int b = 0; b < 4; b++) {
                    sum[b] += b < bands ? (px[b] / (double) max) : 0;
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
            return 3;
        }
        return 4;
    }

    private static int clamp255(int v) {
        if (v < 0) {
            return 0;
        }
        return Math.min(255, v);
    }
}
