package com.inkcore.infrastructure.out.colorconversion;

import com.inkcore.domain.colorconversion.exception.ColorConversionFailedException;
import com.inkcore.domain.colorconversion.model.ConversionRequest;
import com.inkcore.domain.colorconversion.model.RasterImageInfo;
import com.inkcore.domain.colorconversion.model.RenderingIntent;
import com.inkcore.domain.colorconversion.ports.out.ImageColorConverterPort;
import com.inkcore.infrastructure.config.ColorConversionProperties;
import org.springframework.stereotype.Component;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;

/**
 * Conversión RGB→CMYK de alta fidelidad con perfiles ICC y TIFF LZW (TwelveMonkeys).
 * <p>
 * Garantías de calidad (independiente de PNG/JPEG/TIFF y de salida TIFF/PDF):
 * <ul>
 *   <li>sin remuestreo espacial ni cambio de dimensiones</li>
 *   <li>lectura ImageReader sin subsampling</li>
 *   <li>alpha aplanado sobre blanco (sin pérdida de resolución)</li>
 *   <li>transformación siempre con perfiles ICC explícitos (src→dst), no CS_sRGB implícito</li>
 *   <li>preserva 8 o 16 bits/canal en TIFF; PDF usa 8 bpc (límite del formato)</li>
 *   <li>TIFF LZW sin pérdida + ICC CMYK original embebido + DPI</li>
 *   <li>por defecto sin vibrance/lift ni escala soft-proof (fidelidad CTP; configurables)</li>
 * </ul>
 * Nota: JPEG de entrada ya es con pérdida; RGB→CMYK implica mapeo de gamut (esperado en impresión).
 */
@Component
public class ImageColorConverterAdapter implements ImageColorConverterPort {

    private static final int TIFF_TAG_ICC_PROFILE = 34675;
    /** DPI por defecto si la imagen no trae metadatos (impresión estándar). */
    private static final double DEFAULT_RASTER_DPI = 300.0;
    /**
     * Empuje hacia blanco en RGB antes del CMYK (0–0.15). Default 0 para no lavar saturación.
     */
    private static final float DEFAULT_BRIGHTNESS_LIFT = 0f;
    private static final RenderingHints QUALITY_HINTS = createQualityHints();

    private final IccProfileLoader iccProfileLoader;
    private final ColorConversionProperties properties;

    public ImageColorConverterAdapter(IccProfileLoader iccProfileLoader, ColorConversionProperties properties) {
        this.iccProfileLoader = iccProfileLoader;
        this.properties = properties;
    }

