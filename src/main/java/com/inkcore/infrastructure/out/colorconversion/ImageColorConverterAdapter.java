package com.inkcore.infrastructure.out.colorconversion;

import com.inkcore.domain.colorconversion.exception.ColorConversionFailedException;
import com.inkcore.domain.colorconversion.model.ConversionRequest;
import com.inkcore.domain.colorconversion.model.CmykTiffConversion;
import com.inkcore.domain.colorconversion.model.RasterImageInfo;
import com.inkcore.domain.colorconversion.model.RenderingIntent;
import com.inkcore.domain.colorconversion.ports.out.ImageColorConverterPort;
import com.inkcore.infrastructure.config.ColorConversionProperties;
import com.inkcore.infrastructure.out.colorconversion.lcms.LittleCmsColorConverter;
import com.inkcore.infrastructure.out.colorconversion.lcms.LcmsNativeUnavailableException;
import com.inkcore.infrastructure.out.colorconversion.lcms.LcmsProfileException;
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
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Locale;

/**
 * Conversión RGB→CMYK de alta fidelidad con LittleCMS (lcms2) + TIFF LZW (TwelveMonkeys).
 * <p>
 * El cálculo de color (RGB↔CMYK y soft-proof) usa {@link LittleCmsColorConverter}; el JDK
 * {@code ColorConvertOp} ya no participa. {@code ICC_Profile}/{@code ICC_ColorSpace} solo
 * etiquetan ColorModel / leen bytes de perfil para I/O.
 */
@Component
public class ImageColorConverterAdapter implements ImageColorConverterPort {

    private static final int TIFF_TAG_ICC_PROFILE = 34675;
    /** DPI por defecto si la imagen no trae metadatos (impresión estándar). */
    private static final double DEFAULT_RASTER_DPI = 300.0;

    private final IccProfileLoader iccProfileLoader;
    private final ColorConversionProperties properties;
    private final LittleCmsColorConverter littleCms;

    public ImageColorConverterAdapter(
            IccProfileLoader iccProfileLoader,
            ColorConversionProperties properties,
            LittleCmsColorConverter littleCms
    ) {
        this.iccProfileLoader = iccProfileLoader;
        this.properties = properties;
        this.littleCms = littleCms;
    }

