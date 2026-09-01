package com.inkcore.infrastructure.out.inkestimation;

import com.inkcore.domain.inkestimation.exception.InkEstimationFailedException;
import com.inkcore.domain.inkestimation.exception.UnsupportedInkFileException;
import com.inkcore.domain.inkestimation.model.InkCoverageAnalysis;
import com.inkcore.domain.inkestimation.model.InkCoverageAnalysis.RawInkCoverage;
import com.inkcore.domain.inkestimation.model.InkEstimateRequest;
import com.inkcore.domain.inkestimation.model.InkPageSelection;
import com.inkcore.domain.inkestimation.ports.out.InkCoverageAnalyzerPort;
import com.inkcore.infrastructure.config.InkEstimationProperties;
import com.inkcore.infrastructure.out.colorconversion.IccProfileLoader;
import com.inkcore.infrastructure.out.colorconversion.ImageColorConverterAdapter;
import com.inkcore.infrastructure.out.colorconversion.PdfColorConverterAdapter;
import com.inkcore.infrastructure.out.colorconversion.lcms.LittleCmsColorConverter;
import com.inkcore.infrastructure.out.colorconversion.lcms.LcmsNativeUnavailableException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.springframework.stereotype.Component;

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Analiza cobertura CMYK/spot.
 * <ul>
 *   <li>Raster: downscale + ICC rápido (o TIFF CMYK nativo)</li>
 *   <li>PDF: {@link PdfInkCoverageEngine} + {@link FastInkRgbToCmyk} (LittleCMS)</li>
 * </ul>
 */
@Component
public class InkCoverageAnalyzerAdapter implements InkCoverageAnalyzerPort {

    private final ImageColorConverterAdapter imageColorConverter;
    private final PdfColorConverterAdapter pdfColorConverter;
    private final IccProfileLoader iccProfileLoader;
    private final LittleCmsColorConverter littleCms;
    private final InkEstimationProperties properties;

    public InkCoverageAnalyzerAdapter(
            ImageColorConverterAdapter imageColorConverter,
            PdfColorConverterAdapter pdfColorConverter,
            IccProfileLoader iccProfileLoader,
            LittleCmsColorConverter littleCms,
            InkEstimationProperties properties
    ) {
        this.imageColorConverter = imageColorConverter;
        this.pdfColorConverter = pdfColorConverter;
        this.iccProfileLoader = iccProfileLoader;
        this.littleCms = littleCms;
        this.properties = properties;
    }

