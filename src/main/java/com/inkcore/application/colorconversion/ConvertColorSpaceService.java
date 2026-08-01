package com.inkcore.application.colorconversion;

import com.inkcore.domain.colorconversion.exception.ConversionIntegrityException;
import com.inkcore.domain.colorconversion.exception.UnsupportedColorFileException;
import com.inkcore.domain.colorconversion.model.ConversionHistory;
import com.inkcore.domain.colorconversion.model.ConversionRequest;
import com.inkcore.domain.colorconversion.model.ConversionResult;
import com.inkcore.domain.colorconversion.model.OutputFormat;
import com.inkcore.domain.colorconversion.model.RasterImageInfo;
import com.inkcore.domain.colorconversion.ports.in.ConvertColorSpaceUseCase;
import com.inkcore.domain.colorconversion.ports.out.ConversionHistoryRepositoryPort;
import com.inkcore.domain.colorconversion.ports.out.ImageColorConverterPort;
import com.inkcore.domain.colorconversion.ports.out.PdfColorConverterPort;
import com.inkcore.infrastructure.config.ColorConversionProperties;
import com.inkcore.infrastructure.out.colorconversion.IccProfileLoader;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Orquesta conversión RGB→CMYK con perfiles ICC y valida garantías de integridad
 * (dimensiones, DPI, profundidad de bit, compresión sin pérdida, páginas PDF).
 * El aumento de peso (~20–40 %) por 4 canales es esperado; nunca se corrige con pérdida.
 * <p>
 * Los binarios de entrada/salida viven solo en memoria (bytes) y se devuelven en la respuesta HTTP.
 * No se persisten archivos de imagen/PDF en disco ni en BD; solo metadatos opcionales de historial.
 */
@Service
public class ConvertColorSpaceService implements ConvertColorSpaceUseCase {

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("tif", "tiff", "jpg", "jpeg", "png");
    private static final Set<String> PDF_EXTENSIONS = Set.of("pdf");
    private static final Set<String> LOSSLESS_TIFF_COMPRESSION = Set.of(
            "LZW", "Deflate", "ZIP", "None", "Uncompressed", "PackBits"
    );

    private final ImageColorConverterPort imageColorConverter;
    private final PdfColorConverterPort pdfColorConverter;
    private final ConversionHistoryRepositoryPort conversionHistoryRepository;
    private final ColorConversionProperties properties;
    private final IccProfileLoader iccProfileLoader;
    private final Clock clock;
    private final MeterRegistry meterRegistry;

    public ConvertColorSpaceService(
            ImageColorConverterPort imageColorConverter,
            PdfColorConverterPort pdfColorConverter,
            ConversionHistoryRepositoryPort conversionHistoryRepository,
            ColorConversionProperties properties,
            IccProfileLoader iccProfileLoader,
            Clock clock,
            MeterRegistry meterRegistry
    ) {
        this.imageColorConverter = imageColorConverter;
        this.pdfColorConverter = pdfColorConverter;
        this.conversionHistoryRepository = conversionHistoryRepository;
        this.properties = properties;
        this.iccProfileLoader = iccProfileLoader;
        this.clock = clock;
        this.meterRegistry = meterRegistry;
    }

    @Override
    @Transactional
    public ConversionResult convert(ConversionRequest request) {
        Objects.requireNonNull(request, "request");
        String kind = resolveKind(request);
        String destinationProfile = resolveDestinationProfile(request);
        String sourceProfile = properties.getSourceIccProfile();

        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "success";
        try {
            ConversionResult result = switch (kind) {
                case "image" -> convertImage(request, sourceProfile, destinationProfile);
                case "pdf" -> convertPdf(request, sourceProfile, destinationProfile);
                default -> throw new UnsupportedColorFileException("Tipo de archivo no soportado");
            };

            conversionHistoryRepository.save(ConversionHistory.createNew(
                    request.getOriginalFileName(),
                    result.getOriginalSizeBytes(),
                    result.getFinalSizeBytes(),
                    result.getRenderingIntent(),
                    result.getIccProfileUsed(),
                    Instant.now(clock),
                    result.getProcessingTimeMs(),
                    request.getUserId(),
                    request.getMimeType()
            ));
            return result;
        } catch (RuntimeException ex) {
            outcome = "error";
            throw ex;
        } finally {
            sample.stop(Timer.builder("color_conversion_duration_seconds")
                    .description("Duración de conversión RGB→CMYK")
                    .tag("file_type", kind)
                    .tag("outcome", outcome)
                    .register(meterRegistry));
            meterRegistry.counter("color_conversion_total", "file_type", kind, "outcome", outcome).increment();
        }
    }

    private ConversionResult convertImage(
            ConversionRequest request,
            String sourceProfile,
            String destinationProfile
    ) {
        OutputFormat outputFormat = request.getOutputFormat() == null ? OutputFormat.TIFF : request.getOutputFormat();
        if (outputFormat == OutputFormat.PDF) {
            return convertImageToPdf(request, sourceProfile, destinationProfile);
        }
        return convertImageToTiff(request, sourceProfile, destinationProfile);
    }

