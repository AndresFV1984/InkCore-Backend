package com.inkcore.application.inkestimation;

import com.inkcore.domain.inkestimation.exception.InkFileTooLargeException;
import com.inkcore.domain.inkestimation.exception.UnsupportedInkFileException;
import com.inkcore.domain.inkestimation.model.InkConsumptionCalculator;
import com.inkcore.domain.inkestimation.model.InkCoverageAnalysis;
import com.inkcore.domain.inkestimation.model.InkDensityFactors;
import com.inkcore.domain.inkestimation.model.InkEstimateHistory;
import com.inkcore.domain.inkestimation.model.InkEstimateRequest;
import com.inkcore.domain.inkestimation.model.InkEstimateResult;
import com.inkcore.domain.inkestimation.ports.in.EstimateInkUseCase;
import com.inkcore.domain.inkestimation.ports.out.InkCoverageAnalyzerPort;
import com.inkcore.domain.inkestimation.ports.out.InkEstimateHistoryRepositoryPort;
import com.inkcore.infrastructure.config.InkEstimationProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
public class EstimateInkService implements EstimateInkUseCase {

    private static final Set<String> IMAGE_EXT = Set.of("jpg", "jpeg", "png", "tif", "tiff", "webp", "gif");
    private static final Set<String> PDF_EXT = Set.of("pdf");

    private final InkCoverageAnalyzerPort inkCoverageAnalyzer;
    private final InkEstimateHistoryRepositoryPort historyRepository;
    private final InkEstimationProperties properties;
    private final Clock clock;
    private final MeterRegistry meterRegistry;

    public EstimateInkService(
            InkCoverageAnalyzerPort inkCoverageAnalyzer,
            InkEstimateHistoryRepositoryPort historyRepository,
            InkEstimationProperties properties,
            Clock clock,
            MeterRegistry meterRegistry
    ) {
        this.inkCoverageAnalyzer = inkCoverageAnalyzer;
        this.historyRepository = historyRepository;
        this.properties = properties;
        this.clock = clock;
        this.meterRegistry = meterRegistry;
    }

    @Override
    @Transactional
    public InkEstimateResult estimate(InkEstimateRequest request) {
        Objects.requireNonNull(request, "request");
        validateExtension(request.getOriginalFileName(), request.getMimeType());

        int dpi = request.getDpi() != null ? request.getDpi() : properties.getDefaultDpi();
        InkDensityFactors density = resolveDensityFactors(request);
        String icc = request.getAnalysisIccProfile() != null && !request.getAnalysisIccProfile().isBlank()
                ? request.getAnalysisIccProfile().trim()
                : properties.getAnalysisIccProfile();

        validateFileSize(request, dpi);

        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "success";
        try {
            long started = System.nanoTime();
            InkCoverageAnalysis analysis = inkCoverageAnalyzer.analyze(request, dpi, icc);
            long durationMs = (System.nanoTime() - started) / 1_000_000L;
            InkEstimateResult result = InkConsumptionCalculator.build(request, analysis, density, durationMs);
            historyRepository.save(InkEstimateHistory.fromResult(result, request.getUserId(), Instant.now(clock)));
            return result;
        } catch (RuntimeException ex) {
            outcome = "error";
            throw ex;
        } finally {
            sample.stop(Timer.builder("ink_estimation_duration_seconds")
                    .description("Duración de estimación de tinta")
                    .tag("outcome", outcome)
                    .register(meterRegistry));
            meterRegistry.counter("ink_estimation_total", "outcome", outcome).increment();
        }
    }

    private InkDensityFactors resolveDensityFactors(InkEstimateRequest request) {
        if (request.getGramsPerCm2AtFullCoverage() != null) {
            return InkDensityFactors.uniform(request.getGramsPerCm2AtFullCoverage());
        }
        InkEstimationProperties.ChannelGrams ch = properties.getChannelGramsPerCm2();
        if (ch == null) {
            return InkDensityFactors.uniform(properties.getDefaultGramsPerCm2AtFullCoverage());
        }
        return new InkDensityFactors(
                ch.getCyan(),
                ch.getMagenta(),
                ch.getYellow(),
                ch.getBlack(),
                ch.getSpot()
        );
    }

    private void validateFileSize(InkEstimateRequest request, int dpi) {
        long size = request.getOriginalSizeBytes();
        long absoluteMax = properties.getAbsoluteMaxFileBytes();
        if (size > absoluteMax) {
            throw new InkFileTooLargeException(
                    "El archivo supera el techo de seguridad (" + absoluteMax + " bytes)"
            );
        }
        // Tamaño teórico sin comprimir: px ≈ cm/2.54 * dpi; 4 canales CMYK × 1 byte
        double widthIn = request.getWidthCm() / 2.54;
        double heightIn = request.getHeightCm() / 2.54;
        long expectedPx = Math.max(1L, Math.round(widthIn * dpi) * Math.round(heightIn * dpi));
        long expectedUncompressed = expectedPx * 4L;
        long minAllowed = Math.max(1L, (long) (expectedUncompressed * properties.getSizeToleranceMinRatio()));
        long maxAllowed = Math.max(minAllowed, (long) (expectedUncompressed * properties.getSizeToleranceMaxRatio()));
        maxAllowed = Math.min(maxAllowed, absoluteMax);
        if (size > maxAllowed) {
            throw new InkFileTooLargeException(
                    "El archivo (" + size + " bytes) es demasiado grande para el área declarada "
                            + request.getWidthCm() + "×" + request.getHeightCm() + " cm a " + dpi
                            + " DPI (máx. esperado ~" + maxAllowed + " bytes)"
            );
        }
        // Archivos muy pequeños respecto al área no se rechazan: la compresión puede ser extrema
        // (p. ej. PDF vectorial). Solo se usa el techo superior + absoluto.
        if (size < minAllowed && isRasterExtension(request.getOriginalFileName())) {
            // Raster sin casi datos para el área: sospechoso pero puede ser válido (1 color). No rechazar.
        }
    }

    private static boolean isRasterExtension(String fileName) {
        String ext = extensionOf(fileName);
        return IMAGE_EXT.contains(ext);
    }

    private static void validateExtension(String fileName, String mimeType) {
        String ext = extensionOf(fileName);
        String mime = mimeType == null ? "" : mimeType.toLowerCase(Locale.ROOT);
        boolean ok = IMAGE_EXT.contains(ext) || PDF_EXT.contains(ext)
                || mime.startsWith("image/") || mime.contains("pdf");
        if (!ok) {
            throw new UnsupportedInkFileException(
                    "Formato no soportado. Use JPG, PNG, TIFF, WEBP, GIF o PDF"
            );
        }
        if ("webp".equals(ext)) {
            // WEBP: soporte vía ImageIO si hay plugin; si no, el analyzer fallará con mensaje claro
        }
    }

    private static String extensionOf(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
