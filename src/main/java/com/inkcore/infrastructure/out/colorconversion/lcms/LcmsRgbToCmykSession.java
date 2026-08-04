package com.inkcore.infrastructure.out.colorconversion.lcms;

import com.inkcore.domain.colorconversion.model.RenderingIntent;
import com.inkcore.infrastructure.out.colorconversion.DeviceCmykColorSpace;

import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

/**
 * Sesión reutilizable RGB8→CMYK8 (una transformación LittleCMS abierta).
 * No es thread-safe; usar una por hilo / petición de estimación.
 */
public final class LcmsRgbToCmykSession implements AutoCloseable {

    private final LcmsTransform transform;
    private final byte[] pixelIn = new byte[3];
    private final byte[] pixelOut = new byte[4];
    private boolean closed;

    LcmsRgbToCmykSession(LcmsTransform transform) {
        this.transform = transform;
    }

    /**
     * Convierte un buffer RGB8 intercalado (N píxeles) a CMYK8 reutilizando la transformación.
     */
    public void transformRgb8ToCmyk8(byte[] rgbInterleaved, byte[] cmykInterleaved, int pixelCount) {
        ensureOpen();
        if (pixelCount <= 0) {
            return;
        }
        transform.transform(rgbInterleaved, cmykInterleaved, pixelCount);
    }

    public float[] rgbToCmyk01(float r, float g, float b) {
        ensureOpen();
        pixelIn[0] = (byte) clamp255(Math.round(r * 255f));
        pixelIn[1] = (byte) clamp255(Math.round(g * 255f));
        pixelIn[2] = (byte) clamp255(Math.round(b * 255f));
        transform.transform(pixelIn, pixelOut, 1);
        return new float[]{
                (pixelOut[0] & 0xff) / 255f,
                (pixelOut[1] & 0xff) / 255f,
                (pixelOut[2] & 0xff) / 255f,
                (pixelOut[3] & 0xff) / 255f
        };
    }

    /**
     * Convierte imagen RGB a CMYK 8-bit reutilizando la misma transformación nativa.
     */
    public BufferedImage rgbToCmykImage(BufferedImage rgb) {
        ensureOpen();
        int w = rgb.getWidth();
        int h = rgb.getHeight();
        byte[] in = extractRgb8(rgb);
        byte[] out = new byte[w * h * 4];
        transform.transform(in, out, w * h);
        return createCmyk8(w, h, out);
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        transform.close();
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Sesión LittleCMS RGB→CMYK ya cerrada");
        }
    }

    private static byte[] extractRgb8(BufferedImage source) {
        int w = source.getWidth();
        int h = source.getHeight();
        if (source.getType() == BufferedImage.TYPE_3BYTE_BGR) {
            // BGR interleaved → RGB
            byte[] bgr = ((DataBufferByte) source.getRaster().getDataBuffer()).getData();
            byte[] rgb = new byte[w * h * 3];
            for (int i = 0, o = 0; i + 2 < bgr.length && o + 2 < rgb.length; i += 3, o += 3) {
                rgb[o] = bgr[i + 2];
                rgb[o + 1] = bgr[i + 1];
                rgb[o + 2] = bgr[i];
            }
            return rgb;
        }
        BufferedImage rgb = source;
        if (source.getType() != BufferedImage.TYPE_INT_RGB && source.getType() != BufferedImage.TYPE_INT_ARGB) {
            rgb = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics g = rgb.getGraphics();
            try {
                g.drawImage(source, 0, 0, null);
            } finally {
                g.dispose();
            }
        }
        int[] pixels = rgb.getRGB(0, 0, w, h, null, 0, w);
        byte[] out = new byte[w * h * 3];
        for (int i = 0, o = 0; i < pixels.length; i++) {
            int p = pixels[i];
            out[o++] = (byte) ((p >> 16) & 0xff);
            out[o++] = (byte) ((p >> 8) & 0xff);
            out[o++] = (byte) (p & 0xff);
        }
        return out;
    }

    private static BufferedImage createCmyk8(int w, int h, byte[] interleaved) {
        ComponentColorModel model = new ComponentColorModel(
                DeviceCmykColorSpace.INSTANCE,
                new int[]{8, 8, 8, 8},
                false,
                false,
                Transparency.OPAQUE,
                DataBuffer.TYPE_BYTE
        );
        WritableRaster raster = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, w, h, 4, null);
        byte[] bank = ((DataBufferByte) raster.getDataBuffer()).getData();
        System.arraycopy(interleaved, 0, bank, 0, Math.min(interleaved.length, bank.length));
        return new BufferedImage(model, raster, false, null);
    }

    private static int clamp255(int v) {
        if (v < 0) {
            return 0;
        }
        return Math.min(255, v);
    }
}
