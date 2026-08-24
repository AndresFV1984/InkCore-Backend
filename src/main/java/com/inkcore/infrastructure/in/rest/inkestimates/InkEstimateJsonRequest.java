package com.inkcore.infrastructure.in.rest.inkestimates;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(
        name = "InkEstimateRequest",
        description = """
                JSON para estimar consumo de tinta (POST /api/v1/ink-estimates/estimate).
                Obligatorios: objectKey, sheetCount.
                El archivo ya está en object storage (presign + PUT). No se acepta multipart ni file.
                """,
        requiredProperties = {"objectKey", "sheetCount"}
)
public record InkEstimateJsonRequest(
        @Schema(
                description = "Clave S3 del arte original ya subido",
                example = "tmp/company/company-seed-001/ink-estimates/user-1/entry-1/original.pdf",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String objectKey,

        @Schema(
                description = "Ancho del área de impresión en cm. Default servidor: 70.",
                example = "70",
                minimum = "0.01"
        )
        Double widthCm,

        @Schema(
                description = "Alto del área de impresión en cm. Default servidor: 100.",
                example = "100",
                minimum = "0.01"
        )
        Double heightCm,

        @Schema(
                description = "Cantidad de pliegos del pedido.",
                example = "1000",
                minimum = "1",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Integer sheetCount,

        @Schema(
                description = "DPI de referencia. Default 300. Mínimo 72.",
                example = "300",
                minimum = "72"
        )
        Integer dpi,

        @Schema(
                description = "Override uniforme g/cm². Si se omite, factores por canal del servidor.",
                example = "0.00021"
        )
        Double gramsPerCm2,

        @Schema(
                description = "Perfil ICC de destino RGB→CMYK. Default FOGRA39.icc.",
                example = "FOGRA39.icc"
        )
        String iccProfile,

        @Schema(
                description = "Páginas PDF 1-based, máximo 2. Ignorado en raster.",
                example = "[1, 2]"
        )
        List<Integer> pages
) {
}
