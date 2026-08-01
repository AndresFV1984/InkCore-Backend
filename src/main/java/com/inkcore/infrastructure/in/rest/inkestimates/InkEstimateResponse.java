package com.inkcore.infrastructure.in.rest.inkestimates;

import com.inkcore.domain.inkestimation.model.InkCoverage;
import com.inkcore.domain.inkestimation.model.InkEstimateResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(
        name = "InkEstimateResponse",
        description = """
                Resultado de estimación comercial de consumo de tinta.
                El archivo no se devuelve ni se guarda en disco (solo historial de metadatos en BD).
                Gramos por tinta = (coveragePercent/100) × areaCm2 × factor_canal × sheetCount.
                """
)
public record InkEstimateResponse(
        @Schema(description = "Nombre del archivo analizado", example = "arte.pdf")
        String fileName,

        @Schema(description = "MIME declarado en la petición", example = "application/pdf")
        String contentType,

        @Schema(description = "Ancho de pliego (cm)", example = "70")
        double widthCm,

        @Schema(description = "Alto de pliego (cm)", example = "100")
        double heightCm,

        @Schema(description = "Área (cm²) = widthCm × heightCm", example = "7000")
        double areaCm2,

        @Schema(description = "Cantidad de pliegos del pedido", example = "1000")
        int sheetCount,

        @Schema(
                description = "DPI de referencia usado (metadatos / validación / raster). En PDF vectorial no limita la medición de cobertura.",
                example = "300"
        )
        int dpi,

        @Schema(
                description = """
                        Factor g/cm² de referencia reportado: cian del perfil por canal,
                        o el override uniforme si se envió gramsPerCm2.
                        Los gramos de cada tinta pueden usar un factor distinto (Y/K/spot).
                        """,
                example = "0.00021"
        )
        double gramsPerCm2AtFullCoverage,

        @Schema(description = "Ancho de referencia analizado (px ≈ cropBox × dpi/72 en PDF)", example = "8270")
        int widthPx,

        @Schema(description = "Alto de referencia analizado (px)", example = "11810")
        int heightPx,

        @Schema(
                description = "Perfil ICC usado para RGB→CMYK. Canales CMYK/spot nativos no pasan por este perfil.",
                example = "FOGRA39.icc"
        )
        String iccProfileUsed,

        @Schema(description = """
                Tintas de proceso C/M/Y/K. coveragePercent es la SUMA de las páginas PDF seleccionadas
                (cada página = un lado al tamaño widthCm×heightCm). Así 2 páginas no diluyen vs 1.
                """)
        List<InkCoverageResponse> processInks,

        @Schema(description = """
                Tintas spot SOLO si en las páginas seleccionadas hay pintura Separation/DeviceN
                con cobertura > 0 (consumo real). Nunca lista Pantones a 0%.
                Si el PDF declara el Pantone en recursos pero pinta en CMYK, no aparece aquí:
                el azul va en processInks (C/M/Y/K).
                En multi-página, coveragePercent es la SUMA de las páginas seleccionadas.
                """)
        List<InkCoverageResponse> spotInks,

        @Schema(description = "Suma de gramos de proceso por pliego", example = "0.59535")
        double processGramsPerSheet,

        @Schema(description = "Suma de gramos spot por pliego (0 si coverageMeasured=false)", example = "0.0294")
        double spotGramsPerSheet,

        @Schema(description = "Gramos totales por pliego (proceso + spot)", example = "0.62475")
        double totalGramsPerSheet,

        @Schema(description = "Gramos de proceso del pedido (= processGramsPerSheet × sheetCount)", example = "595.35")
        double processGramsOrder,

        @Schema(description = "Gramos spot del pedido", example = "29.4")
        double spotGramsOrder,

        @Schema(description = "Gramos totales del pedido", example = "624.75")
        double totalGramsOrder,

        @Schema(description = "Duración del procesamiento (ms)", example = "1850")
        long processingTimeMs,

        @Schema(description = "Tamaño del archivo de entrada (bytes)", example = "2457600")
        long originalSizeBytes,

        @Schema(
                description = """
                        Páginas PDF analizadas (índices 1-based).
                        Vacío en rasters. Si no se envió pages, incluye todas las del documento.
                        Máximo 2 cuando se filtra con el parámetro pages.
                        """,
                example = "[1, 2]"
        )
        List<Integer> pagesAnalyzed,

        @Schema(
                description = """
                        true si se inventariaron Separation/DeviceN en recursos del PDF (páginas analizadas).
                        Si true y hasSpotColors=false, se puede afirmar que no hay Pantone/spot reportable
                        (se excluyen All/None y marcas técnicas). En raster siempre false.
                        """,
                example = "true"
        )
        boolean spotInventoryVerified,

        @Schema(
                description = """
                        true si en las páginas analizadas hay Separation/DeviceN reportables
                        (Pantones/spots reales del PDF). No usa metadatos XMP sueltos.
                        """,
                example = "true"
        )
        boolean hasSpotColors,

        @Schema(
                description = """
                        Nombres exactos de spots con cobertura > 0 en las páginas seleccionadas
                        (mismo criterio que spotInks). Vacío si solo hay Pantones declarados
                        en recursos pero el arte está en CMYK.
                        """,
                example = "[\"PANTONE 185 C\"]"
        )
        List<String> declaredSpotColorNames
) {
    public static InkEstimateResponse from(InkEstimateResult result) {
        return new InkEstimateResponse(
                result.getOriginalFileName(),
                result.getContentType(),
                result.getWidthCm(),
                result.getHeightCm(),
                result.getAreaCm2(),
                result.getSheetCount(),
                result.getDpi(),
                result.getGramsPerCm2AtFullCoverage(),
                result.getWidthPx(),
                result.getHeightPx(),
                result.getIccProfileUsed(),
                result.getProcessInks().stream().map(InkCoverageResponse::from).toList(),
                result.getSpotInks().stream().map(InkCoverageResponse::from).toList(),
                result.getProcessGramsPerSheet(),
                result.getSpotGramsPerSheet(),
                result.getTotalGramsPerSheet(),
                result.getProcessGramsOrder(),
                result.getSpotGramsOrder(),
                result.getTotalGramsOrder(),
                result.getProcessingTimeMs(),
                result.getOriginalSizeBytes(),
                result.getPagesAnalyzed(),
                result.isSpotInventoryVerified(),
                result.hasSpotColors(),
                result.getDeclaredSpotColorNames()
        );
    }

    @Schema(
            name = "InkCoverageResponse",
            description = "Cobertura y consumo de una tinta (proceso o spot)"
    )
    public record InkCoverageResponse(
            @Schema(
                    description = """
                            Nombre literal del PDF para SPOT (mismo string que declaredSpotColorNames).
                            Para proceso: Cian/Magenta/Amarillo/Negro.
                            """,
                    example = "PANTONE 2925 C"
            )
            String name,

            @Schema(description = "Canal", allowableValues = {"C", "M", "Y", "K", "SPOT"}, example = "C")
            String channel,

            @Schema(
                    description = "Cobertura media % sobre el área de página/pliego (0–100). Área × tint acumulado.",
                    example = "12.5"
            )
            double coveragePercent,

            @Schema(
                    description = "Swatch hex aproximado (proceso fijo o alternate RGB de Separation)",
                    example = "#00A3E0",
                    nullable = true
            )
            String swatchHex,

            @Schema(
                    description = """
                            true si se midió pintura Separation/DeviceN con cobertura > 0.
                            Los spots a 0% / solo inventariados no se incluyen en spotInks.
                            """,
                    example = "true"
            )
            boolean coverageMeasured,

            @Schema(description = "Gramos por pliego (= coverage/100 × areaCm2 × factor_canal)", example = "0.18375")
            double gramsPerSheet,

            @Schema(description = "Gramos del pedido (= gramsPerSheet × sheetCount)", example = "183.75")
            double gramsOrder
    ) {
        static InkCoverageResponse from(InkCoverage ink) {
            return new InkCoverageResponse(
                    ink.getName(),
                    ink.getChannel(),
                    ink.getCoveragePercent(),
                    ink.getSwatchHex(),
                    ink.isCoverageMeasured(),
                    ink.getGramsPerSheet(),
                    ink.getGramsOrder()
            );
        }
    }
}
