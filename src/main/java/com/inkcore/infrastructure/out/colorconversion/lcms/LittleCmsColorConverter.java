package com.inkcore.infrastructure.out.colorconversion.lcms;

import com.inkcore.domain.colorconversion.model.RenderingIntent;
import com.inkcore.infrastructure.config.ColorConversionProperties;
import com.inkcore.infrastructure.out.colorconversion.DeviceCmykColorSpace;
import org.springframework.stereotype.Component;

import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.Objects;

/**
 * Servicio único de conversión de color LittleCMS (RGB↔CMYK) con BPC opcional.
 * Consumido por CTP, estimación de tintas y soft-proof.
 * <p>
 * Thread-safe: cada transformación nativa se crea y destruye por llamada (handles no compartidos).
 */
@Component
public class LittleCmsColorConverter {

    public static final String ENGINE_LITTLECMS = "littlecms";
    public static final String ENGINE_FALLBACK_NAIVE = "fallback-naive";

    private final ColorConversionProperties properties;

    public LittleCmsColorConverter(ColorConversionProperties properties) {
        this.properties = properties;
        // Precarga best-effort (no falla el arranque si falta la DLL en local)
        LcmsNativeLoader.tryLoad(properties.getLcmsLibraryPath());
    }

    public boolean isNativeAvailable() {
        return LcmsNativeLoader.isAvailable(properties.getLcmsLibraryPath());
    }

    /**
     * RGB {@link BufferedImage} → CMYK 8-bit (DeviceCmyk ColorModel para escritura TIFF).
     * Exige LittleCMS; no degrada silenciosamente.
     */
    public BufferedImage rgbToCmykImage(
            BufferedImage rgb,
            byte[] sourceIcc,
            byte[] destinationIcc,
            RenderingIntent intent,
            Boolean blackPointCompensation
    ) {
        Objects.requireNonNull(rgb, "rgb");
        int w = rgb.getWidth();
        int h = rgb.getHeight();
        boolean use16 = sampleBits(rgb) >= 16;
        byte[] in = extractRgbInterleaved(rgb, use16);
        byte[] out = new byte[w * h * 4 * (use16 ? 2 : 1)];
        transform(
                in, out, w * h,
                sourceIcc, destinationIcc,
                use16 ? Lcms2Library.TYPE_RGB_16 : Lcms2Library.TYPE_RGB_8,
                use16 ? Lcms2Library.TYPE_CMYK_16 : Lcms2Library.TYPE_CMYK_8,
                intent, blackPointCompensation
        );
        return createCmykImage(w, h, out, use16);
    }

    /**
     * Soft-proof: CMYK → RGB sRGB (TYPE_INT_RGB).
     */
    public BufferedImage cmykToRgbImage(
            BufferedImage cmyk,
            byte[] cmykIcc,
            byte[] rgbIcc,
            RenderingIntent intent,
            Boolean blackPointCompensation
    ) {
        Objects.requireNonNull(cmyk, "cmyk");
        int w = cmyk.getWidth();
        int h = cmyk.getHeight();
        boolean use16 = sampleBits(cmyk) >= 16;
        byte[] in = extractCmykInterleaved(cmyk, use16);
        byte[] out = new byte[w * h * 3 * (use16 ? 2 : 1)];
        transform(
                in, out, w * h,
                cmykIcc, rgbIcc,
                use16 ? Lcms2Library.TYPE_CMYK_16 : Lcms2Library.TYPE_CMYK_8,
                use16 ? Lcms2Library.TYPE_RGB_16 : Lcms2Library.TYPE_RGB_8,
                intent, blackPointCompensation
        );
        return createRgbImage(w, h, out, use16);
    }

    /**
     * Un píxel RGB 0–1 → CMYK 0–1 (estimación de tintas).
     */
    public float[] rgbToCmyk01(
            float r, float g, float b,
            byte[] sourceIcc,
            byte[] destinationIcc,
            RenderingIntent intent,
            Boolean blackPointCompensation
    ) {
        byte[] in = new byte[]{
                (byte) clamp255(Math.round(r * 255f)),
                (byte) clamp255(Math.round(g * 255f)),
                (byte) clamp255(Math.round(b * 255f))
        };
        byte[] out = new byte[4];
        transform(
                in, out, 1,
                sourceIcc, destinationIcc,
                Lcms2Library.TYPE_RGB_8, Lcms2Library.TYPE_CMYK_8,
                intent, blackPointCompensation
        );
        return new float[]{
                (out[0] & 0xff) / 255f,
                (out[1] & 0xff) / 255f,
                (out[2] & 0xff) / 255f,
                (out[3] & 0xff) / 255f
        };
    }

