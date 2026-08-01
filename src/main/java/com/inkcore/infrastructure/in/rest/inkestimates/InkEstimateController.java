package com.inkcore.infrastructure.in.rest.inkestimates;

import com.inkcore.domain.inkestimation.model.InkEstimateRequest;
import com.inkcore.domain.inkestimation.model.InkPageSelection;
import com.inkcore.domain.inkestimation.model.InkEstimateResult;
import com.inkcore.domain.inkestimation.ports.in.EstimateInkUseCase;
import com.inkcore.infrastructure.config.InkEstimationProperties;
import com.inkcore.infrastructure.in.rest.envelope.ApiErrorEnvelope;
import com.inkcore.infrastructure.in.rest.envelope.ApiResponseFactory;
import com.inkcore.infrastructure.in.rest.envelope.ApiSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.ApiErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.ApiSecuredErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.InkEstimateSuccessEnvelope;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;

@RestController
@RequestMapping("/api/v1/ink-estimates")
@Tag(
        name = "Estimación de tinta",
        description = "Consumo CMYK + spots Separation/DeviceN (PDFBox + ICC libres; factores g/cm² por canal; sin RIP de pago)"
)
@SecurityRequirement(name = "bearerAuth")
public class InkEstimateController {

    private final EstimateInkUseCase estimateInkUseCase;
    private final InkEstimationProperties properties;
    private final ApiResponseFactory responseFactory;

    public InkEstimateController(
            EstimateInkUseCase estimateInkUseCase,
            InkEstimationProperties properties,
            ApiResponseFactory responseFactory
    ) {
        this.estimateInkUseCase = estimateInkUseCase;
        this.properties = properties;
        this.responseFactory = responseFactory;
    }