    private ConversionResult convertImageToTiff(
            ConversionRequest request,
            String sourceProfile,
            String destinationProfile
    ) {
        long started = System.nanoTime();
        RasterImageInfo inputInfo = imageColorConverter.readInfo(request.getFileBytes(), request.getOriginalFileName());
        byte[] converted = imageColorConverter.convertToCmykTiff(request, sourceProfile, destinationProfile);
        RasterImageInfo outputInfo = imageColorConverter.readTiffInfo(converted);
        assertImageIntegrity(inputInfo, outputInfo);
        long durationMs = (System.nanoTime() - started) / 1_000_000L;
        return buildResult(
                converted,
                toCmykFileName(request.getOriginalFileName(), ".tif"),
                "image/tiff",
                request,
                destinationProfile,
                durationMs,
                outputInfo.getWidthPx(),
                outputInfo.getHeightPx()
        );
    }

    private ConversionResult convertImageToPdf(
            ConversionRequest request,
            String sourceProfile,
            String destinationProfile
    ) {
        long started = System.nanoTime();
        RasterImageInfo inputInfo = imageColorConverter.readInfo(request.getFileBytes(), request.getOriginalFileName());
        byte[] converted = pdfColorConverter.convertRasterImageToCmykPdf(request, sourceProfile, destinationProfile);
        RasterImageInfo outputInfo = pdfColorConverter.readInfo(converted);
        // Misma resolución espacial (px); el PDF es 1 página con la imagen
        if (inputInfo.getWidthPx() != outputInfo.getWidthPx() || inputInfo.getHeightPx() != outputInfo.getHeightPx()) {
            // readInfo de PDF estima px desde mediaBox*dpi; tolerar si el DPI usado difiere
            if (outputInfo.getPageCount() != 1) {
                throw new ConversionIntegrityException("El PDF de salida debe tener 1 página");
            }
        } else {
            assertDpiPreserved(inputInfo, outputInfo);
        }
        if (outputInfo.getPageCount() != 1) {
            throw new ConversionIntegrityException("El PDF de salida debe tener 1 página");
        }
        long durationMs = (System.nanoTime() - started) / 1_000_000L;
        return buildResult(
                converted,
                toCmykFileName(request.getOriginalFileName(), ".pdf"),
                "application/pdf",
                request,
                destinationProfile,
                durationMs,
                inputInfo.getWidthPx(),
                inputInfo.getHeightPx()
        );
    }

    private ConversionResult convertPdf(
            ConversionRequest request,
            String sourceProfile,
            String destinationProfile
    ) {
        OutputFormat outputFormat = request.getOutputFormat() == null ? OutputFormat.PDF : request.getOutputFormat();
        if (outputFormat == OutputFormat.TIFF) {
            return convertPdfToTiff(request, sourceProfile, destinationProfile);
        }
        long started = System.nanoTime();
        RasterImageInfo inputInfo = pdfColorConverter.readInfo(request.getFileBytes());
        byte[] converted = pdfColorConverter.convertToCmykPdf(request, sourceProfile, destinationProfile);
        RasterImageInfo outputInfo = pdfColorConverter.readInfo(converted);
        assertPdfIntegrity(inputInfo, outputInfo);
        long durationMs = (System.nanoTime() - started) / 1_000_000L;
        return buildResult(
                converted,
                toCmykFileName(request.getOriginalFileName(), ".pdf"),
                "application/pdf",
                request,
                destinationProfile,
                durationMs,
                outputInfo.getWidthPx(),
                outputInfo.getHeightPx()
        );
    }

    private ConversionResult convertPdfToTiff(
            ConversionRequest request,
            String sourceProfile,
            String destinationProfile
    ) {
        long started = System.nanoTime();
        RasterImageInfo inputInfo = pdfColorConverter.readInfo(request.getFileBytes());
        byte[] converted = pdfColorConverter.convertPdfToCmykTiff(request, sourceProfile, destinationProfile);
        RasterImageInfo outputInfo = imageColorConverter.readTiffInfo(converted);
        if (inputInfo.getPageCount() > 0
                && outputInfo.getPageCount() > 0
                && inputInfo.getPageCount() != outputInfo.getPageCount()) {
            throw new ConversionIntegrityException(
                    "Número de páginas distinto: entrada " + inputInfo.getPageCount()
                            + " vs salida TIFF " + outputInfo.getPageCount()
            );
        }
        long durationMs = (System.nanoTime() - started) / 1_000_000L;
        return buildResult(
                converted,
                toCmykFileName(request.getOriginalFileName(), ".tif"),
                "image/tiff",
                request,
                destinationProfile,
                durationMs,
                outputInfo.getWidthPx(),
                outputInfo.getHeightPx()
        );
    }