    /**
     * Abre una sesión RGB8→CMYK8 reutilizable (estimación de tintas / muchos colores).
     * El caller debe cerrarla ({@code try-with-resources}).
     */
    public LcmsRgbToCmykSession openRgbToCmykSession(
            byte[] sourceIcc,
            byte[] destinationIcc,
            RenderingIntent intent,
            Boolean blackPointCompensation
    ) {
        Lcms2Library lib = LcmsNativeLoader.tryLoad(properties.getLcmsLibraryPath());
        if (lib == null) {
            throw new LcmsNativeUnavailableException(
                    "LittleCMS (lcms2) no está disponible en este host. "
                            + "Instale la biblioteca nativa antes de convertir color."
            );
        }
        int cmsIntent = toCmsIntent(intent != null ? intent : RenderingIntent.PERCEPTUAL);
        boolean bpc = blackPointCompensation != null
                ? blackPointCompensation
                : properties.isBlackPointCompensation();
        int flags = Lcms2Library.cmsFLAGS_NOCACHE;
        if (bpc) {
            flags |= Lcms2Library.cmsFLAGS_BLACKPOINTCOMPENSATION;
        }
        LcmsTransform tx = new LcmsTransform(
                lib, sourceIcc, destinationIcc,
                Lcms2Library.TYPE_RGB_8, Lcms2Library.TYPE_CMYK_8,
                cmsIntent, flags
        );
        return new LcmsRgbToCmykSession(tx);
    }

    public void transform(
            byte[] input,
            byte[] output,
            int pixelCount,
            byte[] inputIcc,
            byte[] outputIcc,
            int inputFormat,
            int outputFormat,
            RenderingIntent intent,
            Boolean blackPointCompensation
    ) {
        Lcms2Library lib = LcmsNativeLoader.tryLoad(properties.getLcmsLibraryPath());
        if (lib == null) {
            throw new LcmsNativeUnavailableException(
                    "LittleCMS (lcms2) no está disponible en este host. "
                            + "Instale la biblioteca nativa antes de convertir color."
            );
        }
        int cmsIntent = toCmsIntent(intent != null ? intent : RenderingIntent.PERCEPTUAL);
        boolean bpc = blackPointCompensation != null
                ? blackPointCompensation
                : properties.isBlackPointCompensation();
        int flags = Lcms2Library.cmsFLAGS_NOCACHE;
        if (bpc) {
            flags |= Lcms2Library.cmsFLAGS_BLACKPOINTCOMPENSATION;
        }
        try (LcmsTransform tx = new LcmsTransform(
                lib, inputIcc, outputIcc, inputFormat, outputFormat, cmsIntent, flags
        )) {
            tx.transform(input, output, pixelCount);
        }
    }

    static int toCmsIntent(RenderingIntent intent) {
        return switch (intent) {
            case RELATIVE_COLORIMETRIC -> Lcms2Library.INTENT_RELATIVE_COLORIMETRIC;
            case PERCEPTUAL -> Lcms2Library.INTENT_PERCEPTUAL;
        };
    }

    private static int sampleBits(BufferedImage image) {
        try {
            return image.getSampleModel().getSampleSize(0);
        } catch (Exception ex) {
            return 8;
        }
    }

    private static byte[] extractRgbInterleaved(BufferedImage source, boolean use16) {
        int w = source.getWidth();
        int h = source.getHeight();
        BufferedImage rgb = source;
        if (source.getType() != BufferedImage.TYPE_INT_RGB
                && source.getType() != BufferedImage.TYPE_INT_ARGB
                && source.getType() != BufferedImage.TYPE_3BYTE_BGR) {
            rgb = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            rgb.getGraphics().drawImage(source, 0, 0, null);
        }
        int[] pixels = rgb.getRGB(0, 0, w, h, null, 0, w);
        if (!use16) {
            byte[] out = new byte[w * h * 3];
            for (int i = 0, o = 0; i < pixels.length; i++) {
                int p = pixels[i];
                out[o++] = (byte) ((p >> 16) & 0xff);
                out[o++] = (byte) ((p >> 8) & 0xff);
                out[o++] = (byte) (p & 0xff);
            }
            return out;
        }
        byte[] out = new byte[w * h * 6];
        for (int i = 0, o = 0; i < pixels.length; i++) {
            int p = pixels[i];
            int r = (p >> 16) & 0xff;
            int g = (p >> 8) & 0xff;
            int b = p & 0xff;
            // 8→16 expandido (little-endian)
            out[o++] = 0;
            out[o++] = (byte) r;
            out[o++] = 0;
            out[o++] = (byte) g;
            out[o++] = 0;
            out[o++] = (byte) b;
        }
        return out;
    }

