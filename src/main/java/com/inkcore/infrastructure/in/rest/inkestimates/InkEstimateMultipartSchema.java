package com.inkcore.infrastructure.in.rest.inkestimates;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;

/**
 * Schema OpenAPI del body multipart (archivo + parámetros de pliego).
 */
@Schema(
        name = "InkEstimateRequest",
        description = """
                Multipart para estimar consumo de tinta.
                Obligatorios: file, sheetCount.
                Opcionales: widthCm/heightCm (default 70×100), dpi, gramsPerCm2, iccProfile, pages (PDF, máx. 2).
                """,
        requiredProperties = {"file", "sheetCount"}
)
public class InkEstimateMultipartSchema {

    @Schema(
            description = """
                    Archivo de preprensa.
                    PDF: cobertura vectorial/texto DeviceCMYK + Separation/DeviceN e imágenes CMYK nativas (PDFBox).
                    Raster: TIFF CMYK nativo preferido; JPG/PNG/WEBP/GIF vía RGB→LittleCMS+ICC.
                    Extensiones: .jpg, .jpeg, .png, .tif, .tiff, .webp, .gif, .pdf
                    """,
            type = "string",
            format = "binary",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    public MultipartFile file;

    @Schema(
            description = "Ancho del área de impresión en cm. Default servidor: 70 (offset Colombia).",
            example = "70",
            minimum = "0.01",
            defaultValue = "70",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    public Double widthCm;

    @Schema(
            description = "Alto del área de impresión en cm. Default servidor: 100.",
            example = "100",
            minimum = "0.01",
            defaultValue = "100",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    public Double heightCm;

    @Schema(
            description = "Cantidad de pliegos del pedido (multiplica gramos por pliego → pedido).",
            example = "1000",
            minimum = "1",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    public Integer sheetCount;

    @Schema(
            description = """
                    DPI de referencia para metadatos/raster y tamaño teórico de validación.
                    Default 300. En PDF la cobertura vectorial/spot no se remuestrea a este DPI
                    (se mide en espacio de página). En imágenes se usa el DPI embebido si existe.
                    """,
            example = "300",
            minimum = "72",
            defaultValue = "300",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    public Integer dpi;

    @Schema(
            description = """
                    Override uniforme de densidad (g/cm² al 100 % de cobertura) para C/M/Y/K/spot.
                    Si se omite, el servidor aplica factores por canal configurados
                    (default aprox.: C/M 0.00021, Y 0.00020, K 0.00022, spot 0.00025).
                    """,
            example = "0.00021",
            minimum = "0.0000001",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    public Double gramsPerCm2;

    @Schema(
            description = """
                    Perfil ICC de destino para RGB→CMYK vía LittleCMS (colores/imágenes RGB en PDF o raster RGB).
                    Debe existir en el servidor (GET /color-conversions/list). Default FOGRA39.icc (CTP Colombia).
                    No aplica a canales DeviceCMYK / Separation nativos (se leen directos).
                    """,
            example = "FOGRA39.icc",
            defaultValue = "FOGRA39.icc",
            allowableValues = {
                    "FOGRA39.icc",
                    "FOGRA51.icc",
                    "FOGRA52.icc",
                    "GRACoL2013.icc",
                    "SWOP2006_Coated3v2.icc",
                    "JapanColor2011Coated.icc"
            },
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    public String iccProfile;

    @Schema(
            description = """
                    Páginas del PDF a estimar (1-based), máximo 2. Ejemplos: "1", "1,2", "3,5".
                    Si se omite, se analizan todas las páginas.
                    Ignorado en archivos raster (JPG/PNG/TIFF/...).
                    Útil cuando el PDF agrupa varios trabajos.
                    """,
            example = "1,2",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    public String pages;
}