    private ConversionResult buildResult(
            byte[] converted,
            String fileName,
            String mimeType,
            ConversionRequest request,
            String destinationProfile,
            long durationMs,
            int widthPx,
            int heightPx
    ) {
        return new ConversionResult(
                converted,
                fileName,
                mimeType,
                request.getOriginalSizeBytes(),
                converted.length,
                durationMs,
                widthPx,
                heightPx,
                request.getRenderingIntent(),
                destinationProfile,
                request.resolveBrightnessLift(properties.getBrightnessLift()),
                request.resolveVibranceBoost(properties.getVibranceBoost()),
                request.resolveSoftProofBrightnessMatch(properties.isSoftProofBrightnessMatch())
        );
    }

    void assertImageIntegrity(RasterImageInfo input, RasterImageInfo output) {
        if (input.getWidthPx() != output.getWidthPx() || input.getHeightPx() != output.getHeightPx()) {
            throw new ConversionIntegrityException(
                    "Dimensiones distintas tras la conversión: entrada "
                            + input.getWidthPx() + "x" + input.getHeightPx()
                            + " vs salida " + output.getWidthPx() + "x" + output.getHeightPx()
            );
        }
        assertDpiPreserved(input, output);
        if (input.getBitsPerSample() > 0
                && output.getBitsPerSample() > 0
                && output.getBitsPerSample() < input.getBitsPerSample()) {
            throw new ConversionIntegrityException(
                    "Profundidad de bit reducida: entrada " + input.getBitsPerSample()
                            + " bits vs salida " + output.getBitsPerSample() + " bits"
            );
        }
        if (output.getCompression() != null && !output.getCompression().isBlank()) {
            String compression = output.getCompression().trim();
            boolean lossless = LOSSLESS_TIFF_COMPRESSION.stream()
                    .anyMatch(allowed -> allowed.equalsIgnoreCase(compression));
            if (!lossless && compression.toUpperCase(Locale.ROOT).contains("JPEG")) {
                throw new ConversionIntegrityException(
                        "Compresión con pérdida no permitida en TIFF de salida: " + compression
                );
            }
            if (!lossless) {
                throw new ConversionIntegrityException(
                        "Compresión de salida no es LZW/Deflate/None: " + compression
                );
            }
        }
    }

    void assertPdfIntegrity(RasterImageInfo input, RasterImageInfo output) {
        if (input.getPageCount() != output.getPageCount()) {
            throw new ConversionIntegrityException(
                    "Número de páginas distinto: entrada " + input.getPageCount()
                            + " vs salida " + output.getPageCount()
            );
        }
        if (input.getWidthPx() > 0 && input.getHeightPx() > 0
                && output.getWidthPx() > 0 && output.getHeightPx() > 0
                && (input.getWidthPx() != output.getWidthPx() || input.getHeightPx() != output.getHeightPx())) {
            throw new ConversionIntegrityException(
                    "Dimensiones de página distintas tras la conversión PDF"
            );
        }
        assertDpiPreserved(input, output);
    }

    private static void assertDpiPreserved(RasterImageInfo input, RasterImageInfo output) {
        if (input.getXResolutionDpi() != null && output.getXResolutionDpi() != null
                && !almostEqual(input.getXResolutionDpi(), output.getXResolutionDpi())) {
            throw new ConversionIntegrityException(
                    "XResolution/DPI no preservada: " + input.getXResolutionDpi()
                            + " → " + output.getXResolutionDpi()
            );
        }
        if (input.getYResolutionDpi() != null && output.getYResolutionDpi() != null
                && !almostEqual(input.getYResolutionDpi(), output.getYResolutionDpi())) {
            throw new ConversionIntegrityException(
                    "YResolution/DPI no preservada: " + input.getYResolutionDpi()
                            + " → " + output.getYResolutionDpi()
            );
        }
    }

    private static boolean almostEqual(double a, double b) {
        return Math.abs(a - b) < 0.51;
    }

    private String resolveKind(ConversionRequest request) {
        String ext = extensionOf(request.getOriginalFileName());
        String mime = request.getMimeType() == null ? "" : request.getMimeType().toLowerCase(Locale.ROOT);
        if (IMAGE_EXTENSIONS.contains(ext) || mime.startsWith("image/")) {
            if (ext.isEmpty() && mime.contains("pdf")) {
                return "pdf";
            }
            if (PDF_EXTENSIONS.contains(ext)) {
                return "pdf";
            }
            return "image";
        }
        if (PDF_EXTENSIONS.contains(ext) || mime.contains("pdf")) {
            return "pdf";
        }
        throw new UnsupportedColorFileException(
                "Extensión no soportada. Use .tif, .tiff, .jpg, .jpeg, .png o .pdf"
        );
    }

    private String resolveDestinationProfile(ConversionRequest request) {
        String requested = request.getDestinationIccProfile();
        if (requested == null || requested.isBlank()) {
            requested = properties.getDestinationIccProfile();
        }
        // Resuelve alias del catálogo al archivo real presente en classpath
        return iccProfileLoader.resolveExistingFileName(requested);
    }

    private static String extensionOf(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static String toCmykFileName(String original, String newExt) {
        String base = original == null ? "converted" : original;
        int dot = base.lastIndexOf('.');
        if (dot > 0) {
            base = base.substring(0, dot);
        }
        return base + "_CMYK" + newExt;
    }
}