    private static byte[] extractCmykInterleaved(BufferedImage cmyk, boolean use16) {
        int w = cmyk.getWidth();
        int h = cmyk.getHeight();
        WritableRaster raster = cmyk.getRaster();
        int bands = Math.min(4, raster.getNumBands());
        if (!use16) {
            byte[] out = new byte[w * h * 4];
            int[] px = new int[Math.max(4, bands)];
            int o = 0;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    raster.getPixel(x, y, px);
                    out[o++] = (byte) clamp255(px[0]);
                    out[o++] = (byte) clamp255(px[1]);
                    out[o++] = (byte) clamp255(px[2]);
                    out[o++] = (byte) clamp255(bands > 3 ? px[3] : 0);
                }
            }
            return out;
        }
        byte[] out = new byte[w * h * 8];
        int[] px = new int[Math.max(4, bands)];
        int o = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                raster.getPixel(x, y, px);
                for (int b = 0; b < 4; b++) {
                    int v = b < bands ? px[b] : 0;
                    if (v > 65535) {
                        v = 65535;
                    }
                    out[o++] = (byte) (v & 0xff);
                    out[o++] = (byte) ((v >> 8) & 0xff);
                }
            }
        }
        return out;
    }

    private static BufferedImage createCmykImage(int w, int h, byte[] interleaved, boolean use16) {
        int dataType = use16 ? DataBuffer.TYPE_USHORT : DataBuffer.TYPE_BYTE;
        int[] bits = use16 ? new int[]{16, 16, 16, 16} : new int[]{8, 8, 8, 8};
        ComponentColorModel model = new ComponentColorModel(
                DeviceCmykColorSpace.INSTANCE,
                bits,
                false,
                false,
                Transparency.OPAQUE,
                dataType
        );
        WritableRaster raster = Raster.createInterleavedRaster(
                dataType, w, h, 4, null
        );
        if (!use16) {
            byte[] bank = ((DataBufferByte) raster.getDataBuffer()).getData();
            System.arraycopy(interleaved, 0, bank, 0, Math.min(interleaved.length, bank.length));
        } else {
            short[] bank = ((DataBufferUShort) raster.getDataBuffer()).getData();
            for (int i = 0, o = 0; o < bank.length && i + 1 < interleaved.length; o++) {
                int lo = interleaved[i++] & 0xff;
                int hi = interleaved[i++] & 0xff;
                bank[o] = (short) ((hi << 8) | lo);
            }
        }
        return new BufferedImage(model, raster, false, null);
    }

    private static BufferedImage createRgbImage(int w, int h, byte[] interleaved, boolean use16) {
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        int[] pixels = new int[w * h];
        if (!use16) {
            for (int i = 0, p = 0; p < pixels.length; p++) {
                int r = interleaved[i++] & 0xff;
                int g = interleaved[i++] & 0xff;
                int b = interleaved[i++] & 0xff;
                pixels[p] = (r << 16) | (g << 8) | b;
            }
        } else {
            for (int i = 0, p = 0; p < pixels.length; p++) {
                int r = ((interleaved[i++] & 0xff) | ((interleaved[i++] & 0xff) << 8)) >> 8;
                int g = ((interleaved[i++] & 0xff) | ((interleaved[i++] & 0xff) << 8)) >> 8;
                int b = ((interleaved[i++] & 0xff) | ((interleaved[i++] & 0xff) << 8)) >> 8;
                pixels[p] = (r << 16) | (g << 8) | b;
            }
        }
        image.setRGB(0, 0, w, h, pixels, 0, w);
        return image;
    }

    private static int clamp255(int v) {
        if (v < 0) {
            return 0;
        }
        return Math.min(255, v);
    }
}
