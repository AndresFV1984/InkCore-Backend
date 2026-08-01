package com.inkcore.infrastructure.in.rest.colorconversions;

import com.inkcore.domain.colorconversion.model.OutputFormat;
import com.inkcore.domain.colorconversion.model.RenderingIntent;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;

/**
 * Schema OpenAPI del body multipart (archivo binario + opciones).
 */
@Schema(name = "ColorConversionRequest", requiredProperties = {"file"})
public class ColorConversionRequest {

    @Schema(
            description = "Archivo RGB binario. Extensiones: .tif, .tiff, .jpg, .jpeg, .png, .pdf",
            type = "string",
            format = "binary",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    public MultipartFile file;

    @Schema(
            description = "Intent ICC. PERCEPTUAL = fotos (default). RELATIVE_COLORIMETRIC = logos/colores planos",
            implementation = RenderingIntent.class,
            example = "PERCEPTUAL",
            defaultValue = "PERCEPTUAL",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    public RenderingIntent renderingIntent;

    @Schema(
            description = """
                    Perfil ICC de destino CMYK según condición de impresión/CTP.
                    Catálogo: FOGRA39 (estucado EU), FOGRA51 (estucado v3), FOGRA47/52 (no estucado),
                    GRACoL2013 (US coated), SWOP2006 (US web), JapanColor2011Coated.
                    Solo los marcados available=true en GET /color-conversions/list están instalados.
                    Default FOGRA39.icc. Se aceptan alias (p. ej. ISOcoated_v2_eci.icc → FOGRA39).
                    """,
            example = "FOGRA39.icc",
            defaultValue = "FOGRA39.icc",
            allowableValues = {
                    "FOGRA39.icc",
                    "FOGRA51.icc",
                    "FOGRA47.icc",
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
                    Formato de salida CMYK (opcional).
                    TIFF = TIFF CMYK LZW + ICC embebido (default si la entrada es imagen).
                    PDF = PDF CMYK con ICCBased + OutputIntent (default si la entrada es PDF).
                    PDF→TIFF rasteriza a DPI configurado (pierde vectores; calidad de impresión ~300 dpi).
                    """,
            allowableValues = {"TIFF", "PDF"},
            example = "TIFF",
            defaultValue = "TIFF",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    public OutputFormat outputFormat;

    @Schema(
            description = """
                    Empuje hacia blanco en RGB (0–0.15) antes del CMYK.
                    Omítase o 0 = fidelidad CTP (default servidor). Valores bajos (0.02–0.05) si la prensa oscurece.
                    """,
            example = "0",
            defaultValue = "0",
            minimum = "0",
            maximum = "0.15",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    public Float brightnessLift;

    @Schema(
            description = """
                    Refuerzo de saturación HSB (0–0.25) antes del CMYK.
                    Omítase o 0 = fidelidad CTP. 0.10–0.18 para fotos comerciales más vivas.
                    """,
            example = "0",
            defaultValue = "0",
            minimum = "0",
            maximum = "0.25",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    public Float vibranceBoost;

    @Schema(
            description = """
                    Si true, escala tinta CMYK tras soft-proof para recuperar brillo percibido.
                    false (default CTP) = conversión ICC pura sin retocar separaciones.
                    """,
            example = "false",
            defaultValue = "false",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    public Boolean softProofBrightnessMatch;
}