    private static RenderingHints createQualityHints() {
        RenderingHints hints = new RenderingHints(
                RenderingHints.KEY_COLOR_RENDERING,
                RenderingHints.VALUE_COLOR_RENDER_QUALITY
        );
        hints.put(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
        hints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        return hints;
    }

    @Override
    public byte[] convertToCmykTiff(
            ConversionRequest request,
            String sourceIccProfileName,
            String destinationIccProfileName
    ) {
        try {
            LoadedRaster loaded = loadRasterHighFidelity(request.getFileBytes());
            ICC_Profile srcProfile = resolveSourceProfile(loaded.image(), sourceIccProfileName);
            byte[] destinationIccBytes = iccProfileLoader.loadProfileBytes(destinationIccProfileName);
            ICC_Profile dstProfile = profileWithIntent(destinationIccBytes, request.getRenderingIntent());

            BufferedImage cmyk = convertToCmyk(
                    loaded.image(),
                    srcProfile,
                    dstProfile,
                    loaded.bitsPerSample(),
                    brightnessLift(request),
                    vibranceBoost(request),
                    softProofBrightnessMatch(request)
            );
            Double xDpi = loaded.xDpi() != null ? loaded.xDpi() : DEFAULT_RASTER_DPI;
            Double yDpi = loaded.yDpi() != null ? loaded.yDpi() : DEFAULT_RASTER_DPI;
            return writeCmykTiffLzw(cmyk, destinationIccBytes, xDpi, yDpi);
        } catch (ColorConversionFailedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ColorConversionFailedException("Fallo al convertir imagen a CMYK: " + ex.getMessage(), ex);
        }
    }

    @Override
    public RasterImageInfo readInfo(byte[] imageBytes, String hintFileName) {
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(imageBytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));
                if (img == null) {
                    throw new ColorConversionFailedException("Formato de imagen no legible");
                }
                return new RasterImageInfo(img.getWidth(), img.getHeight(), null, null, 8, null, 1);
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(iis, true, false);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                int pageCount = 1;
                try {
                    pageCount = Math.max(1, reader.getNumImages(true));
                } catch (Exception ignored) {
                    pageCount = 1;
                }
                int bits = 8;
                Double xDpi = null;
                Double yDpi = null;
                String compression = null;
                try {
                    IIOMetadata metadata = reader.getImageMetadata(0);
                    DpiResolution dpi = extractDpi(metadata);
                    xDpi = dpi.x();
                    yDpi = dpi.y();
                    compression = extractCompression(metadata);
                    bits = extractBitsPerSample(metadata, 8);
                } catch (Exception ignored) {
                    // metadatos opcionales
                }
                return new RasterImageInfo(width, height, xDpi, yDpi, bits, compression, pageCount);
            } finally {
                reader.dispose();
            }
        } catch (ColorConversionFailedException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new ColorConversionFailedException("No se pudieron leer metadatos de imagen: " + ex.getMessage(), ex);
        }
    }

    @Override
    public RasterImageInfo readTiffInfo(byte[] tiffBytes) {
        return readInfo(tiffBytes, "output.tif");
    }

    /**
     * Conversión RGB→CMYK de un {@link BufferedImage} (reutilizable por PDF).
     * Misma ruta de calidad que TIFF: ICC explícitos, sin remuestreo.
     * Usa defaults del servidor (sin overrides de request).
     */
    public BufferedImage convertRgbToCmykBufferedImage(
            BufferedImage source,
            RenderingIntent renderingIntent,
            String sourceIccProfileName,
            String destinationIccProfileName
    ) {
        return convertRgbToCmykBufferedImage(
                source, renderingIntent, sourceIccProfileName, destinationIccProfileName, null
        );
    }

    /**
     * Misma conversión, aplicando overrides de calidad del {@link ConversionRequest} si vienen.
     */
    public BufferedImage convertRgbToCmykBufferedImage(
            BufferedImage source,
            RenderingIntent renderingIntent,
            String sourceIccProfileName,
            String destinationIccProfileName,
            ConversionRequest qualityOverrides
    ) {
        ICC_Profile srcProfile = resolveSourceProfile(source, sourceIccProfileName);
        byte[] destinationIccBytes = iccProfileLoader.loadProfileBytes(destinationIccProfileName);
        ICC_Profile dstProfile = profileWithIntent(destinationIccBytes, renderingIntent);
        int bits = Math.max(8, source.getSampleModel().getSampleSize(0));
        if (bits != 16) {
            bits = 8;
        }
        BufferedImage cmyk = convertToCmyk(
                source,
                srcProfile,
                dstProfile,
                bits,
                brightnessLift(qualityOverrides),
                vibranceBoost(qualityOverrides),
                softProofBrightnessMatch(qualityOverrides)
        );
        // ColorModel con perfil limpio (para embeber en PDF/TIFF sin header de intent mutado)
        return rewrapWithProfile(cmyk, ICC_Profile.getInstance(destinationIccBytes));
    }

    private float brightnessLift(ConversionRequest request) {
        float serverDefault = properties == null ? 0f : properties.getBrightnessLift();
        if (request == null) {
            return Math.max(0f, Math.min(0.15f, serverDefault));
        }
        return request.resolveBrightnessLift(serverDefault);
    }

    private float vibranceBoost(ConversionRequest request) {
        float serverDefault = properties == null ? 0f : properties.getVibranceBoost();
        if (request == null) {
            return Math.max(0f, Math.min(0.25f, serverDefault));
        }
        return request.resolveVibranceBoost(serverDefault);
    }

    private boolean softProofBrightnessMatch(ConversionRequest request) {
        boolean serverDefault = properties != null && properties.isSoftProofBrightnessMatch();
        if (request == null) {
            return serverDefault;
        }
        return request.resolveSoftProofBrightnessMatch(serverDefault);
    }

    /**
     * Carga raster a resolución nativa (sin subsampling) con DPI y bits/canal.
     */
    public LoadedRaster loadRasterHighFidelity(byte[] bytes) throws IOException {
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
                if (img == null) {
                    throw new ColorConversionFailedException("No se pudo leer la imagen de entrada");
                }
                return new LoadedRaster(img, null, null, 8);
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(iis, true, false);
                javax.imageio.ImageReadParam param = reader.getDefaultReadParam();
                // Sin sourceRegion / sin sourceSubsampling → píxeles 1:1
                BufferedImage image = reader.read(0, param);
                if (image == null) {
                    throw new ColorConversionFailedException("No se pudo leer la imagen de entrada");
                }
                Double xDpi = null;
                Double yDpi = null;
                int bits = Math.max(8, image.getSampleModel().getSampleSize(0));
                if (bits != 16) {
                    bits = 8;
                }
                try {
                    IIOMetadata metadata = reader.getImageMetadata(0);
                    DpiResolution dpi = extractDpi(metadata);
                    xDpi = dpi.x();
                    yDpi = dpi.y();
                    int metaBits = extractBitsPerSample(metadata, bits);
                    if (metaBits == 16) {
                        bits = 16;
                    }
                } catch (Exception ignored) {
                    // metadatos opcionales
                }
                return new LoadedRaster(image, xDpi, yDpi, bits);
            } finally {
                reader.dispose();
            }
        }
    }

    public record LoadedRaster(BufferedImage image, Double xDpi, Double yDpi, int bitsPerSample) {
    }

    public byte[] writeCmykTiff(BufferedImage cmyk, Double xDpi, Double yDpi) {
        try {
            byte[] icc = null;
            if (cmyk.getColorModel().getColorSpace() instanceof ICC_ColorSpace ics) {
                icc = ics.getProfile().getData();
            }
            return writeCmykTiffLzw(cmyk, icc, xDpi, yDpi);
        } catch (IOException ex) {
            throw new ColorConversionFailedException("No se pudo escribir TIFF CMYK: " + ex.getMessage(), ex);
        }
    }

    /**
     * Escribe un TIFF CMYK multipágina (LZW) con ICC embebido y DPI.
     */
    public byte[] writeCmykTiffPages(java.util.List<BufferedImage> pages, byte[] destinationIccBytes, Double xDpi, Double yDpi) {
        if (pages == null || pages.isEmpty()) {
            throw new ColorConversionFailedException("No hay páginas para escribir en TIFF");
        }
        try {
            if (pages.size() == 1) {
                return writeCmykTiffLzw(pages.get(0), destinationIccBytes, xDpi, yDpi);
            }
            return writeCmykTiffLzwSequence(pages, destinationIccBytes, xDpi, yDpi);
        } catch (IOException ex) {
            throw new ColorConversionFailedException("No se pudo escribir TIFF CMYK multipágina: " + ex.getMessage(), ex);
        }
    }

    private ICC_Profile resolveSourceProfile(BufferedImage source, String fallbackProfileName) {
        ColorSpace cs = source.getColorModel().getColorSpace();
        // Solo usar ICC embebido si el ColorModel lo expone de forma fiable (≥3 canales)
        if (cs instanceof ICC_ColorSpace ics && cs.getNumComponents() >= 3) {
            ICC_Profile embedded = ics.getProfile();
            if (embedded != null && embedded.getData() != null && embedded.getData().length > 128) {
                return embedded;
            }
        }
        return ICC_Profile.getInstance(iccProfileLoader.loadProfileBytes(fallbackProfileName));
    }

    private ICC_Profile loadProfileWithIntent(String profileName, RenderingIntent intent) {
        return profileWithIntent(iccProfileLoader.loadProfileBytes(profileName), intent);
    }

    private static ICC_Profile profileWithIntent(byte[] iccBytes, RenderingIntent intent) {
        byte[] copy = iccBytes.clone();
        applyRenderingIntentHeader(copy, intent);
        return ICC_Profile.getInstance(copy);
    }

    /**
     * Escribe el rendering intent en el header ICC (offset 64), usado por el CMM del JDK.
     */
    static void applyRenderingIntentHeader(byte[] iccBytes, RenderingIntent intent) {
        if (iccBytes == null || iccBytes.length < 68) {
            return;
        }
        int value = intent == RenderingIntent.RELATIVE_COLORIMETRIC ? 1 : 0;
        iccBytes[64] = 0;
        iccBytes[65] = 0;
        iccBytes[66] = 0;
        iccBytes[67] = (byte) value;
    }

    /**
     * Aplana alpha sobre blanco a la misma resolución (sin escala).
     * Necesario: ColorConvertOp a CMYK opaco no interpreta alpha correctamente.
     */
    private static BufferedImage flattenAlphaOntoWhite(BufferedImage source) {
        if (!source.getColorModel().hasAlpha()) {
            return source;
        }
        int w = source.getWidth();
        int h = source.getHeight();
        BufferedImage opaque = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = opaque.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, w, h);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(source, 0, 0, w, h, null);
        } finally {
            g.dispose();
        }
        return opaque;
    }

    /**
     * Prepara raster RGB opaco convertible. Sin remuestreo espacial.
     * Indexados / binarios / gris se expanden a TYPE_INT_RGB (misma resolución).
     */
    private static BufferedImage prepareRgbWorkingImage(BufferedImage source, ICC_Profile srcProfile) {
        BufferedImage flat = flattenAlphaOntoWhite(source);
        ColorModel cm = flat.getColorModel();
        ColorSpace cs = cm.getColorSpace();

        boolean indexed = cm instanceof java.awt.image.IndexColorModel
                || flat.getType() == BufferedImage.TYPE_BYTE_INDEXED
                || flat.getType() == BufferedImage.TYPE_BYTE_BINARY;
        boolean gray = cs.getNumComponents() < 3;
        boolean alreadyRgb = !indexed && !gray && !cm.hasAlpha() && cs.getNumComponents() >= 3
                && cs.getType() != ColorSpace.TYPE_CMYK;

        if (alreadyRgb) {
            return flat;
        }

        // Expansión a RGB opaco sin escala (Graphics2D 1:1). El perfil src se aplica luego en tagRgbWithProfile.
        BufferedImage rgb = new BufferedImage(flat.getWidth(), flat.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, flat.getWidth(), flat.getHeight());
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(flat, 0, 0, null);
        } finally {
            g.dispose();
        }
        return rgb;
    }

    /**
     * RGB→CMYK (o CMYK→CMYK) con perfiles ICC vía ColorModel de origen/destino.
     * Por defecto no retoca tinta tras soft-proof (fidelidad CTP); ver softProofBrightnessMatch.
     */
    private static BufferedImage convertToCmyk(
            BufferedImage source,
            ICC_Profile srcProfile,
            ICC_Profile dstProfile,
            int bitsPerSample,
            float brightnessLift,
            float vibranceBoost,
            boolean softProofBrightnessMatch
    ) {
        int bits = bitsPerSample >= 16 ? 16 : 8;
        ColorSpace dstCs = new ICC_ColorSpace(dstProfile);
        BufferedImage cmyk = createComponentImage(source.getWidth(), source.getHeight(), dstCs, 4, bits);

        BufferedImage referenceRgb = null;
        if (isCmykImage(source)) {
            new ColorConvertOp(QUALITY_HINTS).filter(source, cmyk);
            if (softProofBrightnessMatch) {
                referenceRgb = softProofCmykToRgb(cmyk);
            }
        } else {
            BufferedImage appearanceRef = prepareRgbWorkingImage(source, srcProfile);
            BufferedImage working = boostVibranceHsb(appearanceRef, vibranceBoost);
            working = liftRgbTowardWhite(working, brightnessLift);
            // Igualar brillo al original (sin lift), no al RGB ya aclarado
            referenceRgb = appearanceRef;
            BufferedImage tagged = tagRgbWithProfile(working, srcProfile, bits);
            new ColorConvertOp(QUALITY_HINTS).filter(tagged, cmyk);
        }

        if (softProofBrightnessMatch) {
            matchSoftProofBrightness(referenceRgb, cmyk);
        }

        if (cmyk.getWidth() != source.getWidth() || cmyk.getHeight() != source.getHeight()) {
            throw new ColorConversionFailedException(
                    "La conversión alteró las dimensiones (no permitido): "
                            + source.getWidth() + "x" + source.getHeight()
                            + " → " + cmyk.getWidth() + "x" + cmyk.getHeight()
            );
        }
        return cmyk;
    }

    /**
     * Mezcla RGB hacia blanco (misma resolución). amount=0.08 ≈ +8% de brillo percibido
     * antes del mapeo a tinta, sin saturar blancos puros.
     */
    static BufferedImage liftRgbTowardWhite(BufferedImage source, float amount) {
        if (amount <= 0f) {
            return source;
        }
        float a = Math.min(0.15f, amount);
        int w = source.getWidth();
        int h = source.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        int[] pixels = source.getRGB(0, 0, w, h, null, 0, w);
        for (int i = 0; i < pixels.length; i++) {
            int rgb = pixels[i];
            int r = (rgb >> 16) & 0xff;
            int g = (rgb >> 8) & 0xff;
            int b = rgb & 0xff;
            r = r + Math.round((255 - r) * a);
            g = g + Math.round((255 - g) * a);
            b = b + Math.round((255 - b) * a);
            pixels[i] = (r << 16) | (g << 8) | b;
        }
        out.setRGB(0, 0, w, h, pixels, 0, w);
        return out;
    }

    /**
     * Aumenta saturación HSB de forma selectiva (más en colores medios/vivos, casi nada en piel baja-sat).
     * Mitiga el aspecto “apagado” tras comprimir a gamut CMYK.
     */
    static BufferedImage boostVibranceHsb(BufferedImage source, float amount) {
        if (amount <= 0f) {
            return source;
        }
        float boost = Math.min(0.25f, amount);
        int w = source.getWidth();
        int h = source.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        int[] pixels = source.getRGB(0, 0, w, h, null, 0, w);
        float[] hsb = new float[3];
        for (int i = 0; i < pixels.length; i++) {
            int rgb = pixels[i];
            int r = (rgb >> 16) & 0xff;
            int g = (rgb >> 8) & 0xff;
            int b = rgb & 0xff;
            Color.RGBtoHSB(r, g, b, hsb);
            // Vibrance: más boost donde ya hay color; protege bajos (piel/neutros)
            float sat = hsb[1];
            if (sat > 0.12f && sat < 0.98f) {
                float weight = sat < 0.45f ? (sat / 0.45f) : 1f;
                float newSat = sat + (1f - sat) * boost * weight;
                hsb[1] = Math.min(1f, newSat);
                pixels[i] = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]) & 0xffffff;
            }
        }
        out.setRGB(0, 0, w, h, pixels, 0, w);
        return out;
    }

    /**
     * Si el soft-proof CMYK→RGB queda más oscuro que el RGB de referencia,
     * reduce tinta (escala C/M/Y/K) para recuperar brillo sin remuestrear.
     */
    static void matchSoftProofBrightness(BufferedImage referenceRgb, BufferedImage cmyk) {
        if (referenceRgb == null || cmyk == null) {
            return;
        }
        BufferedImage proof = softProofCmykToRgb(cmyk);
        double srcLuma = meanLuma(referenceRgb);
        double proofLuma = meanLuma(proof);
        if (srcLuma < 1.0 || proofLuma < 1.0) {
            return;
        }
        // Solo corregir si el proof está claramente más oscuro
        if (proofLuma >= srcLuma * 0.97) {
            return;
        }
        double inkScale = proofLuma / srcLuma;
        // No lavar la imagen: máximo ~18% menos tinta
        inkScale = Math.max(0.82, Math.min(1.0, inkScale));
        scaleCmykInPlace(cmyk, inkScale);
    }

    private static BufferedImage softProofCmykToRgb(BufferedImage cmyk) {
        BufferedImage rgb = new BufferedImage(cmyk.getWidth(), cmyk.getHeight(), BufferedImage.TYPE_INT_RGB);
        new ColorConvertOp(QUALITY_HINTS).filter(cmyk, rgb);
        return rgb;
    }

    private static double meanLuma(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        long sum = 0;
        int[] pixels = image.getRGB(0, 0, w, h, null, 0, w);
        for (int rgb : pixels) {
            int r = (rgb >> 16) & 0xff;
            int g = (rgb >> 8) & 0xff;
            int b = rgb & 0xff;
            sum += (int) (0.2126 * r + 0.7152 * g + 0.0722 * b);
        }
        return sum / (double) pixels.length;
    }

    private static void scaleCmykInPlace(BufferedImage cmyk, double scale) {
        if (scale >= 0.999) {
            return;
        }
        WritableRaster raster = cmyk.getRaster();
        int w = cmyk.getWidth();
        int h = cmyk.getHeight();
        int bands = Math.min(4, raster.getNumBands());
        boolean ushort = raster.getDataBuffer().getDataType() == DataBuffer.TYPE_USHORT;
        int max = ushort ? 65535 : 255;
        int[] pixel = new int[Math.max(4, bands)];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                raster.getPixel(x, y, pixel);
                for (int b = 0; b < bands; b++) {
                    pixel[b] = (int) Math.round(pixel[b] * scale);
                    if (pixel[b] < 0) {
                        pixel[b] = 0;
                    } else if (pixel[b] > max) {
                        pixel[b] = max;
                    }
                }
                raster.setPixel(x, y, pixel);
            }
        }
    }

    private static boolean isCmykImage(BufferedImage image) {
        ColorSpace cs = image.getColorModel().getColorSpace();
        return cs != null && cs.getNumComponents() == 4 && cs.getType() == ColorSpace.TYPE_CMYK;
    }

    /**
     * Copia píxeles RGB al ColorModel con el perfil ICC de origen (sin remuestreo).
     * Así ColorConvertOp usa el perfil pedido y no el CS_sRGB implícito de TYPE_INT_RGB.
     */
    private static BufferedImage tagRgbWithProfile(BufferedImage source, ICC_Profile srcProfile, int bits) {
        ColorSpace srcCs = new ICC_ColorSpace(srcProfile);
        if (source.getColorModel().getColorSpace() instanceof ICC_ColorSpace existing
                && profilesLikelySame(existing.getProfile(), srcProfile)
                && !source.getColorModel().hasAlpha()
                && source.getColorModel().getNumColorComponents() == 3) {
            return source;
        }

        BufferedImage tagged = createComponentImage(source.getWidth(), source.getHeight(), srcCs, 3, bits);
        int w = source.getWidth();
        int h = source.getHeight();
        WritableRaster dst = tagged.getRaster();

        if (bits == 8) {
            int[] argbs = source.getRGB(0, 0, w, h, null, 0, w);
            int[] pixel = new int[3];
            int i = 0;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int argb = argbs[i++];
                    pixel[0] = (argb >> 16) & 0xff;
                    pixel[1] = (argb >> 8) & 0xff;
                    pixel[2] = argb & 0xff;
                    dst.setPixel(x, y, pixel);
                }
            }
            return tagged;
        }

        WritableRaster srcRaster = source.getRaster();
        int bands = Math.min(3, srcRaster.getNumBands());
        int[] pixel = new int[3];
        int[] srcPx = new int[Math.max(3, srcRaster.getNumBands())];
        boolean src8 = srcRaster.getSampleModel().getSampleSize(0) <= 8;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                srcRaster.getPixel(x, y, srcPx);
                for (int b = 0; b < 3; b++) {
                    int v = b < bands ? srcPx[b] : 0;
                    if (src8) {
                        v = (v * 65535) / 255;
                    }
                    pixel[b] = v;
                }
                dst.setPixel(x, y, pixel);
            }
        }
        return tagged;
    }

    private static boolean profilesLikelySame(ICC_Profile a, ICC_Profile b) {
        if (a == null || b == null) {
            return false;
        }
        if (a == b) {
            return true;
        }
        byte[] da = a.getData();
        byte[] db = b.getData();
        if (da.length != db.length) {
            return false;
        }
        // Comparación rápida: tamaño + primeros/últimos bytes
        for (int i = 0; i < 64 && i < da.length; i++) {
            if (da[i] != db[i]) {
                return false;
            }
        }
        return true;
    }

    private static BufferedImage createComponentImage(
            int width,
            int height,
            ColorSpace colorSpace,
            int numComponents,
            int bitsPerSample
    ) {
        int bits = bitsPerSample >= 16 ? 16 : 8;
        int dataType = bits == 16 ? DataBuffer.TYPE_USHORT : DataBuffer.TYPE_BYTE;
        int[] bitsArray = new int[numComponents];
        java.util.Arrays.fill(bitsArray, bits);
        ComponentColorModel model = new ComponentColorModel(
                colorSpace,
                bitsArray,
                false,
                false,
                Transparency.OPAQUE,
                dataType
        );
        WritableRaster raster = model.createCompatibleWritableRaster(width, height);
        return new BufferedImage(model, raster, false, null);
    }

    /**
     * Reasigna el mismo raster CMYK a un ColorModel con el perfil ICC limpio (para embeber).
     */
    private static BufferedImage rewrapWithProfile(BufferedImage cmyk, ICC_Profile cleanProfile) {
        int bits = cmyk.getSampleModel().getSampleSize(0) >= 16 ? 16 : 8;
        int dataType = bits == 16 ? DataBuffer.TYPE_USHORT : DataBuffer.TYPE_BYTE;
        int[] bitsArray = bits == 16 ? new int[]{16, 16, 16, 16} : new int[]{8, 8, 8, 8};
        ComponentColorModel model = new ComponentColorModel(
                new ICC_ColorSpace(cleanProfile),
                bitsArray,
                false,
                false,
                Transparency.OPAQUE,
                dataType
        );
        return new BufferedImage(model, cmyk.getRaster(), false, null);
    }

    private static byte[] writeCmykTiffLzw(
            BufferedImage cmyk,
            byte[] destinationIccBytes,
            Double xDpi,
            Double yDpi
    ) throws IOException {
        // Mismo raster CMYK, ColorModel sin ICC pesado (escritura estable/rápida)
        BufferedImage forWrite = rewrapForTiffWrite(cmyk);
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("TIFF");
        if (!writers.hasNext()) {
            throw new ColorConversionFailedException("No hay ImageWriter TIFF (TwelveMonkeys) disponible");
        }
        ImageWriter writer = writers.next();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            ImageWriteParam writeParam = createLosslessTiffWriteParam(writer);
            IIOMetadata metadata = buildPageMetadata(writer, writeParam, forWrite, destinationIccBytes, xDpi, yDpi);
            writer.write(null, new IIOImage(forWrite, null, metadata), writeParam);
        } finally {
            writer.dispose();
        }
        byte[] tiff = baos.toByteArray();
        if (destinationIccBytes != null && destinationIccBytes.length > 0) {
            tiff = TiffIccEmbedder.embed(tiff, destinationIccBytes);
        }
        return tiff;
    }

    private static byte[] writeCmykTiffLzwSequence(
            java.util.List<BufferedImage> pages,
            byte[] destinationIccBytes,
            Double xDpi,
            Double yDpi
    ) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("TIFF");
        if (!writers.hasNext()) {
            throw new ColorConversionFailedException("No hay ImageWriter TIFF (TwelveMonkeys) disponible");
        }
        ImageWriter writer = writers.next();
        if (!writer.canWriteSequence()) {
            return writeCmykTiffLzw(pages.get(0), destinationIccBytes, xDpi, yDpi);
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            ImageWriteParam writeParam = createLosslessTiffWriteParam(writer);
            writer.prepareWriteSequence(null);
            for (BufferedImage page : pages) {
                BufferedImage forWrite = rewrapForTiffWrite(page);
                IIOMetadata metadata = buildPageMetadata(writer, writeParam, forWrite, destinationIccBytes, xDpi, yDpi);
                writer.writeToSequence(new IIOImage(forWrite, null, metadata), writeParam);
            }
            writer.endWriteSequence();
        } finally {
            writer.dispose();
        }
        byte[] tiff = baos.toByteArray();
        if (destinationIccBytes != null && destinationIccBytes.length > 0) {
            tiff = TiffIccEmbedder.embed(tiff, destinationIccBytes);
        }
        return tiff;
    }

    /**
     * TwelveMonkeys cuelga o tarda minutos serializando ICC_ColorSpace grandes en el ColorModel.
     * Se mantiene el raster CMYK ya convertido y se etiqueta con un ColorSpace CMYK ligero.
     */
    private static BufferedImage rewrapForTiffWrite(BufferedImage cmyk) {
        int bits = cmyk.getSampleModel().getSampleSize(0) >= 16 ? 16 : 8;
        int dataType = bits == 16 ? DataBuffer.TYPE_USHORT : DataBuffer.TYPE_BYTE;
        int[] bitsArray = bits == 16 ? new int[]{16, 16, 16, 16} : new int[]{8, 8, 8, 8};
        ComponentColorModel model = new ComponentColorModel(
                DeviceCmykColorSpace.INSTANCE,
                bitsArray,
                false,
                false,
                Transparency.OPAQUE,
                dataType
        );
        return new BufferedImage(model, cmyk.getRaster(), false, null);
    }

    private static ImageWriteParam createLosslessTiffWriteParam(ImageWriter writer) {
        ImageWriteParam writeParam = writer.getDefaultWriteParam();
        if (writeParam.canWriteCompressed()) {
            writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            String[] types = writeParam.getCompressionTypes();
            writeParam.setCompressionType(pickLosslessCompression(types));
        }
        return writeParam;
    }

    private static IIOMetadata buildPageMetadata(
            ImageWriter writer,
            ImageWriteParam writeParam,
            BufferedImage cmyk,
            byte[] destinationIccBytes,
            Double xDpi,
            Double yDpi
    ) {
        IIOMetadata metadata = writer.getDefaultImageMetadata(
                ImageTypeSpecifier.createFromRenderedImage(cmyk),
                writeParam
        );
        if (metadata != null) {
            if (xDpi != null && yDpi != null) {
                applyDpi(metadata, xDpi, yDpi);
            }
            // ICC grande no se embebe vía árbol de metadatos (coma-separado): inestable/lento.
            // Los samples ya están en el espacio del perfil de destino (FOGRA, etc.).
            if (destinationIccBytes != null
                    && destinationIccBytes.length > 0
                    && destinationIccBytes.length <= 32_768) {
                embedIccProfile(metadata, destinationIccBytes);
            }
        }
        return metadata;
    }

    private static String pickLosslessCompression(String[] types) {
        if (types == null || types.length == 0) {
            return "LZW";
        }
        // LZW/Deflate = sin pérdida y compacto; None también sin pérdida
        for (String preferred : new String[]{"LZW", "Deflate", "ZLib", "None"}) {
            for (String type : types) {
                if (preferred.equalsIgnoreCase(type)) {
                    return type;
                }
            }
        }
        return types[0];
    }

    private static void applyDpi(IIOMetadata metadata, double xDpi, double yDpi) {
        try {
            String nativeFormat = metadata.getNativeMetadataFormatName();
            if (nativeFormat == null) {
                return;
            }
            IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(nativeFormat);
            IIOMetadataNode ifd = findOrCreate(root, "TIFFIFD");
            setTiffRational(ifd, 282, xDpi); // XResolution
            setTiffRational(ifd, 283, yDpi); // YResolution
            setTiffShort(ifd, 296, 2); // ResolutionUnit = inch
            metadata.setFromTree(nativeFormat, root);
        } catch (Exception ignored) {
            // DPI best-effort
        }
    }

    /**
     * Embebe el perfil ICC CMYK (TIFF tag 34675) para que Photoshop/RIPs interpreten bien el color.
     */
    private static void embedIccProfile(IIOMetadata metadata, byte[] iccBytes) {
        try {
            String nativeFormat = metadata.getNativeMetadataFormatName();
            if (nativeFormat == null) {
                return;
            }
            IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(nativeFormat);
            IIOMetadataNode ifd = findOrCreate(root, "TIFFIFD");

            // Eliminar ICC previo si el writer generó uno vacío
            removeTiffFields(ifd, TIFF_TAG_ICC_PROFILE);

            IIOMetadataNode field = new IIOMetadataNode("TIFFField");
            field.setAttribute("number", String.valueOf(TIFF_TAG_ICC_PROFILE));
            field.setAttribute("name", "ICCProfile");
            IIOMetadataNode undefined = new IIOMetadataNode("TIFFUndefined");
            undefined.setAttribute("value", toCommaSeparatedUnsignedBytes(iccBytes));
            field.appendChild(undefined);
            ifd.appendChild(field);

            metadata.setFromTree(nativeFormat, root);
        } catch (Exception ignored) {
            // Embebido best-effort: la conversión CMYK sigue siendo válida sin tag
        }
    }

    private static void removeTiffFields(IIOMetadataNode ifd, int tagNumber) {
        String tag = String.valueOf(tagNumber);
        for (int i = ifd.getLength() - 1; i >= 0; i--) {
            if (!(ifd.item(i) instanceof IIOMetadataNode node)) {
                continue;
            }
            if ("TIFFField".equals(node.getNodeName()) && tag.equals(node.getAttribute("number"))) {
                ifd.removeChild(node);
            }
        }
    }

    private static String toCommaSeparatedUnsignedBytes(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 4);
        for (int i = 0; i < data.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(data[i] & 0xff);
        }
        return sb.toString();
    }

    private static IIOMetadataNode findOrCreate(IIOMetadataNode root, String name) {
        for (int i = 0; i < root.getLength(); i++) {
            if (name.equals(root.item(i).getNodeName())) {
                return (IIOMetadataNode) root.item(i);
            }
        }
        IIOMetadataNode created = new IIOMetadataNode(name);
        root.appendChild(created);
        return created;
    }

    private static void setTiffRational(IIOMetadataNode ifd, int tag, double value) {
        long numerator = Math.round(value * 100);
        long denominator = 100;
        IIOMetadataNode field = new IIOMetadataNode("TIFFField");
        field.setAttribute("number", String.valueOf(tag));
        IIOMetadataNode rationals = new IIOMetadataNode("TIFFRationals");
        IIOMetadataNode rational = new IIOMetadataNode("TIFFRational");
        rational.setAttribute("value", numerator + "/" + denominator);
        rationals.appendChild(rational);
        field.appendChild(rationals);
        ifd.appendChild(field);
    }

    private static void setTiffShort(IIOMetadataNode ifd, int tag, int value) {
        IIOMetadataNode field = new IIOMetadataNode("TIFFField");
        field.setAttribute("number", String.valueOf(tag));
        IIOMetadataNode shorts = new IIOMetadataNode("TIFFShorts");
        IIOMetadataNode shortNode = new IIOMetadataNode("TIFFShort");
        shortNode.setAttribute("value", String.valueOf(value));
        shorts.appendChild(shortNode);
        field.appendChild(shorts);
        ifd.appendChild(field);
    }

    private static DpiResolution extractDpi(IIOMetadata metadata) {
        if (metadata == null) {
            return new DpiResolution(null, null);
        }
        try {
            IIOMetadataNode std = (IIOMetadataNode) metadata.getAsTree("javax_imageio_1.0");
            IIOMetadataNode dim = getChild(std, "Dimension");
            if (dim != null) {
                IIOMetadataNode horizontal = getChild(dim, "HorizontalPixelSize");
                IIOMetadataNode vertical = getChild(dim, "VerticalPixelSize");
                Double x = horizontal == null ? null : mmToDpi(horizontal.getAttribute("value"));
                Double y = vertical == null ? null : mmToDpi(vertical.getAttribute("value"));
                if (x != null || y != null) {
                    return new DpiResolution(x, y);
                }
            }
        } catch (Exception ignored) {
        }
        return new DpiResolution(null, null);
    }

    private static Double mmToDpi(String mmPerPixel) {
        if (mmPerPixel == null || mmPerPixel.isBlank()) {
            return null;
        }
        double mm = Double.parseDouble(mmPerPixel);
        if (mm <= 0) {
            return null;
        }
        return 25.4 / mm;
    }

    private static String extractCompression(IIOMetadata metadata) {
        if (metadata == null) {
            return null;
        }
        try {
            String format = metadata.getNativeMetadataFormatName();
            if (format == null) {
                return null;
            }
            IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(format);
            String text = root.toString().toUpperCase(Locale.ROOT);
            if (text.contains("LZW")) {
                return "LZW";
            }
            if (text.contains("DEFLATE") || text.contains("ZIP") || text.contains("ZLIB")) {
                return "Deflate";
            }
            if (text.contains("JPEG")) {
                return "JPEG";
            }
            if (text.contains("PACKBITS")) {
                return "PackBits";
            }
            if (text.contains("NONE") || text.contains("UNCOMPRESSED")) {
                return "None";
            }
        } catch (Exception ignored) {
        }
        return "LZW";
    }

    private static int extractBitsPerSample(IIOMetadata metadata, int defaultBits) {
        if (metadata == null) {
            return defaultBits;
        }
        try {
            String format = metadata.getNativeMetadataFormatName();
            if (format == null) {
                return defaultBits;
            }
            IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(format);
            String text = root.toString();
            if (text.contains("16")) {
                // Heurística TIFF BitsPerSample
                if (text.contains("BitsPerSample") || text.contains("number=\"258\"")) {
                    return 16;
                }
            }
        } catch (Exception ignored) {
        }
        return defaultBits;
    }

    private static IIOMetadataNode getChild(IIOMetadataNode parent, String name) {
        for (int i = 0; i < parent.getLength(); i++) {
            if (name.equals(parent.item(i).getNodeName())) {
                return (IIOMetadataNode) parent.item(i);
            }
        }
        return null;
    }

    private record DpiResolution(Double x, Double y) {
    }
}
