package com.inkcore.infrastructure.in.rest.colorconversions;

import com.inkcore.domain.colorconversion.model.OutputFormat;
import com.inkcore.domain.colorconversion.model.QualityPreset;
import com.inkcore.domain.colorconversion.model.RenderingIntent;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;

/**
 * Schema OpenAPI del body multipart (archivo binario + opciones).
 */
@Schema(
        name = "ColorConversionRequest",
        requiredProperties = {"file"},
        description = """
                Multipart de conversión (CMM LittleCMS + ICC).
                Calidad comercial recomendada: renderingIntent=PERCEPTUAL, outputFormat=TIFF,
                iccProfile=FOGRA39.icc, brightnessLift=0.12, vibranceBoost=0.28,
                softProofBrightnessMatch=true, blackPointCompensation=true
                (o qualityPreset=COMMERCIAL / VIVID / FIDELITY).
                Overrides explícitos ganan sobre qualityPreset.
                """
)
public class ColorConversionRequest {

    @Schema(
            description = "Archivo RGB binario. Extensiones: .tif, .tiff, .jpg, .jpeg, .png, .pdf",
            type = "string",
            format = "binary",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    public MultipartFile file;

    @Schema(
            description = "Intent ICC. PERCEPTUAL = fotos (recomendado). RELATIVE_COLORIMETRIC = logos/colores planos",
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
                    TIFF = TIFF CMYK LZW + ICC embebido (recomendado preprensa; default si la entrada es imagen).
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
                    Empuje hacia blanco en RGB (0–0.20) antes del CMYK.
                    Recomendado comercial: 0.12. CTP puro: 0. Si se omite, usa qualityPreset o default servidor.
                    """,
            example = "0.12",
            defaultValue = "0.12",
            minimum = "0",
            maximum = "0.20",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    public Float brightnessLift;

    @Schema(
            description = """
                    Refuerzo de saturación HSB (0–0.35) antes del CMYK.
                    Recomendado comercial: 0.28 (rango útil 0.22–0.35). CTP puro: 0.
                    Si se omite, usa qualityPreset o default servidor.
                    """,
            example = "0.28",
            defaultValue = "0.28",
            minimum = "0",
            maximum = "0.35",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    public Float vibranceBoost;

    @Schema(
            description = """
                    Si true (COMMERCIAL/VIVID): lift/vibrance + apertura CMY comercial en el CMYK CTP
                    y preview JPEG soft-proof alineado hacia el RGB de referencia (solo el JPEG de UI
                    recibe esa mezcla extra hacia pantalla). CTP puro: false (FIDELITY).
                    Si se omite, usa qualityPreset o default servidor.
                    """,
            example = "true",
            defaultValue = "true",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    public Boolean softProofBrightnessMatch;

    @Schema(
            description = "Presets FIDELITY|COMMERCIAL|VIVID (aliases: CTP, PHOTO, MAX, …). Solo aplica a ajustes no enviados explícitamente.",
            implementation = QualityPreset.class,
            example = "COMMERCIAL",
            allowableValues = {"FIDELITY", "COMMERCIAL", "VIVID"},
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    public QualityPreset qualityPreset;

    @Schema(
            description = """
                    Black Point Compensation (LittleCMS). Default servidor true si se omite.
                    Acerca la separación a Photoshop Convert to Profile (sobre todo con Relative).
                    Aplica a RGB→CMYK y al soft-proof CMYK→RGB del preview.
                    """,
            example = "true",
            defaultValue = "true",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    public Boolean blackPointCompensation;
}
