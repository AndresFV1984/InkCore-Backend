package com.inkcore.domain.inkestimation.model;

import java.util.List;

/**
 * Resultado crudo del análisis de cobertura (sin aplicar gramos ni cantidad de pliegos).
 */
public record InkCoverageAnalysis(
        int widthPx,
        int heightPx,
        int dpiUsed,
        String iccProfileUsed,
        List<RawInkCoverage> processInks,
        /**
         * Spots reportables hallados en las páginas seleccionadas (inventario Separation/DeviceN
         * y/o pintura medida). Puede incluir cobertura 0% si el Pantone está declarado pero
         * el arte se pintó en CMYK ({@code coverageMeasured=false}).
         */
        List<RawInkCoverage> spotInks,
        /** Páginas PDF analizadas (1-based). Vacía en rasters. */
        List<Integer> pagesAnalyzed,
        /**
         * true si se ejecutó inventario/análisis Separation/DeviceN en páginas PDF.
         */
        boolean spotInventoryVerified,
        /**
         * true si hay al menos un spot reportable (inventario o medido) en las páginas seleccionadas.
         */
        boolean hasSpotColors,
        /**
         * Nombres exactos de spots reportables (alineado con spotInks; incluye 0%).
         */
        List<String> declaredSpotColorNames,
        /**
         * Motor de color usado para RGB→CMYK de proceso: {@code littlecms}.
         */
        String colorEngine
) {
    public InkCoverageAnalysis {
        pagesAnalyzed = pagesAnalyzed == null ? List.of() : List.copyOf(pagesAnalyzed);
        processInks = processInks == null ? List.of() : List.copyOf(processInks);
        spotInks = spotInks == null ? List.of() : List.copyOf(spotInks);
        declaredSpotColorNames = declaredSpotColorNames == null ? List.of() : List.copyOf(declaredSpotColorNames);
        if (colorEngine == null || colorEngine.isBlank()) {
            colorEngine = "littlecms";
        }
    }

    /** Compatibilidad tests/raster. */
    public InkCoverageAnalysis(
            int widthPx,
            int heightPx,
            int dpiUsed,
            String iccProfileUsed,
            List<RawInkCoverage> processInks,
            List<RawInkCoverage> spotInks
    ) {
        this(
                widthPx,
                heightPx,
                dpiUsed,
                iccProfileUsed,
                processInks,
                spotInks,
                List.of(),
                false,
                !spotInks.isEmpty(),
                List.of(),
                "littlecms"
        );
    }

    public InkCoverageAnalysis(
            int widthPx,
            int heightPx,
            int dpiUsed,
            String iccProfileUsed,
            List<RawInkCoverage> processInks,
            List<RawInkCoverage> spotInks,
            List<Integer> pagesAnalyzed
    ) {
        this(
                widthPx,
                heightPx,
                dpiUsed,
                iccProfileUsed,
                processInks,
                spotInks,
                pagesAnalyzed,
                false,
                spotInks != null && !spotInks.isEmpty(),
                List.of(),
                "littlecms"
        );
    }

    /** Compatibilidad tests con inventario/spots y sin colorEngine. */
    public InkCoverageAnalysis(
            int widthPx,
            int heightPx,
            int dpiUsed,
            String iccProfileUsed,
            List<RawInkCoverage> processInks,
            List<RawInkCoverage> spotInks,
            List<Integer> pagesAnalyzed,
            boolean spotInventoryVerified,
            boolean hasSpotColors,
            List<String> declaredSpotColorNames
    ) {
        this(
                widthPx,
                heightPx,
                dpiUsed,
                iccProfileUsed,
                processInks,
                spotInks,
                pagesAnalyzed,
                spotInventoryVerified,
                hasSpotColors,
                declaredSpotColorNames,
                "littlecms"
        );
    }

    public record RawInkCoverage(
            String name,
            String channel,
            double coveragePercent,
            String swatchHex,
            boolean coverageMeasured
    ) {
    }
}
