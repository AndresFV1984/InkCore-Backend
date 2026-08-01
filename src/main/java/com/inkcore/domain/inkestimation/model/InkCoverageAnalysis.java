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
         * Spots con cobertura medida &gt; 0 en las páginas seleccionadas.
         * No incluye pantones solo declarados en recursos (sin pintura Separation).
         */
        List<RawInkCoverage> spotInks,
        /** Páginas PDF analizadas (1-based). Vacía en rasters. */
        List<Integer> pagesAnalyzed,
        /**
         * true si se ejecutó inventario/análisis Separation/DeviceN en páginas PDF.
         */
        boolean spotInventoryVerified,
        /**
         * true si hay al menos un spot con cobertura &gt; 0 en las páginas seleccionadas.
         */
        boolean hasSpotColors,
        /**
         * Nombres exactos de spots con cobertura &gt; 0 (alineado con spotInks).
         */
        List<String> declaredSpotColorNames
) {
    public InkCoverageAnalysis {
        pagesAnalyzed = pagesAnalyzed == null ? List.of() : List.copyOf(pagesAnalyzed);
        processInks = processInks == null ? List.of() : List.copyOf(processInks);
        spotInks = spotInks == null ? List.of() : List.copyOf(spotInks);
        declaredSpotColorNames = declaredSpotColorNames == null ? List.of() : List.copyOf(declaredSpotColorNames);
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
                List.of()
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
                List.of()
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
