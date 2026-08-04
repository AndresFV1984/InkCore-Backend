package com.inkcore.infrastructure.in.rest.colorconversions;

import com.inkcore.domain.colorconversion.model.ConversionRequest;
import com.inkcore.domain.colorconversion.model.ConversionResult;
import com.inkcore.domain.colorconversion.model.IccProfileInfo;
import com.inkcore.domain.colorconversion.model.OutputFormat;
import com.inkcore.domain.colorconversion.model.QualityPreset;
import com.inkcore.domain.colorconversion.model.RenderingIntent;
import com.inkcore.domain.colorconversion.ports.in.ConvertColorSpaceUseCase;
import com.inkcore.domain.colorconversion.ports.in.ListIccProfilesUseCase;
import com.inkcore.infrastructure.in.rest.envelope.ApiErrorEnvelope;
import com.inkcore.infrastructure.in.rest.envelope.ApiResponseFactory;
import com.inkcore.infrastructure.in.rest.envelope.ApiSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.ApiErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.ApiSecuredErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.ColorConversionSuccessEnvelope;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/v1/color-conversions")
@Tag(
        name = "Conversión de color",
        description = """
                Conversión RGB → CMYK con LittleCMS (lcms2 empaquetado) + perfiles ICC.
                Convert: multipart → JSON con fileBase64 (CMYK) y previewRgbBase64 (JPEG soft-proof para UI).
                Defaults de calidad comercial: brightnessLift=0.12, vibranceBoost=0.28,
                softProofBrightnessMatch=true, blackPointCompensation=true (o qualityPreset=COMMERCIAL).
                """
)
@SecurityRequirement(name = "bearerAuth")
public class ColorConversionController {

    private final ConvertColorSpaceUseCase convertColorSpaceUseCase;
    private final ListIccProfilesUseCase listIccProfilesUseCase;
    private final ApiResponseFactory responseFactory;

    public ColorConversionController(
            ConvertColorSpaceUseCase convertColorSpaceUseCase,
            ListIccProfilesUseCase listIccProfilesUseCase,
            ApiResponseFactory responseFactory
    ) {
        this.convertColorSpaceUseCase = convertColorSpaceUseCase;
        this.listIccProfilesUseCase = listIccProfilesUseCase;
        this.responseFactory = responseFactory;
    }

    @GetMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "listColorConversionIccProfiles",
            summary = "Listar perfiles ICC de destino",
            description = """
                    Catálogo de perfiles CMYK según tipo de papel/prensa (FOGRA, GRACoL, SWOP, Japan Color).
                    `available=true` indica que el .icc está instalado en el servidor y se puede usar en `convert`.
                    Para instalar más perfiles: copiar el .icc a classpath:/color-profiles/ (ver README del módulo).
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Catálogo de perfiles ICC",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiSuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "CatalogoIcc",
                            value = """
                                    {
                                      "headers": {
                                        "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                        "statusCode": 200,
                                        "code": "OK",
                                        "description": "Success"
                                      },
                                      "timestamp": "2026-07-31T12:00:00Z",
                                      "data": [
                                        {
                                          "fileName": "FOGRA39.icc",
                                          "title": "Offset estucado Europa (ISO Coated v2 / FOGRA39)",
                                          "useCase": "Papel couché/estucado offset. Estándar CTP europeo y LATAM más usado.",
                                          "paperClass": "COATED_OFFSET",
                                          "aliases": ["FOGRA39.icc", "ISOcoated_v2_eci.icc", "ISOCoatedV2.icc", "CoatedFOGRA39.icc"],
                                          "available": true
                                        },
                                        {
                                          "fileName": "FOGRA51.icc",
                                          "title": "Offset estucado Europa v3 (PSO Coated v3 / FOGRA51)",
                                          "useCase": "Estucado moderno (sustituye FOGRA39 en muchas imprentas).",
                                          "paperClass": "COATED_OFFSET",
                                          "aliases": ["FOGRA51.icc", "PSOcoated_v3.icc"],
                                          "available": false
                                        }
                                      ]
                                    }
                                    """
                    )
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<List<IccProfileResponse>>> listIccProfiles(
            HttpServletRequest httpRequest
    ) {
        List<IccProfileResponse> data = listIccProfilesUseCase.listDestinationProfiles().stream()
                .map(ColorConversionController::toResponse)
                .toList();
        return responseFactory.okStandard(httpRequest, data);
    }

    private static IccProfileResponse toResponse(IccProfileInfo info) {
        return new IccProfileResponse(
                info.fileName(),
                info.title(),
                info.useCase(),
                info.paperClass(),
                info.aliases(),
                info.available()
        );
    }

    @PostMapping(
            value = "/convert",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "convertColorSpace",
            summary = "Convertir archivo RGB a CMYK",
            description = """
                    Multipart binario → envelope JSON (`headers`, `timestamp`, `data`).