    @Override
    public InkCoverageAnalysis analyze(InkEstimateRequest request, int dpi, String destinationIccProfile) {
        String ext = extensionOf(request.getOriginalFileName());
        try {
            if ("pdf".equals(ext) || (request.getMimeType() != null && request.getMimeType().toLowerCase().contains("pdf"))) {
                return analyzePdf(request, dpi, destinationIccProfile);
            }
            return analyzeRaster(request, dpi, destinationIccProfile);
        } catch (UnsupportedInkFileException | InkEstimationFailedException ex) {
            throw ex;
        } catch (LcmsNativeUnavailableException ex) {
            throw new InkEstimationFailedException(ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new InkEstimationFailedException("Fallo al analizar cobertura de tinta: " + ex.getMessage(), ex);
        }
    }

    private InkCoverageAnalysis analyzeRaster(
            InkEstimateRequest request,
            int dpi,
            String destinationIccProfile
    ) throws IOException {
        ImageColorConverterAdapter.LoadedRaster loaded = loadRaster(request);
        BufferedImage source = loaded.image();
        int effectiveDpi = dpi;
        if (loaded.xDpi() != null && loaded.xDpi() > 0) {
            effectiveDpi = (int) Math.round(loaded.xDpi());
        }

        int maxPixels = properties.getMaxAnalysisPixels() > 0
                ? properties.getMaxAnalysisPixels()
                : FastInkRgbToCmyk.DEFAULT_RASTER_MAX_PIXELS;
        int imageMaxEdge = properties.getRgbImageMaxEdge() > 0
                ? properties.getRgbImageMaxEdge()
                : FastInkRgbToCmyk.DEFAULT_IMAGE_MAX_EDGE;

        try (FastInkRgbToCmyk converter = new FastInkRgbToCmyk(
                iccProfileLoader,
                littleCms,
                destinationIccProfile,
                imageMaxEdge,
                properties.isBlackPointCompensation()
        )) {
            boolean nativeCmyk = isCmyk(source);
            double[] means01;
            int width;
            int height;
            if (nativeCmyk) {
                BufferedImage working = FastInkRgbToCmyk.scaleToMaxPixels(source, maxPixels);
                means01 = FastInkRgbToCmyk.meanChannels01(working.getRaster());
                width = working.getWidth();
                height = working.getHeight();
            } else {
                BufferedImage working = converter.prepareRgbForCoverage(source, maxPixels);
                means01 = converter.meanCmyk01(working, false);
                width = working.getWidth();
                height = working.getHeight();
            }
            double[] means = new double[]{means01[0] * 100, means01[1] * 100, means01[2] * 100, means01[3] * 100};
            List<RawInkCoverage> process = processInksFromMeans(means);
            return new InkCoverageAnalysis(
                    width,
                    height,
                    effectiveDpi,
                    destinationIccProfile,
                    process,
                    List.of(),
                    List.of(),
                    false,
                    false,
                    List.of(),
                    converter.colorEngine()
            );
        }
    }

    private InkCoverageAnalysis analyzePdf(
            InkEstimateRequest request,
            int dpi,
            String destinationIccProfile
    ) throws IOException {
        if (!pdfColorConverter.isAvailable()) {
            throw new UnsupportedInkFileException("Análisis PDF deshabilitado en el servidor");
        }

        int imageMaxEdge = properties.getRgbImageMaxEdge() > 0
                ? properties.getRgbImageMaxEdge()
                : FastInkRgbToCmyk.DEFAULT_IMAGE_MAX_EDGE;

        // Suma de coberturas % por página (cada página = un lado al tamaño widthCm×heightCm).
        // No promediar: si no, 2 páginas diluyen y dan MENOS tinta que 1 sola.
        double[] coverageSum = new double[4];
        int widthPx = 0;
        int heightPx = 0;
        Map<String, MergedSpot> spots = new LinkedHashMap<>();

        try (FastInkRgbToCmyk rgbToCmyk = new FastInkRgbToCmyk(
                iccProfileLoader,
                littleCms,
                destinationIccProfile,
                imageMaxEdge,
                properties.isBlackPointCompensation()
        );
             PDDocument doc = loadPdf(request)) {
            int pages = doc.getNumberOfPages();
            if (pages < 1) {
                throw new InkEstimationFailedException("El PDF no contiene páginas");
            }
            List<Integer> selected = request.getPages();
            InkPageSelection.validateAgainstDocument(selected, pages);

            List<Integer> analyzed = new ArrayList<>();
            for (int i = 0; i < pages; i++) {
                int pageOneBased = i + 1;
                if (!InkPageSelection.includes(selected, pageOneBased)) {
                    continue;
                }
                PDPage page = doc.getPage(i);
                float pw = page.getCropBox().getWidth();
                float ph = page.getCropBox().getHeight();
                int pageWpx = Math.max(1, Math.round(pw * dpi / 72f));
                int pageHpx = Math.max(1, Math.round(ph * dpi / 72f));
                widthPx = Math.max(widthPx, pageWpx);
                heightPx = Math.max(heightPx, pageHpx);

                PdfInkCoverageEngine engine = new PdfInkCoverageEngine(page, rgbToCmyk);
                engine.processPage(page);
                PdfInkCoverageEngine.PageInkCoverage coverage = engine.result();

                // Inventario solo de la(s) página(s) seleccionada(s)
                Map<String, String> inventory = PdfSpotColorScanner.scanPage(page);
                for (Map.Entry<String, String> entry : inventory.entrySet()) {
                    String exact = SpotColorantNames.exactSpotReference(entry.getKey());
                    if (exact == null) {
                        continue;
                    }
                    MergedSpot merged = spots.computeIfAbsent(exact, MergedSpot::new);
                    merged.fromInventory = true;
                    if (merged.swatchHex == null) {
                        merged.swatchHex = entry.getValue();
                    }
                }

                for (int c = 0; c < 4; c++) {
                    coverageSum[c] += coverage.processMeans()[c];
                }
                analyzed.add(pageOneBased);

                for (PdfInkCoverageEngine.SpotCoverage spot : coverage.spots()) {
                    String exact = SpotColorantNames.exactSpotReference(spot.name());
                    if (exact == null) {
                        continue;
                    }
                    MergedSpot merged = spots.computeIfAbsent(exact, MergedSpot::new);
                    merged.coverageSumPercent += spot.coveragePercent();
                    merged.measured = merged.measured || spot.measured();
                    if (merged.swatchHex == null) {
                        merged.swatchHex = spot.swatchHex();
                    }
                }
            }

            if (analyzed.isEmpty()) {
                throw new InkEstimationFailedException("No quedó ninguna página para analizar con pages="
                        + InkPageSelection.describe(selected));
            }

            List<RawInkCoverage> process = processInksFromMeans(coverageSum);
            // Inventario + pintura: siempre devolver nombre/referencia aunque cobertura sea 0%
            // (p. ej. Pantone declarado en recursos pero arte aplanado a CMYK).
            LinkedHashSet<String> declaredNames = new LinkedHashSet<>();
            List<RawInkCoverage> spotList = new ArrayList<>();
            for (MergedSpot spot : spots.values()) {
                String exact = SpotColorantNames.exactSpotReference(spot.name);
                if (exact == null) {
                    continue;
                }
                if (!spot.fromInventory && !spot.measured) {
                    continue;
                }
                double cov = round2(spot.coverageSumPercent);
                declaredNames.add(exact);
                spotList.add(new RawInkCoverage(
                        exact,
                        "SPOT",
                        cov,
                        spot.swatchHex,
                        spot.measured
                ));
            }

            boolean hasSpot = !declaredNames.isEmpty();
            return new InkCoverageAnalysis(
                    widthPx,
                    heightPx,
                    dpi,
                    destinationIccProfile,
                    process,
                    spotList,
                    analyzed,
                    true,
                    hasSpot,
                    List.copyOf(declaredNames),
                    rgbToCmyk.colorEngine()
            );
        }
    }

    private static List<RawInkCoverage> processInksFromMeans(double[] means) {
        return List.of(
                new RawInkCoverage("Cian", "C", round2(means[0]), "#00A3E0", true),
                new RawInkCoverage("Magenta", "M", round2(means[1]), "#EC008C", true),
                new RawInkCoverage("Amarillo", "Y", round2(means[2]), "#FFF200", true),
                new RawInkCoverage("Negro", "K", round2(means[3]), "#231F20", true)
        );
    }

    private ImageColorConverterAdapter.LoadedRaster loadRaster(InkEstimateRequest request) throws IOException {
        int maxPixels = properties.getMaxAnalysisPixels() > 0
                ? properties.getMaxAnalysisPixels()
                : FastInkRgbToCmyk.DEFAULT_RASTER_MAX_PIXELS;
        Path sourceFile = request.getSourceFile();
        if (sourceFile != null) {
            return imageColorConverter.loadRasterForInkCoverage(sourceFile, maxPixels);
        }
        return imageColorConverter.loadRasterForInkCoverage(request.getFileBytes(), maxPixels);
    }

    private static PDDocument loadPdf(InkEstimateRequest request) throws IOException {
        Path sourceFile = request.getSourceFile();
        if (sourceFile != null) {
            return Loader.loadPDF(sourceFile.toFile());
        }
        return Loader.loadPDF(new RandomAccessReadBuffer(request.getFileBytes()));
    }

    /**
     * Cobertura media por canal CMYK en % (0–100). Conservado para tests unitarios.
     */
    static double[] meanChannelCoveragePercent(BufferedImage cmyk) {
        double[] m = FastInkRgbToCmyk.meanChannels01(cmyk.getRaster());
        return new double[]{m[0] * 100, m[1] * 100, m[2] * 100, m[3] * 100};
    }

    private static boolean isCmyk(BufferedImage image) {
        ColorSpace cs = image.getColorModel().getColorSpace();
        return cs != null && cs.getNumComponents() == 4 && cs.getType() == ColorSpace.TYPE_CMYK;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static String extensionOf(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private static final class MergedSpot {
        private final String name;
        /** Suma de coveragePercent de cada página analizada. */
        private double coverageSumPercent;
        private String swatchHex;
        private boolean measured;
        private boolean fromInventory;

        private MergedSpot(String name) {
            this.name = name;
        }
    }
}