    @Override
    public CmykTiffConversion convertToCmykTiff(
            ConversionRequest request,
            String sourceIccProfileName,
            String destinationIccProfileName
    ) {
        try {
            LoadedRaster loaded = loadRasterHighFidelity(request.getFileBytes());
            byte[] sourceIccBytes = resolveSourceProfileBytes(loaded.image(), sourceIccProfileName);
            byte[] destinationIccBytes = iccProfileLoader.loadProfileBytes(destinationIccProfileName);

            CmykConvertOutcome outcome = convertToCmyk(
                    loaded.image(),
                    sourceIccBytes,
                    destinationIccBytes,
                    request.getRenderingIntent(),
                    loaded.bitsPerSample(),
                    brightnessLift(request),
                    vibranceBoost(request),
                    softProofBrightnessMatch(request),
                    blackPointCompensation(request)
            );
            BufferedImage cmyk = outcome.cmyk();
            BufferedImage previewRgb = outcome.previewRgb();
            if (previewRgb == null) {
                previewRgb = softProofCmykToRgb(
                        cmyk, destinationIccBytes, sourceIccBytes,
                        request.getRenderingIntent(), blackPointCompensation(request)
                );
            }
            byte[] previewJpeg = writeJpeg(previewRgb, 0.95f);
            Double xDpi = loaded.xDpi() != null ? loaded.xDpi() : DEFAULT_RASTER_DPI;
            Double yDpi = loaded.yDpi() != null ? loaded.yDpi() : DEFAULT_RASTER_DPI;
            byte[] tiff = writeCmykTiffLzw(cmyk, destinationIccBytes, xDpi, yDpi);
            return new CmykTiffConversion(tiff, previewJpeg, outcome.softProofLumaRatio());
        } catch (ColorConversionFailedException ex) {
            throw ex;
        } catch (LcmsNativeUnavailableException | LcmsProfileException ex) {
            throw new ColorConversionFailedException(ex.getMessage(), ex);
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

    @Override
    public byte[] softProofRgbJpeg(byte[] cmykTiffBytes, float jpegQuality) {
        if (cmykTiffBytes == null || cmykTiffBytes.length == 0) {
            throw new ColorConversionFailedException("TIFF CMYK vacío para soft-proof");
        }
        try {
            BufferedImage cmyk = ImageIO.read(new ByteArrayInputStream(cmykTiffBytes));
            if (cmyk == null) {
                throw new ColorConversionFailedException("No se pudo leer TIFF CMYK para soft-proof");
            }
            byte[] cmykIcc = iccProfileLoader.loadProfileBytes(properties.getDestinationIccProfile());
            byte[] rgbIcc = iccProfileLoader.loadProfileBytes(properties.getSourceIccProfile());
            BufferedImage rgb = softProofCmykToRgb(
                    cmyk, cmykIcc, rgbIcc, RenderingIntent.PERCEPTUAL, null
            );
            return writeJpeg(rgb, jpegQuality);
        } catch (ColorConversionFailedException ex) {
            throw ex;
        } catch (LcmsNativeUnavailableException | LcmsProfileException ex) {
            throw new ColorConversionFailedException(ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new ColorConversionFailedException("Fallo al generar preview RGB: " + ex.getMessage(), ex);
        }
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
     * Incluye preview soft-proof JPEG y métrica de luma para la UI.
     */
    public RgbToCmykArtifacts convertRgbToCmykWithArtifacts(
            BufferedImage source,
            RenderingIntent renderingIntent,
            String sourceIccProfileName,
            String destinationIccProfileName,
            ConversionRequest qualityOverrides
    ) {
        byte[] sourceIccBytes = resolveSourceProfileBytes(source, sourceIccProfileName);
        byte[] destinationIccBytes = iccProfileLoader.loadProfileBytes(destinationIccProfileName);
        int bits = Math.max(8, source.getSampleModel().getSampleSize(0));
        if (bits != 16) {
            bits = 8;
        }
        RenderingIntent intent = renderingIntent != null ? renderingIntent : RenderingIntent.PERCEPTUAL;
        boolean bpc = blackPointCompensation(qualityOverrides);
        CmykConvertOutcome outcome = convertToCmyk(
                source,
                sourceIccBytes,
                destinationIccBytes,
                intent,
                bits,
                brightnessLift(qualityOverrides),
                vibranceBoost(qualityOverrides),
                softProofBrightnessMatch(qualityOverrides),
                bpc
        );
        BufferedImage cmyk = rewrapWithProfile(outcome.cmyk(), ICC_Profile.getInstance(destinationIccBytes));
        byte[] previewJpeg;
        try {
            BufferedImage previewRgb = outcome.previewRgb();
            if (previewRgb == null) {
                previewRgb = softProofCmykToRgb(cmyk, destinationIccBytes, sourceIccBytes, intent, bpc);
            }
            previewJpeg = writeJpeg(previewRgb, 0.95f);
        } catch (IOException ex) {
            throw new ColorConversionFailedException("Fallo al generar preview soft-proof: " + ex.getMessage(), ex);
        }
        return new RgbToCmykArtifacts(cmyk, previewJpeg, outcome.softProofLumaRatio());
    }

    public record RgbToCmykArtifacts(BufferedImage cmyk, byte[] previewJpeg, Double softProofLumaRatio) {
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
        return convertRgbToCmykWithArtifacts(
                source, renderingIntent, sourceIccProfileName, destinationIccProfileName, qualityOverrides
        ).cmyk();
    }

    private float brightnessLift(ConversionRequest request) {
        float serverDefault = properties == null ? 0f : properties.getBrightnessLift();
        if (request == null) {
            return Math.max(0f, Math.min(0.20f, serverDefault));
        }
        return request.resolveBrightnessLift(serverDefault);
    }

    private float vibranceBoost(ConversionRequest request) {
        float serverDefault = properties == null ? 0f : properties.getVibranceBoost();
        if (request == null) {
            return Math.max(0f, Math.min(0.35f, serverDefault));
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

    private boolean blackPointCompensation(ConversionRequest request) {
        boolean serverDefault = properties == null || properties.isBlackPointCompensation();
        if (request == null) {
            return serverDefault;
        }
        return request.resolveBlackPointCompensation(serverDefault);
    }

    /**
     * Carga raster a resolución nativa (sin subsampling) con DPI y bits/canal.
     */
    public LoadedRaster loadRasterHighFidelity(byte[] bytes) throws IOException {
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            return readLoadedRaster(iis, () -> ImageIO.read(new ByteArrayInputStream(bytes)));
        }
    }

    public LoadedRaster loadRasterHighFidelity(Path file) throws IOException {
        File raw = file.toFile();
        try (ImageInputStream iis = ImageIO.createImageInputStream(raw)) {
            return readLoadedRaster(iis, () -> ImageIO.read(raw));
        }
    }

    @FunctionalInterface
    private interface RasterFallback {
        BufferedImage read() throws IOException;
    }

    private LoadedRaster readLoadedRaster(ImageInputStream iis, RasterFallback fallback) throws IOException {
        if (iis == null) {
            BufferedImage img = fallback.read();
            if (img == null) {
                throw new ColorConversionFailedException("No se pudo leer la imagen de entrada");
            }
            return new LoadedRaster(img, null, null, 8);
        }
        Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
        if (!readers.hasNext()) {
            BufferedImage img = fallback.read();
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
        applyRenderingIntent(copy, intent);
        return ICC_Profile.getInstance(copy);
    }

    /**
     * Aplica rendering intent de forma efectiva para el CMM del JDK.
     * Solo escribir el header (offset 64) no basta: LCMS/Java suele usar siempre las tablas
     * A2B0/B2A0 (perceptual). Para RELATIVE se retargetean A2B0←A2B1 y B2A0←B2A1.
     */
    static void applyRenderingIntent(byte[] iccBytes, RenderingIntent intent) {
        applyRenderingIntentHeader(iccBytes, intent);
        if (intent == RenderingIntent.RELATIVE_COLORIMETRIC) {
            retargetIccTag(iccBytes, "A2B0", "A2B1");
            retargetIccTag(iccBytes, "B2A0", "B2A1");
        }
    }

    /**
     * Escribe el rendering intent en el header ICC (offset 64).
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
     * Hace que {@code targetSig} apunte a los mismos datos que {@code sourceSig} en el directorio de tags ICC.
     */
    static void retargetIccTag(byte[] iccBytes, String targetSig, String sourceSig) {
        if (iccBytes == null || iccBytes.length < 144 || targetSig == null || sourceSig == null) {
            return;
        }
        int tagCount = readIccU32(iccBytes, 128);
        if (tagCount <= 0 || tagCount > 10_000) {
            return;
        }
        int targetEntry = -1;
        int sourceEntry = -1;
        for (int i = 0; i < tagCount; i++) {
            int entry = 132 + i * 12;
            if (entry + 12 > iccBytes.length) {
                return;
            }
            String sig = new String(iccBytes, entry, 4, StandardCharsets.US_ASCII);
            if (targetSig.equals(sig)) {
                targetEntry = entry;
            } else if (sourceSig.equals(sig)) {
                sourceEntry = entry;
            }
        }
        if (targetEntry < 0 || sourceEntry < 0) {
            return;
        }
        // offset (4) + size (4)
        System.arraycopy(iccBytes, sourceEntry + 4, iccBytes, targetEntry + 4, 8);
    }

    private static int readIccU32(byte[] data, int offset) {
        return ((data[offset] & 0xff) << 24)
                | ((data[offset + 1] & 0xff) << 16)
                | ((data[offset + 2] & 0xff) << 8)
                | (data[offset + 3] & 0xff);
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
    private static BufferedImage prepareRgbWorkingImage(BufferedImage source, byte[] unusedSrcIcc) {
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
     * RGB→CMYK (o CMYK→CMYK) vía LittleCMS. Soft-proof / recovery también usan LittleCMS.
     * La recuperación apunta al RGB original (appearanceRef), no al working ya boosteado.
     */
    private CmykConvertOutcome convertToCmyk(
            BufferedImage source,
            byte[] srcIccBytes,
            byte[] dstIccBytes,
            RenderingIntent intent,
            int bitsPerSample,
            float brightnessLift,
            float vibranceBoost,
            boolean softProofBrightnessMatch,
            boolean blackPointCompensation
    ) {
        RenderingIntent effectiveIntent = intent != null ? intent : RenderingIntent.PERCEPTUAL;
        Boolean bpc = blackPointCompensation;

        if (isCmykImage(source)) {
            BufferedImage referenceRgb = softProofCmykToRgb(source, dstIccBytes, srcIccBytes, effectiveIntent, bpc);
            BufferedImage cmyk = littleCms.rgbToCmykImage(
                    referenceRgb, srcIccBytes, dstIccBytes, effectiveIntent, bpc
            );
            Double lumaRatio = null;
            BufferedImage previewRgb = softProofCmykToRgb(cmyk, dstIccBytes, srcIccBytes, effectiveIntent, bpc);
            if (softProofBrightnessMatch) {
                recoverSoftProofAppearance(referenceRgb, cmyk, dstIccBytes, srcIccBytes, effectiveIntent, bpc);
                lumaRatio = measureSoftProofLumaRatio(
                        referenceRgb, cmyk, dstIccBytes, srcIccBytes, effectiveIntent, bpc
                );
                previewRgb = softProofCmykToRgb(cmyk, dstIccBytes, srcIccBytes, effectiveIntent, bpc);
                previewRgb = alignPreviewToReference(previewRgb, referenceRgb);
            }
            assertSameSize(source, cmyk);
            return new CmykConvertOutcome(cmyk, lumaRatio, previewRgb);
        }

        BufferedImage appearanceRef = prepareRgbWorkingImage(source, srcIccBytes);
        // Precompensación comercial: lo que el soft-proof FOGRA39 suele “apagar”.
        BufferedImage softProofTarget = boostVibranceHsb(appearanceRef, vibranceBoost);
        softProofTarget = boostWarmPop(softProofTarget, Math.min(0.20f, vibranceBoost * 0.55f));
        softProofTarget = liftRgbTowardWhite(softProofTarget, brightnessLift);
        if (softProofBrightnessMatch && (brightnessLift > 0f || vibranceBoost > 0f)) {
            softProofTarget = midtoneContrast(softProofTarget, 0.22f);
        }
        BufferedImage working = softProofTarget;
        if (softProofBrightnessMatch) {
            working = unsharpLight(working, 0.48f);
        }
        BufferedImage cmyk = littleCms.rgbToCmykImage(
                working, srcIccBytes, dstIccBytes, effectiveIntent, bpc
        );
        Double lumaRatio = null;
        BufferedImage previewRgb = softProofCmykToRgb(cmyk, dstIccBytes, srcIccBytes, effectiveIntent, bpc);
        if (softProofBrightnessMatch) {
            openCmyForScreenProof(cmyk, brightnessLift, vibranceBoost);
            recoverSoftProofAppearance(softProofTarget, cmyk, dstIccBytes, srcIccBytes, effectiveIntent, bpc);
            lumaRatio = measureSoftProofLumaRatio(
                    softProofTarget, cmyk, dstIccBytes, srcIccBytes, effectiveIntent, bpc
            );
            if (lumaRatio != null && lumaRatio < 0.90) {
                openCmyForScreenProof(cmyk, brightnessLift + 0.04f, vibranceBoost);
                lumaRatio = measureSoftProofLumaRatio(
                        softProofTarget, cmyk, dstIccBytes, srcIccBytes, effectiveIntent, bpc
                );
            }
            // Preview de UI: re-soft-proof + alinear a referencia creativa (pantalla).
            // El TIFF/PDF CMYK no se altera aquí; solo el JPEG de vista previa.
            previewRgb = softProofCmykToRgb(cmyk, dstIccBytes, srcIccBytes, effectiveIntent, bpc);
            // Alinear preview a la foto original (pantalla). CMYK CTP no se toca aquí.
            previewRgb = alignPreviewToReference(previewRgb, appearanceRef);
        }
        assertSameSize(source, cmyk);
        return new CmykConvertOutcome(cmyk, lumaRatio, previewRgb);
    }

    private record CmykConvertOutcome(BufferedImage cmyk, Double softProofLumaRatio, BufferedImage previewRgb) {
    }

    private static void assertSameSize(BufferedImage source, BufferedImage cmyk) {
        if (cmyk.getWidth() != source.getWidth() || cmyk.getHeight() != source.getHeight()) {
            throw new ColorConversionFailedException(
                    "La conversión alteró las dimensiones (no permitido): "
                            + source.getWidth() + "x" + source.getHeight()
                            + " → " + cmyk.getWidth() + "x" + cmyk.getHeight()
            );
        }
    }

    /**
     * Empuje hacia blanco en medios/claros (misma resolución), protegiendo negros.
     * amount=0.12 aclara comida sin convertir sombras profundas en gris lavado.
     */
    static BufferedImage liftRgbTowardWhite(BufferedImage source, float amount) {
        if (amount <= 0f) {
            return source;
        }
        float a = Math.min(0.20f, amount);
        int w = source.getWidth();
        int h = source.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        int[] pixels = source.getRGB(0, 0, w, h, null, 0, w);
        for (int i = 0; i < pixels.length; i++) {
            int rgb = pixels[i];
            int r = (rgb >> 16) & 0xff;
            int g = (rgb >> 8) & 0xff;
            int b = rgb & 0xff;
            float luma = (0.2126f * r + 0.7152f * g + 0.0722f * b) / 255f;
            // 0 en negros (&lt;~8%), rampa hasta 1 en medios (&gt;~40%)
            float shadowProtect = smoothstep(0.08f, 0.40f, luma);
            float localA = a * shadowProtect;
            if (localA > 0.0001f) {
                r = r + Math.round((255 - r) * localA);
                g = g + Math.round((255 - g) * localA);
                b = b + Math.round((255 - b) * localA);
            }
            pixels[i] = (r << 16) | (g << 8) | b;
        }
        out.setRGB(0, 0, w, h, pixels, 0, w);
        return out;
    }

    /**
     * Contraste en medios (curva S suave sobre luma): más “punch” percibido sin levantar negros.
     */
    static BufferedImage midtoneContrast(BufferedImage source, float amount) {
        if (amount <= 0f) {
            return source;
        }
        float a = Math.min(0.25f, amount);
        int w = source.getWidth();
        int h = source.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        int[] pixels = source.getRGB(0, 0, w, h, null, 0, w);
        for (int i = 0; i < pixels.length; i++) {
            int rgb = pixels[i];
            int r = (rgb >> 16) & 0xff;
            int g = (rgb >> 8) & 0xff;
            int b = rgb & 0xff;
            float luma = (0.2126f * r + 0.7152f * g + 0.0722f * b) / 255f;
            // S-curve centrada en 0.5
            float x = luma;
            float curved = x + a * (x - 0.5f) * (1f - Math.abs(2f * x - 1f));
            curved = Math.max(0f, Math.min(1f, curved));
            if (luma < 1e-4f) {
                continue;
            }
            float scale = curved / luma;
            r = clampByte(Math.round(r * scale));
            g = clampByte(Math.round(g * scale));
            b = clampByte(Math.round(b * scale));
            pixels[i] = (r << 16) | (g << 8) | b;
        }
        out.setRGB(0, 0, w, h, pixels, 0, w);
        return out;
    }

    /** Hermite smoothstep en [edge0, edge1]. */
    private static float smoothstep(float edge0, float edge1, float x) {
        if (x <= edge0) {
            return 0f;
        }
        if (x >= edge1) {
            return 1f;
        }
        float t = (x - edge0) / (edge1 - edge0);
        return t * t * (3f - 2f * t);
    }

    /**
     * Aumenta saturación HSB de forma selectiva (más en colores medios/vivos, casi nada en neutros).
     * Mitiga el aspecto “apagado” tras comprimir a gamut CMYK.
     */
    static BufferedImage boostVibranceHsb(BufferedImage source, float amount) {
        if (amount <= 0f) {
            return source;
        }
        float boost = Math.min(0.35f, amount);
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
            float sat = hsb[1];
            if (sat > 0.04f && sat < 0.995f) {
                float weight = sat < 0.30f ? (sat / 0.30f) : 1f;
                float newSat = sat + (1f - sat) * boost * weight;
                hsb[1] = Math.min(1f, newSat);
                if (hsb[2] > 0.12f && hsb[2] < 0.90f) {
                    hsb[2] = Math.min(1f, hsb[2] + (1f - hsb[2]) * boost * 0.18f * weight);
                }
                pixels[i] = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]) & 0xffffff;
            }
        }
        out.setRGB(0, 0, w, h, pixels, 0, w);
        return out;
    }

    /**
     * Contraste local (clarity) vía unsharp 5×5: recupera microcontraste perdido al comprimir gamut.
     */
    static BufferedImage enhanceLocalContrast(BufferedImage source, float amount) {
        if (amount <= 0f) {
            return source;
        }
        float a = Math.min(0.55f, amount);
        int w = source.getWidth();
        int h = source.getHeight();
        int[] src = source.getRGB(0, 0, w, h, null, 0, w);
        int[] out = new int[src.length];
        final int radius = 2;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int i = y * w + x;
                int r = 0;
                int g = 0;
                int b = 0;
                int n = 0;
                for (int dy = -radius; dy <= radius; dy++) {
                    int yy = y + dy;
                    if (yy < 0 || yy >= h) {
                        continue;
                    }
                    for (int dx = -radius; dx <= radius; dx++) {
                        int xx = x + dx;
                        if (xx < 0 || xx >= w) {
                            continue;
                        }
                        int p = src[yy * w + xx];
                        r += (p >> 16) & 0xff;
                        g += (p >> 8) & 0xff;
                        b += p & 0xff;
                        n++;
                    }
                }
                int blurR = r / n;
                int blurG = g / n;
                int blurB = b / n;
                int o = src[i];
                int or = (o >> 16) & 0xff;
                int og = (o >> 8) & 0xff;
                int ob = o & 0xff;
                // Menos agresivo en luces altas y en negros (evita velo/ruido en sombras)
                float luma = (0.2126f * or + 0.7152f * og + 0.0722f * ob) / 255f;
                float highlightProtect = luma < 0.85f ? 1f : (1f - luma) / 0.15f;
                float shadowProtect = smoothstep(0.10f, 0.35f, luma);
                float localAmount = a * highlightProtect * Math.max(0.20f, shadowProtect);
                int nr = clampByte(Math.round(or + (or - blurR) * localAmount));
                int ng = clampByte(Math.round(og + (og - blurG) * localAmount));
                int nb = clampByte(Math.round(ob + (ob - blurB) * localAmount));
                out[i] = (nr << 16) | (ng << 8) | nb;
            }
        }
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, w, h, out, 0, w);
        return image;
    }

    /**
     * Unsharp ligero (3×3) para recuperar nitidez percibida tras el mapeo de gamut.
     */
    static BufferedImage unsharpLight(BufferedImage source, float amount) {
        if (amount <= 0f) {
            return source;
        }
        float a = Math.min(0.8f, amount);
        int w = source.getWidth();
        int h = source.getHeight();
        int[] src = source.getRGB(0, 0, w, h, null, 0, w);
        int[] out = new int[src.length];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int i = y * w + x;
                int r = 0;
                int g = 0;
                int b = 0;
                int n = 0;
                for (int dy = -1; dy <= 1; dy++) {
                    int yy = y + dy;
                    if (yy < 0 || yy >= h) {
                        continue;
                    }
                    for (int dx = -1; dx <= 1; dx++) {
                        int xx = x + dx;
                        if (xx < 0 || xx >= w) {
                            continue;
                        }
                        int p = src[yy * w + xx];
                        r += (p >> 16) & 0xff;
                        g += (p >> 8) & 0xff;
                        b += p & 0xff;
                        n++;
                    }
                }
                int blurR = r / n;
                int blurG = g / n;
                int blurB = b / n;
                int o = src[i];
                int or = (o >> 16) & 0xff;
                int og = (o >> 8) & 0xff;
                int ob = o & 0xff;
                int nr = clampByte(Math.round(or + (or - blurR) * a));
                int ng = clampByte(Math.round(og + (og - blurG) * a));
                int nb = clampByte(Math.round(ob + (ob - blurB) * a));
                out[i] = (nr << 16) | (ng << 8) | nb;
            }
        }
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, w, h, out, 0, w);
        return image;
    }

    private static int clampByte(int v) {
        if (v < 0) {
            return 0;
        }
        return Math.min(255, v);
    }

    /**
     * API de tests: recuperación post soft-proof con perfiles del servidor.
     */
    void matchSoftProofBrightness(BufferedImage referenceRgb, BufferedImage cmyk) {
        byte[] cmykIcc = iccProfileLoader.loadProfileBytes(properties.getDestinationIccProfile());
        byte[] rgbIcc = iccProfileLoader.loadProfileBytes(properties.getSourceIccProfile());
        recoverSoftProofAppearance(
                referenceRgb, cmyk, cmykIcc, rgbIcc, RenderingIntent.PERCEPTUAL, null
        );
    }

    /**
     * Ajusta el JPEG de preview (pantalla) hacia la foto original.
     * El soft-proof FOGRA39 puro siempre se ve más opaco en cálidos fuera de gamut;
     * en Comercial/Punch la UI necesita una vista usable. El CMYK de plancha no se modifica.
     */
    static BufferedImage alignPreviewToReference(BufferedImage proof, BufferedImage reference) {
        if (proof == null || reference == null) {
            return proof;
        }
        if (proof.getWidth() != reference.getWidth() || proof.getHeight() != reference.getHeight()) {
            return proof;
        }
        // Mayoría del color original + rastro del soft-proof real
        BufferedImage out = blendRgb(proof, reference, 0.72f);
        out = midtoneContrast(out, 0.10f);
        out = unsharpLight(out, 0.28f);
        return out;
    }

    /** Mezcla {@code amount} de {@code overlay} sobre {@code base} (0=base, 1=overlay). */
    static BufferedImage blendRgb(BufferedImage base, BufferedImage overlay, float amount) {
        if (amount <= 0f || overlay == null) {
            return base;
        }
        float a = Math.min(1f, amount);
        int w = base.getWidth();
        int h = base.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        int[] b = base.getRGB(0, 0, w, h, null, 0, w);
        int[] o = overlay.getRGB(0, 0, w, h, null, 0, w);
        for (int i = 0; i < b.length; i++) {
            int br = (b[i] >> 16) & 0xff;
            int bg = (b[i] >> 8) & 0xff;
            int bb = b[i] & 0xff;
            int or = (o[i] >> 16) & 0xff;
            int og = (o[i] >> 8) & 0xff;
            int ob = o[i] & 0xff;
            int r = Math.round(br + (or - br) * a);
            int g = Math.round(bg + (og - bg) * a);
            int bl = Math.round(bb + (ob - bb) * a);
            b[i] = (clampByte(r) << 16) | (clampByte(g) << 8) | clampByte(bl);
        }
        out.setRGB(0, 0, w, h, b, 0, w);
        return out;
    }

    /**
     * Tras RGB→CMYK, recupera brillo/croma del contenido usando soft-proof LittleCMS.
     */
    void recoverSoftProofAppearance(
            BufferedImage referenceRgb,
            BufferedImage cmyk,
            byte[] cmykIcc,
            byte[] rgbIcc,
            RenderingIntent intent,
            Boolean blackPointCompensation
    ) {
        if (referenceRgb == null || cmyk == null) {
            return;
        }
        if (referenceRgb.getWidth() != cmyk.getWidth() || referenceRgb.getHeight() != cmyk.getHeight()) {
            return;
        }
        boolean[] contentMask = buildContentMask(referenceRgb);
        // Una sola pasada global. Bajar K globalmente “lechea” negros (velo gris).
        BufferedImage proof = softProofCmykToRgb(cmyk, cmykIcc, rgbIcc, intent, blackPointCompensation);
        AppearanceStats ref = appearanceStatsMasked(referenceRgb, contentMask);
        AppearanceStats got = appearanceStatsMasked(proof, contentMask);
        if (ref.sampleCount < 64 || got.sampleCount < 64 || ref.meanLuma < 1.0 || got.meanLuma < 1.0) {
            return;
        }

        double brightnessRatio = Math.min(
                got.meanLuma / Math.max(1.0, ref.meanLuma),
                got.p75Luma / Math.max(1.0, ref.p75Luma)
        );

        double cmyScale = 1.0;
        double kScale = 1.0;

        if (brightnessRatio < 0.99) {
            // Solo CMY, más agresivo que antes; K intacto para negros profundos
            cmyScale = clamp(brightnessRatio, 0.72, 1.0);
        }

        if (ref.meanChroma > 5.0 && got.meanChroma < ref.meanChroma * 0.96) {
            double chromaGap = got.meanChroma / Math.max(1.0, ref.meanChroma);
            kScale = clamp(chromaGap, 0.96, 1.0);
            cmyScale = clamp(cmyScale * clamp(0.80 + 0.20 * chromaGap, 0.72, 1.0), 0.72, 1.0);
        }

        if (cmyScale < 0.999 || kScale < 0.999) {
            scaleCmykChannelsInPlace(cmyk, cmyScale, cmyScale, cmyScale, kScale);
        }
    }

    /**
     * Reduce CMY (no K) según lift/vibrance para que el soft-proof no se vea opaco.
     */
    private static void openCmyForScreenProof(BufferedImage cmyk, float brightnessLift, float vibranceBoost) {
        float punch = Math.max(0f, Math.min(1f, brightnessLift / 0.20f * 0.45f + vibranceBoost / 0.35f * 0.55f));
        if (punch <= 0.01f) {
            return;
        }
        double cmyScale = clamp(1.0 - 0.18 * punch, 0.78, 1.0);
        scaleCmykChannelsInPlace(cmyk, cmyScale, cmyScale, cmyScale, 1.0);
    }

    /**
     * Extra saturación en cálidos (naranja/amarillo/rojo comida) antes del CMYK.
     */
    static BufferedImage boostWarmPop(BufferedImage source, float amount) {
        if (amount <= 0f) {
            return source;
        }
        float a = Math.min(0.25f, amount);
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
            float hue = hsb[0]; // 0..1 ; cálidos ~ 0–0.15 y ~0.95–1
            boolean warm = hue <= 0.14f || hue >= 0.92f;
            if (warm && hsb[1] > 0.08f && hsb[2] > 0.12f) {
                hsb[1] = Math.min(1f, hsb[1] + (1f - hsb[1]) * a);
                if (hsb[2] < 0.92f) {
                    hsb[2] = Math.min(1f, hsb[2] + (1f - hsb[2]) * a * 0.25f);
                }
                pixels[i] = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]) & 0xffffff;
            }
        }
        out.setRGB(0, 0, w, h, pixels, 0, w);
        return out;
    }

    private BufferedImage softProofCmykToRgb(
            BufferedImage cmyk,
            byte[] cmykIcc,
            byte[] rgbIcc,
            RenderingIntent intent,
            Boolean blackPointCompensation
    ) {
        return littleCms.cmykToRgbImage(cmyk, cmykIcc, rgbIcc, intent, blackPointCompensation);
    }

    /**
     * Relación luma(soft-proof) / luma(RGB de referencia) sobre máscara de contenido.
     */
    Double measureSoftProofLumaRatio(
            BufferedImage referenceRgb,
            BufferedImage cmyk,
            byte[] cmykIcc,
            byte[] rgbIcc,
            RenderingIntent intent,
            Boolean blackPointCompensation
    ) {
        if (referenceRgb == null || cmyk == null) {
            return null;
        }
        if (referenceRgb.getWidth() != cmyk.getWidth() || referenceRgb.getHeight() != cmyk.getHeight()) {
            return null;
        }
        boolean[] contentMask = buildContentMask(referenceRgb);
        BufferedImage proof = softProofCmykToRgb(cmyk, cmykIcc, rgbIcc, intent, blackPointCompensation);
        AppearanceStats ref = appearanceStatsMasked(referenceRgb, contentMask);
        AppearanceStats got = appearanceStatsMasked(proof, contentMask);
        if (ref.sampleCount < 64 || got.sampleCount < 64 || ref.meanLuma < 1.0) {
            return null;
        }
        return Math.round((got.meanLuma / ref.meanLuma) * 1000.0) / 1000.0;
    }

    private byte[] resolveSourceProfileBytes(BufferedImage source, String fallbackProfileName) {
        ColorSpace cs = source.getColorModel().getColorSpace();
        if (cs instanceof ICC_ColorSpace ics && cs.getNumComponents() >= 3) {
            ICC_Profile embedded = ics.getProfile();
            if (embedded != null && embedded.getData() != null && embedded.getData().length > 128) {
                return embedded.getData();
            }
        }
        return iccProfileLoader.loadProfileBytes(fallbackProfileName);
    }

    private static byte[] writeJpeg(BufferedImage rgb, float quality) throws IOException {
        float q = Math.max(0.5f, Math.min(1f, quality));
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(rgb, "jpg", baos);
            return baos.toByteArray();
        }
        ImageWriter writer = writers.next();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(q);
            }
            writer.write(null, new IIOImage(rgb, null, null), param);
        } finally {
            writer.dispose();
        }
        return baos.toByteArray();
    }

    /**
     * Máscara de contenido a partir del RGB de referencia (excluye fondo blanco de estudio).
     */
    private static boolean[] buildContentMask(BufferedImage referenceRgb) {
        int w = referenceRgb.getWidth();
        int h = referenceRgb.getHeight();
        int[] pixels = referenceRgb.getRGB(0, 0, w, h, null, 0, w);
        boolean[] mask = new boolean[pixels.length];
        for (int i = 0; i < pixels.length; i++) {
            int rgb = pixels[i];
            int r = (rgb >> 16) & 0xff;
            int g = (rgb >> 8) & 0xff;
            int b = rgb & 0xff;
            int max = Math.max(r, Math.max(g, b));
            int min = Math.min(r, Math.min(g, b));
            int chroma = max - min;
            int luma = (int) (0.2126 * r + 0.7152 * g + 0.0722 * b);
            mask[i] = !((luma >= 242 && chroma <= 18) || luma <= 8);
        }
        return mask;
    }

    private static AppearanceStats appearanceStatsMasked(BufferedImage image, boolean[] contentMask) {
        int w = image.getWidth();
        int h = image.getHeight();
        int[] pixels = image.getRGB(0, 0, w, h, null, 0, w);
        int step = pixels.length > 400_000 ? 4 : (pixels.length > 100_000 ? 2 : 1);
        long lumaSum = 0;
        long chromaSum = 0;
        int count = 0;
        int[] lumaHist = new int[256];
        for (int i = 0; i < pixels.length; i += step) {
            if (contentMask != null && i < contentMask.length && !contentMask[i]) {
                continue;
            }
            int rgb = pixels[i];
            int r = (rgb >> 16) & 0xff;
            int g = (rgb >> 8) & 0xff;
            int b = rgb & 0xff;
            int luma = (int) (0.2126 * r + 0.7152 * g + 0.0722 * b);
            if (luma < 0) {
                luma = 0;
            } else if (luma > 255) {
                luma = 255;
            }
            int chroma = Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b));
            lumaSum += luma;
            lumaHist[luma]++;
            chromaSum += chroma;
            count++;
        }
        if (count < 64) {
            return appearanceStatsAllPixels(pixels, step);
        }
        return buildAppearanceStats(lumaSum, chromaSum, count, lumaHist);
    }

    /**
     * Stats sobre píxeles de contenido (excluye casi-blancos/negros de estudio).
     */
    private static AppearanceStats appearanceStats(BufferedImage image) {
        return appearanceStatsMasked(image, buildContentMask(image));
    }

    private static AppearanceStats appearanceStatsAllPixels(int[] pixels, int step) {
        long lumaSum = 0;
        long chromaSum = 0;
        int count = 0;
        int[] lumaHist = new int[256];
        for (int i = 0; i < pixels.length; i += step) {
            int rgb = pixels[i];
            int r = (rgb >> 16) & 0xff;
            int g = (rgb >> 8) & 0xff;
            int b = rgb & 0xff;
            int luma = (int) (0.2126 * r + 0.7152 * g + 0.0722 * b);
            if (luma < 0) {
                luma = 0;
            } else if (luma > 255) {
                luma = 255;
            }
            int chroma = Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b));
            lumaSum += luma;
            lumaHist[luma]++;
            chromaSum += chroma;
            count++;
        }
        return buildAppearanceStats(lumaSum, chromaSum, count, lumaHist);
    }

    private static AppearanceStats buildAppearanceStats(
            long lumaSum, long chromaSum, int count, int[] lumaHist
    ) {
        double meanLuma = lumaSum / (double) count;
        double meanChroma = chromaSum / (double) count;
        int target = (int) Math.round(count * 0.75);
        int seen = 0;
        int p75 = 255;
        for (int v = 0; v < 256; v++) {
            seen += lumaHist[v];
            if (seen >= target) {
                p75 = v;
                break;
            }
        }
        return new AppearanceStats(meanLuma, p75, meanChroma, count);
    }

    private record AppearanceStats(double meanLuma, double p75Luma, double meanChroma, int sampleCount) {
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void scaleCmykInPlace(BufferedImage cmyk, double scale) {
        scaleCmykChannelsInPlace(cmyk, scale, scale, scale, scale);
    }

    private static void scaleCmykChannelsInPlace(
            BufferedImage cmyk,
            double scaleC,
            double scaleM,
            double scaleY,
            double scaleK
    ) {
        if (scaleC >= 0.999 && scaleM >= 0.999 && scaleY >= 0.999 && scaleK >= 0.999) {
            return;
        }
        WritableRaster raster = cmyk.getRaster();
        int w = cmyk.getWidth();
        int h = cmyk.getHeight();
        int bands = Math.min(4, raster.getNumBands());
        boolean ushort = raster.getDataBuffer().getDataType() == DataBuffer.TYPE_USHORT;
        int max = ushort ? 65535 : 255;
        double[] scales = {scaleC, scaleM, scaleY, scaleK};
        int[] pixel = new int[Math.max(4, bands)];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                raster.getPixel(x, y, pixel);
                for (int b = 0; b < bands; b++) {
                    double s = b < scales.length ? scales[b] : 1.0;
                    pixel[b] = (int) Math.round(pixel[b] * s);
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