                    CMM: LittleCMS 2 (nativa empaquetada en el JAR; sin instalación manual).
                    Soft-proof y separación ICC usan el mismo motor.

                    ## Contrato front (calidad)

                    **Request recomendado (fotos / comercial):**
                    - `renderingIntent=PERCEPTUAL`
                    - `outputFormat=TIFF`
                    - `iccProfile=FOGRA39.icc` (o el del catálogo / imprenta)
                    - `brightnessLift=0.12` (rango 0–0.20)
                    - `vibranceBoost=0.28` (rango 0–0.35)
                    - `softProofBrightnessMatch=true` (recupera brillo/croma del contenido tras ICC)
                    - `blackPointCompensation=true` (default servidor; acerca a Photoshop Convert to Profile)
                    Alternativa corta: `qualityPreset=COMMERCIAL`.

                    **Más punch:** `qualityPreset=VIVID` (lift 0.16 / vibrance 0.35 / softProof true).

                    **CTP puro (sin retoque):** `qualityPreset=FIDELITY` o lift=0, vibrance=0, softProof=false.

                    **Prioridad:** overrides explícitos (`brightnessLift` / `vibranceBoost` /
                    `softProofBrightnessMatch` / `blackPointCompensation`) ganan sobre `qualityPreset`.

                    ## Respuesta — uso obligatorio

                    - **Vista previa UI:** `data.previewRgbBase64` + `data.previewContentType` (`image/jpeg`).
                      No renderizar el TIFF/PDF CMYK en el navegador.
                    - **Descarga / CTP:** `data.fileBase64` + `data.contentType` + `data.fileName`.

                    ## Otros

