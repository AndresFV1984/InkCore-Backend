package com.inkcore.infrastructure.out.inkestimation;

import com.inkcore.domain.inkestimation.model.InkConsumptionCalculator;
import com.inkcore.domain.inkestimation.model.InkCoverageAnalysis;
import com.inkcore.domain.inkestimation.model.InkDensityFactors;
import com.inkcore.domain.inkestimation.model.InkEstimateRequest;
import com.inkcore.domain.inkestimation.model.InkEstimateResult;
import com.inkcore.infrastructure.config.ColorConversionProperties;
import com.inkcore.infrastructure.config.InkEstimationProperties;
import com.inkcore.infrastructure.out.cache.InMemoryIccProfileCacheAdapter;
import com.inkcore.infrastructure.out.colorconversion.ColorConversionTestSupport;
import com.inkcore.infrastructure.out.colorconversion.IccProfileLoader;
import com.inkcore.infrastructure.out.colorconversion.ImageColorConverterAdapter;
import com.inkcore.infrastructure.out.colorconversion.PdfColorConverterAdapter;
import com.inkcore.infrastructure.out.colorconversion.lcms.LittleCmsColorConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RealPdfInkEstimateRegressionTest {

    private static final Path SAMPLE = Path.of(
            "E:/Desarrollos Personales/Imagenes con Pantone/Actividad-Fisica-Deportes-Presencial-MED-CVS.pdf"
    );

    static boolean sampleExists() {
        return Files.isRegularFile(SAMPLE);
    }

    private InkCoverageAnalyzerAdapter analyzer;
    private InkDensityFactors density;

    @BeforeEach
    void setUp() {
        ColorConversionProperties colorProps = new ColorConversionProperties();
        colorProps.setPdfEnabled(true);
        InkEstimationProperties inkProps = new InkEstimationProperties();
        IccProfileLoader icc = new IccProfileLoader(new InMemoryIccProfileCacheAdapter(colorProps));
        LittleCmsColorConverter lcms = ColorConversionTestSupport.littleCms(colorProps);
        ImageColorConverterAdapter images = new ImageColorConverterAdapter(icc, colorProps, lcms);
        analyzer = new InkCoverageAnalyzerAdapter(
                images,
                new PdfColorConverterAdapter(colorProps, images),
                icc,
                lcms,
                inkProps
        );
        density = InkDensityFactors.uniform(0.00021);
    }

    @Test
    @EnabledIf("sampleExists")
    void flattenedPantones_listedAtZero_andTwoPages_sum() throws Exception {
        byte[] bytes = Files.readAllBytes(SAMPLE);

        InkEstimateResult page1 = estimate(bytes, List.of(1));
        InkEstimateResult page2 = estimate(bytes, List.of(2));
        InkEstimateResult both = estimate(bytes, List.of(1, 2));

        assertTrue(page1.isSpotInventoryVerified());
        // Este PDF declara Pantones en recursos de p.1 pero pinta en CMYK → listar a 0% (sin gramos)
        assertTrue(page1.hasSpotColors());
        assertFalse(page1.getSpotInks().isEmpty(), "Debe listar Pantones inventariados a 0%: " + page1.getSpotInks());
        assertTrue(
                page1.getDeclaredSpotColorNames().contains("PANTONE Medium Blue C"),
                "faltó Medium Blue C: " + page1.getDeclaredSpotColorNames()
        );
        assertTrue(
                page1.getDeclaredSpotColorNames().contains("PANTONE 2925 C"),
                "faltó 2925 C: " + page1.getDeclaredSpotColorNames()
        );
        assertTrue(
                page1.getDeclaredSpotColorNames().contains("PANTONE 2915 C"),
                "faltó 2915 C: " + page1.getDeclaredSpotColorNames()
        );
        assertTrue(
                page1.getSpotInks().stream().allMatch(s -> s.getCoveragePercent() == 0.0 && !s.isCoverageMeasured()),
                "Inventario aplanado a CMYK: cobertura 0 y coverageMeasured=false: " + page1.getSpotInks()
        );
        assertEquals(0.0, page1.getSpotGramsPerSheet(), 1e-9);

        double sumSides = page1.getTotalGramsPerSheet() + page2.getTotalGramsPerSheet();
        assertTrue(
                both.getTotalGramsPerSheet() > Math.max(page1.getTotalGramsPerSheet(), page2.getTotalGramsPerSheet()),
                "2 páginas no puede dar menos que 1. page1=" + page1.getTotalGramsPerSheet()
                        + " page2=" + page2.getTotalGramsPerSheet()
                        + " both=" + both.getTotalGramsPerSheet()
        );
        assertTrue(
                Math.abs(both.getTotalGramsPerSheet() - sumSides) / Math.max(sumSides, 1e-9) < 0.05,
                "both≈page1+page2: both=" + both.getTotalGramsPerSheet() + " sum=" + sumSides
        );
    }

    private InkEstimateResult estimate(byte[] bytes, List<Integer> pages) {
        InkEstimateRequest req = new InkEstimateRequest(
                bytes,
                "Actividad-Fisica-Deportes-Presencial-MED-CVS.pdf",
                "application/pdf",
                70.0,
                100.0,
                1,
                72,
                null,
                "test",
                "FOGRA39.icc",
                pages
        );
        InkCoverageAnalysis analysis = analyzer.analyze(req, 72, "FOGRA39.icc");
        return InkConsumptionCalculator.build(req, analysis, density, 0L);
    }
}