    @PostMapping(
            value = "/estimate",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("isAuthenticated()")
    @Operation(
            operationId = "estimateInkConsumption",
            summary = "Estimar consumo de tinta",
            description = """
                    Multipart → envelope JSON (`headers`, `timestamp`, `data`).

                    Método libre (sin RIP de pago ni Ghostscript AGPL ni base Pantone de pago):

                    **PDF (PDFBox Apache 2.0)**
                    - Vectores/texto DeviceCMYK, Separation y DeviceN (área × tint)
                    - Imágenes CMYK/spot nativas vía getRawRaster
                    - RGB/Gray → ICC (perfil iccProfile / FOGRA39 por defecto)

                    **Raster (TwelveMonkeys BSD)**
                    - TIFF CMYK nativo; JPG/PNG/WEBP/GIF vía RGB→ICC

                    **Consumo**
                    - gramos = (cobertura%/100) × área_cm² × factor_canal × pliegos
                    - Sin gramsPerCm2: factores por canal (C/M/Y/K/spot) del servidor
                    - Con gramsPerCm2: mismo factor uniforme en todos los canales
                    - PDF multi-página: cobertura/gramos = SUMA de páginas seleccionadas
                      (cada página = un lado al tamaño width×height); no se promedia
                    - Spots: solo si hay pintura Separation/DeviceN con % > 0 en páginas elegidas
                      (no se listan Pantones a 0% aunque existan en recursos)
                    - Estimación comercial (no sustituye medición de plancha/RIP)
                    - PDF multi-trabajo: parámetro opcional pages (1-based, máx. 2), p. ej. "1,2" o "3,5"
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Estimación calculada (envelope JSON con processInks, spotInks y totales).",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = InkEstimateSuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "EstimacionOk",
                            summary = "Pliego 70×100 cm, 1000 ejemplares",
                            value = """
                                    {
                                      "headers": {
                                        "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                        "statusCode": 200,
                                        "code": "OK",
                                        "description": "Success"
                                      },
                                      "timestamp": "2026-07-31T12:00:00Z",
                                      "data": {
                                        "fileName": "arte.pdf",
                                        "contentType": "application/pdf",
                                        "widthCm": 70.0,
                                        "heightCm": 100.0,
                                        "areaCm2": 7000.0,
                                        "sheetCount": 1000,
                                        "dpi": 300,
                                        "gramsPerCm2AtFullCoverage": 0.00021,
                                        "widthPx": 8270,
                                        "heightPx": 11810,
                                        "iccProfileUsed": "FOGRA39.icc",
                                        "processInks": [
                                          {
                                            "name": "Cian",
                                            "channel": "C",
                                            "coveragePercent": 12.5,
                                            "swatchHex": "#00A3E0",
                                            "coverageMeasured": true,
                                            "gramsPerSheet": 0.18375,
                                            "gramsOrder": 183.75
                                          },
                                          {
                                            "name": "Magenta",
                                            "channel": "M",
                                            "coveragePercent": 8.0,
                                            "swatchHex": "#EC008C",
                                            "coverageMeasured": true,
                                            "gramsPerSheet": 0.1176,
                                            "gramsOrder": 117.6
                                          },
                                          {
                                            "name": "Amarillo",
                                            "channel": "Y",
                                            "coveragePercent": 15.0,
                                            "swatchHex": "#FFF200",
                                            "coverageMeasured": true,
                                            "gramsPerSheet": 0.2205,
                                            "gramsOrder": 220.5
                                          },
                                          {
                                            "name": "Negro",
                                            "channel": "K",
                                            "coveragePercent": 5.0,
                                            "swatchHex": "#231F20",
                                            "coverageMeasured": true,
                                            "gramsPerSheet": 0.0735,
                                            "gramsOrder": 73.5
                                          }
                                        ],
                                        "spotInks": [
                                          {
                                            "name": "PANTONE 185 C",
                                            "channel": "SPOT",
                                            "coveragePercent": 2.0,
                                            "swatchHex": "#E4002B",
                                            "coverageMeasured": true,
                                            "gramsPerSheet": 0.0294,
                                            "gramsOrder": 29.4
                                          }
                                        ],
                                        "processGramsPerSheet": 0.59535,
                                        "spotGramsPerSheet": 0.0294,
                                        "totalGramsPerSheet": 0.62475,
                                        "processGramsOrder": 595.35,
                                        "spotGramsOrder": 29.4,
                                        "totalGramsOrder": 624.75,
                                        "processingTimeMs": 1850,
                                        "originalSizeBytes": 2457600,
                                        "pagesAnalyzed": [1, 2],
                                        "spotInventoryVerified": true,
                                        "hasSpotColors": true,
                                        "declaredSpotColorNames": ["PANTONE 2925 C", "PANTONE 2915 C"]
                                      }
                                    }
                                    """
                    )
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Parámetros inválidos o formato no soportado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiErrorEnvelope.class),
                    examples = @ExampleObject(
                            name = "FormatoNoSoportado",
                            value = """
                                    {
                                      "headers": {
                                        "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                        "statusCode": 400,
                                        "code": "BAD_REQUEST",
                                        "description": "Extensión no permitida: .docx"
                                      },
                                      "timestamp": "2026-07-31T12:00:00Z",
                                      "path": "/InkCore-backend/api/v1/ink-estimates/estimate",
                                      "message": "Extensión no permitida: .docx. Use jpg, png, tif, tiff, webp, gif o pdf",
                                      "errors": null
                                    }
                                    """
                    )
            )
    )
    @ApiResponse(
            responseCode = "413",
            description = "Archivo demasiado grande (techo absoluto o área×DPI declarada)",
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
                                        "description": "El archivo supera el techo de seguridad"
                                      },
                                      "timestamp": "2026-07-31T12:00:00Z",
                                      "path": "/InkCore-backend/api/v1/ink-estimates/estimate",
                                      "message": "El archivo supera el techo de seguridad (536870912 bytes)",
                                      "errors": ["INK_FILE_TOO_LARGE"]
                                    }
                                    """
                    )
            )
    )
    @ApiResponse(
            responseCode = "422",
            description = "Fallo de análisis / archivo ilegible",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiErrorEnvelope.class),
                    examples = @ExampleObject(
                            name = "AnalisisFallido",
                            value = """
                                    {
                                      "headers": {
                                        "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                        "statusCode": 422,
                                        "code": "UNPROCESSABLE_ENTITY",
                                        "description": "Fallo al analizar cobertura de tinta"
                                      },
                                      "timestamp": "2026-07-31T12:00:00Z",
                                      "path": "/InkCore-backend/api/v1/ink-estimates/estimate",
                                      "message": "Fallo al analizar cobertura de tinta: PDF vacío",
                                      "errors": ["INK_ESTIMATION_FAILED"]
                                    }
                                    """
                    )
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    @RequestBody(
            required = true,
            description = "multipart/form-data: file + sheetCount (obligatorios); widthCm, heightCm, dpi, gramsPerCm2, iccProfile (opcionales)",
            content = @Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = @Schema(implementation = InkEstimateMultipartSchema.class),
                    encoding = {
                            @Encoding(name = "file", contentType = "application/octet-stream"),
                            @Encoding(name = "widthCm", contentType = "text/plain"),
                            @Encoding(name = "heightCm", contentType = "text/plain"),
                            @Encoding(name = "sheetCount", contentType = "text/plain"),
                            @Encoding(name = "dpi", contentType = "text/plain"),
                            @Encoding(name = "gramsPerCm2", contentType = "text/plain"),
                            @Encoding(name = "iccProfile", contentType = "text/plain"),
                            @Encoding(name = "pages", contentType = "text/plain")
                    },
                    examples = {
                            @ExampleObject(
                                    name = "PliegoColombia",
                                    summary = "Offset Colombia 70×100; gramsPerCm2 uniforme",
                                    value = """
                                            {
                                              "file": "arte.pdf",
                                              "widthCm": 70,
                                              "heightCm": 100,
                                              "sheetCount": 1000,
                                              "dpi": 300,
                                              "gramsPerCm2": 0.00021,
                                              "iccProfile": "FOGRA39.icc"
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "ImagenTiff",
                                    summary = "TIFF CMYK nativo (factores por canal del servidor)",
                                    value = """
                                            {
                                              "file": "separacion.tif",
                                              "widthCm": 50,
                                              "heightCm": 70,
                                              "sheetCount": 500,
                                              "dpi": 300,
                                              "iccProfile": "FOGRA39.icc"
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "PdfSpotSinOverride",
                                    summary = "PDF con spots; densidades por canal (sin gramsPerCm2)",
                                    value = """
                                            {
                                              "file": "arte-spot.pdf",
                                              "widthCm": 70,
                                              "heightCm": 100,
                                              "sheetCount": 2000,
                                              "iccProfile": "FOGRA39.icc"
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "PdfPaginasSeleccionadas",
                                    summary = "Solo páginas 1 y 3 (máx. 2) de un PDF multi-trabajo",
                                    value = """
                                            {
                                              "file": "varios-trabajos.pdf",
                                              "widthCm": 10,
                                              "heightCm": 5,
                                              "sheetCount": 1000,
                                              "pages": "1,3",
                                              "iccProfile": "FOGRA39.icc"
                                            }
                                            """
                            )
                    }
            )
    )
    public ResponseEntity<ApiSuccessEnvelope<InkEstimateResponse>> estimate(
            @Parameter(hidden = true) @RequestPart("file") MultipartFile file,
            @Parameter(hidden = true) @RequestParam(value = "widthCm", required = false) Double widthCm,
            @Parameter(hidden = true) @RequestParam(value = "heightCm", required = false) Double heightCm,
            @Parameter(hidden = true) @RequestParam(value = "sheetCount", required = false) Integer sheetCount,
            @Parameter(hidden = true) @RequestParam(value = "dpi", required = false) Integer dpi,
            @Parameter(hidden = true) @RequestParam(value = "gramsPerCm2", required = false) Double gramsPerCm2,
            @Parameter(hidden = true) @RequestParam(value = "iccProfile", required = false) String iccProfile,
            @Parameter(hidden = true) @RequestParam(value = "pages", required = false) String pages,
            @Parameter(hidden = true) Authentication authentication,
            HttpServletRequest httpRequest
    ) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo es obligatorio");
        }
        if (sheetCount == null || sheetCount <= 0) {
            throw new IllegalArgumentException("sheetCount es obligatorio y debe ser > 0");
        }
        validateExtension(file.getOriginalFilename());

        double w = widthCm != null ? widthCm : properties.getDefaultWidthCm();
        double h = heightCm != null ? heightCm : properties.getDefaultHeightCm();

        InkEstimateRequest request = new InkEstimateRequest(
                file.getBytes(),
                file.getOriginalFilename(),
                file.getContentType(),
                w,
                h,
                sheetCount,
                dpi,
                gramsPerCm2,
                extractUserId(authentication),
                iccProfile,
                InkPageSelection.parse(pages)
        );
        InkEstimateResult result = estimateInkUseCase.estimate(request);
        return responseFactory.okStandard(httpRequest, InkEstimateResponse.from(result));
    }

    private static void validateExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            throw new IllegalArgumentException("Nombre de archivo inválido");
        }
        String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (!ext.matches("jpg|jpeg|png|tif|tiff|webp|gif|pdf")) {
            throw new IllegalArgumentException(
                    "Extensión no permitida: ." + ext + ". Use jpg, png, tif, tiff, webp, gif o pdf"
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