                    - `iccProfile`: catálogo en GET `/color-conversions/list` (`available=true`).
                    - PDF→TIFF rasteriza (~300 dpi; se acepta pérdida de vectores).
                    - No se guarda el archivo en disco.
                    - Si LittleCMS no carga: error claro (no hay fallback silencioso en CTP).
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = """
                    Conversión exitosa. Usar previewRgbBase64 para UI y fileBase64 para descarga.
                    contentType/fileName según outputFormat.
                    """,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ColorConversionSuccessEnvelope.class),
                    examples = {
                            @ExampleObject(
                                    name = "SalidaTiffComercial",
                                    summary = "TIFF + preview RGB (calidad comercial)",
                                    value = """
                                            {
                                              "headers": {
                                                "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                                "statusCode": 200,
                                                "code": "OK",
                                                "description": "Success"
                                              },
                                              "timestamp": "2026-07-30T12:00:00Z",
                                              "data": {
                                                "fileName": "foto_CMYK.tif",
                                                "contentType": "image/tiff",
                                                "fileBase64": "SUkqAAgAAAASAP4ABAABAAAAAAAAAAABBAABAAAAwAkAAAEBBAABAAAA",
                                                "originalSizeBytes": 1048576,
                                                "finalSizeBytes": 1400000,
                                                "widthPx": 2400,
                                                "heightPx": 3000,
                                                "processingTimeMs": 850,
                                                "renderingIntent": "PERCEPTUAL",
                                                "iccProfile": "FOGRA39.icc",
                                                "brightnessLift": 0.12,
                                                "vibranceBoost": 0.28,
                                                "softProofBrightnessMatch": true,
                                                "qualityPreset": null,
                                                "previewRgbBase64": "/9j/4AAQSkZJRgABAQAAAQABAAD",
                                                "previewContentType": "image/jpeg",
                                                "softProofLumaRatio": 0.97,
                                                "sizeIncreaseExpected": true
                                              }
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "SalidaTiffCtp",
                                    summary = "TIFF CTP fidelidad (sin retoque)",
                                    value = """
                                            {
                                              "headers": {
                                                "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                                "statusCode": 200,
                                                "code": "OK",
                                                "description": "Success"
                                              },
                                              "timestamp": "2026-07-30T12:00:00Z",
                                              "data": {
                                                "fileName": "arte_CMYK.tif",
                                                "contentType": "image/tiff",
                                                "fileBase64": "SUkqAAgAAAASAP4ABAABAAAAAAAAAAABBAABAAAAwAkAAAEBBAABAAAA",
                                                "originalSizeBytes": 1048576,
                                                "finalSizeBytes": 1400000,
                                                "widthPx": 2400,
                                                "heightPx": 3000,
                                                "processingTimeMs": 850,
                                                "renderingIntent": "PERCEPTUAL",
                                                "iccProfile": "FOGRA39.icc",
                                                "brightnessLift": 0,
                                                "vibranceBoost": 0,
                                                "softProofBrightnessMatch": false,
                                                "qualityPreset": "FIDELITY",
                                                "previewRgbBase64": "/9j/4AAQSkZJRgABAQAAAQABAAD",
                                                "previewContentType": "image/jpeg",
                                                "softProofLumaRatio": null,
                                                "sizeIncreaseExpected": true
                                              }
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "SalidaPdf",
                                    summary = "Respuesta PDF CMYK (sin preview RGB)",
                                    value = """
                                            {
                                              "headers": {
                                                "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                                "statusCode": 200,
                                                "code": "OK",
                                                "description": "Success"
                                              },
                                              "timestamp": "2026-07-30T12:00:00Z",
                                              "data": {
                                                "fileName": "logo_CMYK.pdf",
                                                "contentType": "application/pdf",
                                                "fileBase64": "JVBERi0xLjQKJeLjz9MKMSAwIG9iago8PC9UeXBlL0NhdGFsb2c+PgplbmRvYmoK",
                                                "originalSizeBytes": 512000,
                                                "finalSizeBytes": 780000,
                                                "widthPx": 2400,
                                                "heightPx": 3000,
                                                "processingTimeMs": 1200,
                                                "renderingIntent": "RELATIVE_COLORIMETRIC",
                                                "iccProfile": "FOGRA39.icc",
                                                "brightnessLift": 0,
                                                "vibranceBoost": 0,
                                                "softProofBrightnessMatch": false,
                                                "qualityPreset": null,
                                                "previewRgbBase64": null,
                                                "previewContentType": null,
                                                "sizeIncreaseExpected": true
                                              }
                                            }
                                            """
                            )
                    }
            )
    )
    @ApiResponse(
            responseCode = "413",
            description = "Archivo supera el tamaño máximo permitido",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiErrorEnvelope.class),
                    examples = @ExampleObject(
                            name = "ArchivoDemasiadoGrande",
                            value = """
                                    {
                                      "headers": {
                                        "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                        "statusCode": 413,
                                        "code": "PAYLOAD_TOO_LARGE",
                                        "description": "El archivo supera el tamaño máximo permitido"
                                      },
                                      "timestamp": "2026-07-30T12:00:00Z",
                                      "path": "/InkCore-backend/api/v1/color-conversions/convert",
                                      "message": "El archivo supera el tamaño máximo permitido",
                                      "errors": null
                                    }
                                    """
                    )
            )
    )
    @ApiResponse(
            responseCode = "422",
            description = "Regla de negocio incumplida (integridad, perfil ICC, conversión)",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiErrorEnvelope.class),
                    examples = @ExampleObject(
                            name = "IntegridadFallida",
                            value = """
                                    {
                                      "headers": {
                                        "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                        "statusCode": 422,
                                        "code": "UNPROCESSABLE_ENTITY",
                                        "description": "Dimensiones distintas tras la conversión"
                                      },
                                      "timestamp": "2026-07-30T12:00:00Z",
                                      "path": "/InkCore-backend/api/v1/color-conversions/convert",
                                      "message": "Dimensiones distintas tras la conversión",
                                      "errors": ["COLOR_CONVERSION_INTEGRITY", "INTEGRITY_GUARANTEE_FAILED"]
                                    }
                                    """
                    )
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    @RequestBody(
            required = true,
            description = "Formulario multipart/form-data (archivo binario + opciones)",
            content = @Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = @Schema(implementation = ColorConversionRequest.class),
                    encoding = {
                            @Encoding(name = "file", contentType = "application/octet-stream"),
                            @Encoding(name = "renderingIntent", contentType = "text/plain"),
                            @Encoding(name = "iccProfile", contentType = "text/plain"),
                            @Encoding(name = "outputFormat", contentType = "text/plain"),
                            @Encoding(name = "brightnessLift", contentType = "text/plain"),
                            @Encoding(name = "vibranceBoost", contentType = "text/plain"),
                            @Encoding(name = "softProofBrightnessMatch", contentType = "text/plain"),
                            @Encoding(name = "qualityPreset", contentType = "text/plain"),
                            @Encoding(name = "blackPointCompensation", contentType = "text/plain")
                    },
                    examples = {
                            @ExampleObject(
                                    name = "FotoComercialExplicito",
                                    summary = "Recomendado front: overrides comerciales (calidad UI)",
                                    value = """
                                            {
                                              "file": "foto_rgb.jpg",
                                              "renderingIntent": "PERCEPTUAL",
                                              "iccProfile": "FOGRA39.icc",
                                              "outputFormat": "TIFF",
                                              "brightnessLift": 0.12,
                                              "vibranceBoost": 0.28,
                                              "softProofBrightnessMatch": true,
                                              "blackPointCompensation": true
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "FotoComercialPreset",
                                    summary = "Atajo: qualityPreset=COMMERCIAL",
                                    value = """
                                            {
                                              "file": "foto_rgb.jpg",
                                              "renderingIntent": "PERCEPTUAL",
                                              "iccProfile": "FOGRA39.icc",
                                              "outputFormat": "TIFF",
                                              "qualityPreset": "COMMERCIAL"
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "CtpFidelidad",
                                    summary = "Imagen → TIFF CTP (sin retoque)",
                                    value = """
                                            {
                                              "file": "arte_rgb.tif",
                                              "renderingIntent": "PERCEPTUAL",
                                              "iccProfile": "FOGRA39.icc",
                                              "outputFormat": "TIFF",
                                              "qualityPreset": "FIDELITY"
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "Vivid",
                                    summary = "Máximo realce (qualityPreset=VIVID)",
                                    value = """
                                            {
                                              "file": "foto_rgb.jpg",
                                              "renderingIntent": "PERCEPTUAL",
                                              "iccProfile": "FOGRA39.icc",
                                              "outputFormat": "TIFF",
                                              "qualityPreset": "VIVID"
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "SalidaPdf",
                                    summary = "Logo → PDF CMYK",
                                    value = """
                                            {
                                              "file": "logo_rgb.png",
                                              "renderingIntent": "RELATIVE_COLORIMETRIC",
                                              "iccProfile": "FOGRA39.icc",
                                              "outputFormat": "PDF"
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "EntradaPdf",
                                    summary = "PDF → PDF CMYK",
                                    value = """
                                            {
                                              "file": "arte_rgb.pdf",
                                              "renderingIntent": "PERCEPTUAL",
                                              "iccProfile": "FOGRA39.icc",
                                              "outputFormat": "PDF"
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "PdfATiff",
                                    summary = "PDF → TIFF CMYK (rasterizado)",
                                    value = """
                                            {
                                              "file": "arte_rgb.pdf",
                                              "renderingIntent": "PERCEPTUAL",
                                              "iccProfile": "FOGRA39.icc",
                                              "outputFormat": "TIFF",
                                              "qualityPreset": "COMMERCIAL"
                                            }
                                            """
                            )
                    }
            )
    )
    public ResponseEntity<ApiSuccessEnvelope<ColorConversionResponse>> convert(
            @Parameter(hidden = true)
            @RequestPart("file") MultipartFile file,
            @Parameter(hidden = true)
            @RequestParam(value = "renderingIntent", required = false) String renderingIntent,
            @Parameter(hidden = true)
            @RequestParam(value = "iccProfile", required = false) String iccProfile,
            @Parameter(hidden = true)
            @RequestParam(value = "outputFormat", required = false) String outputFormat,
            @Parameter(hidden = true)
            @RequestParam(value = "brightnessLift", required = false) String brightnessLift,
            @Parameter(hidden = true)
            @RequestParam(value = "vibranceBoost", required = false) String vibranceBoost,
            @Parameter(hidden = true)
            @RequestParam(value = "softProofBrightnessMatch", required = false) String softProofBrightnessMatch,
            @Parameter(hidden = true)
            @RequestParam(value = "qualityPreset", required = false) String qualityPreset,
            @Parameter(hidden = true)
            @RequestParam(value = "blackPointCompensation", required = false) String blackPointCompensation,
            @Parameter(hidden = true) Authentication authentication,
            HttpServletRequest httpRequest
    ) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo es obligatorio");
        }
        validateExtension(file.getOriginalFilename());

        ConversionRequest request = new ConversionRequest(
                file.getBytes(),
                file.getOriginalFilename(),
                file.getContentType(),
                RenderingIntent.fromParam(renderingIntent),
                iccProfile,
                extractUserId(authentication),
                OutputFormat.fromParam(outputFormat),
                ConversionRequest.parseBrightnessLift(brightnessLift),
                ConversionRequest.parseVibranceBoost(vibranceBoost),
                ConversionRequest.parseSoftProofBrightnessMatch(softProofBrightnessMatch),
                QualityPreset.fromParam(qualityPreset),
                ConversionRequest.parseBlackPointCompensation(blackPointCompensation)
        );
        ConversionResult result = convertColorSpaceUseCase.convert(request);
        return responseFactory.okStandard(httpRequest, ColorConversionResponse.from(result));
    }

    private static void validateExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            throw new IllegalArgumentException("Nombre de archivo inválido");
        }
        String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (!ext.matches("tif|tiff|jpg|jpeg|png|pdf")) {
            throw new IllegalArgumentException(
                    "Extensión no permitida: ." + ext + ". Use .tif, .tiff, .jpg, .jpeg, .png o .pdf"
            );
        }
    }

    private static String extractUserId(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            return jwt.getSubject();
        }
        return authentication.getName();
    }
}
