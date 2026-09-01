package com.inkcore.infrastructure.in.rest.inkestimates;

import com.inkcore.application.inkestimation.usecase.EstimateInkFromMultipartUseCase;
import com.inkcore.application.inkestimation.usecase.EstimateInkFromMultipartUseCase.EstimateInkFromMultipartCommand;
import com.inkcore.application.inkestimation.usecase.EstimateInkFromObjectKeyUseCase;
import com.inkcore.application.inkestimation.usecase.EstimateInkFromObjectKeyUseCase.EstimateInkFromObjectKeyCommand;
import com.inkcore.domain.inkestimation.model.InkEstimateResult;
import com.inkcore.infrastructure.in.rest.envelope.ApiErrorEnvelope;
import com.inkcore.infrastructure.in.rest.envelope.ApiResponseFactory;
import com.inkcore.infrastructure.in.rest.envelope.ApiSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.ApiErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.ApiSecuredErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.InkEstimateSuccessEnvelope;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/ink-estimates")
@Tag(name = "Estimación de tinta", description = "Estimación de consumo de tinta CMYK/spot")
@SecurityRequirement(name = "bearerAuth")
public class InkEstimateController {

    private final EstimateInkFromObjectKeyUseCase estimateInkFromObjectKeyUseCase;
    private final EstimateInkFromMultipartUseCase estimateInkFromMultipartUseCase;
    private final ApiResponseFactory responseFactory;

    public InkEstimateController(
            EstimateInkFromObjectKeyUseCase estimateInkFromObjectKeyUseCase,
            EstimateInkFromMultipartUseCase estimateInkFromMultipartUseCase,
            ApiResponseFactory responseFactory
    ) {
        this.estimateInkFromObjectKeyUseCase = estimateInkFromObjectKeyUseCase;
        this.estimateInkFromMultipartUseCase = estimateInkFromMultipartUseCase;
        this.responseFactory = responseFactory;
    }

    @PostMapping(
            value = "/estimate",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "estimateInkConsumptionMultipart",
            summary = "Estimar consumo de tinta (multipart)",
            description = """
                    Formulario **Estimar tintas**: sube el archivo directamente (JPG, PNG, TIFF, WEBP, GIF, PDF).
                    No requiere presign ni objectKey. El archivo no se guarda en object storage.

                    Campos obligatorios: `file`, `sheetCount`.
                    Opcionales: widthCm, heightCm, dpi, gramsPerCm2, iccProfile, pages (PDF 1-based, máx. 2).
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Estimación completada",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = InkEstimateSuccessEnvelope.class)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<InkEstimateResponse>> estimateMultipart(
            @Parameter(description = "Arte a analizar", required = true)
            @RequestPart("file") MultipartFile file,
            @Parameter(description = "Cantidad de pliegos", required = true, example = "1000")
            @RequestParam("sheetCount") Integer sheetCount,
            @Parameter(description = "Ancho cm", example = "70")
            @RequestParam(value = "widthCm", required = false) Double widthCm,
            @Parameter(description = "Alto cm", example = "100")
            @RequestParam(value = "heightCm", required = false) Double heightCm,
            @Parameter(description = "DPI referencia", example = "300")
            @RequestParam(value = "dpi", required = false) Integer dpi,
            @Parameter(description = "Factor g/cm² uniforme", example = "0.00021")
            @RequestParam(value = "gramsPerCm2", required = false) Double gramsPerCm2,
            @Parameter(description = "Perfil ICC destino", example = "FOGRA39.icc")
            @RequestParam(value = "iccProfile", required = false) String iccProfile,
            @Parameter(description = "Páginas PDF 1-based (ej. \"1\" o \"1,2\")")
            @RequestParam(value = "pages", required = false) String pages,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        InkEstimateResult result = estimateInkFromMultipartUseCase.execute(
                new EstimateInkFromMultipartCommand(
                        file,
                        sheetCount,
                        widthCm,
                        heightCm,
                        dpi,
                        gramsPerCm2,
                        iccProfile,
                        pages
                ),
                authentication
        );
        return responseFactory.okStandard(httpRequest, InkEstimateResponse.from(result));
    }

    @PostMapping(
            value = "/estimate",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "estimateInkConsumptionFromObjectKey",
            summary = "Estimar consumo de tinta (objectKey)",
            description = """
                    JSON `{ objectKey, sheetCount, ... }` → envelope (`headers`, `timestamp`, `data`).
                    El arte ya está en object storage (presign PUT). No se acepta `file` ni multipart.

                    Método libre (sin RIP de pago ni Ghostscript AGPL ni base Pantone de pago):

                    **PDF (PDFBox Apache 2.0)**
                    - Vectores/texto DeviceCMYK, Separation y DeviceN (área × tint)
                    - Imágenes CMYK/spot nativas vía getRawRaster
                    - RGB/Gray → LittleCMS + ICC (perfil iccProfile / FOGRA39 por defecto)

                    **Raster (TwelveMonkeys BSD)**
                    - TIFF CMYK nativo; JPG/PNG/WEBP/GIF vía RGB→LittleCMS+ICC

                    **Motor de color**
                    - Exige LittleCMS (`data.colorEngine=littlecms`). Nativa empaquetada en el JAR.
                    - Si lcms2 o los perfiles ICC fallan → error HTTP (sin fallback naive).

                    **Consumo**
                    - gramos = (cobertura%/100) × área_cm² × factor_canal × pliegos
                    - Sin gramsPerCm2: factores por canal (C/M/Y/K/spot) del servidor
                      (aprox. C/M 0.00021, Y 0.00020, K 0.00022, spot 0.00025)
                    - Con gramsPerCm2: mismo factor uniforme en todos los canales
                    - PDF multi-página: cobertura/gramos = SUMA de páginas seleccionadas
                      (cada página = un lado al tamaño width×height); no se promedia
                    - Spots: se listan nombre/referencia si hay Separation/DeviceN en recursos
                      de las páginas elegidas, aunque cobertura sea 0% (arte aplanado a CMYK);
                      gramos spot solo si coverageMeasured=true
                    - Estimación comercial (no sustituye medición de plancha/RIP)
                    - PDF multi-trabajo: pages number[] 1-based, máx. 2 (p. ej. [1, 2])
                    - Obligatorios: objectKey, sheetCount. Defaults pliego: 70×100 cm, dpi 300.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Estimación calculada (envelope JSON con processInks, spotInks y totales).",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = InkEstimateSuccessEnvelope.class),
                    examples = {
                            @ExampleObject(
                                    name = "EstimacionOk",
                                    summary = "Pliego 70×100 cm, 1000 ejemplares, spots medidos",
                                    value = """
                                            {
                                              "headers": {
                                                "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                                "statusCode": 200,
                                                "code": "OK",
                                                "description": "Success"
                                              },
                                              "timestamp": "2026-08-04T12:00:00Z",
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
                                                "declaredSpotColorNames": ["PANTONE 2925 C", "PANTONE 2915 C"],
                                                "colorEngine": "littlecms"
                                              }
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "SpotDeclaradoSinPintura",
                                    summary = "Spot en inventario Separation (cobertura 0%, gramos 0)",
                                    value = """
                                            {
                                              "headers": {
                                                "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                                "statusCode": 200,
                                                "code": "OK",
                                                "description": "Success"
                                              },
                                              "timestamp": "2026-08-04T12:00:00Z",
                                              "data": {
                                                "fileName": "arte-aplanado.pdf",
                                                "contentType": "application/pdf",
                                                "widthCm": 10.0,
                                                "heightCm": 5.0,
                                                "areaCm2": 50.0,
                                                "sheetCount": 1000,
                                                "dpi": 300,
                                                "gramsPerCm2AtFullCoverage": 0.00021,
                                                "widthPx": 1181,
                                                "heightPx": 591,
                                                "iccProfileUsed": "FOGRA39.icc",
                                                "processInks": [
                                                  {
                                                    "name": "Cian",
                                                    "channel": "C",
                                                    "coveragePercent": 20.0,
                                                    "swatchHex": "#00A3E0",
                                                    "coverageMeasured": true,
                                                    "gramsPerSheet": 0.00021,
                                                    "gramsOrder": 0.21
                                                  }
                                                ],
                                                "spotInks": [
                                                  {
                                                    "name": "PANTONE 185 C",
                                                    "channel": "SPOT",
                                                    "coveragePercent": 0.0,
                                                    "swatchHex": "#E4002B",
                                                    "coverageMeasured": false,
                                                    "gramsPerSheet": 0.0,
                                                    "gramsOrder": 0.0
                                                  }
                                                ],
                                                "processGramsPerSheet": 0.00021,
                                                "spotGramsPerSheet": 0.0,
                                                "totalGramsPerSheet": 0.00021,
                                                "processGramsOrder": 0.21,
                                                "spotGramsOrder": 0.0,
                                                "totalGramsOrder": 0.21,
                                                "processingTimeMs": 420,
                                                "originalSizeBytes": 512000,
                                                "pagesAnalyzed": [1],
                                                "spotInventoryVerified": true,
                                                "hasSpotColors": true,
                                                "declaredSpotColorNames": ["PANTONE 185 C"],
                                                "colorEngine": "littlecms"
                                              }
                                            }
                                            """
                            )
                    }
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
            description = "JSON: objectKey + sheetCount (obligatorios); widthCm, heightCm, dpi, gramsPerCm2, iccProfile, pages (opcionales)",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = InkEstimateJsonRequest.class),
                    examples = {
                            @ExampleObject(
                                    name = "PliegoColombia",
                                    summary = "Offset Colombia 70x100; gramsPerCm2 uniforme",
                                    value = """
                                            {
                                              "objectKey": "tmp/company/company-seed-001/ink-estimates/user-1/entry-1/original.pdf",
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
                                    name = "PdfPaginasSeleccionadas",
                                    summary = "Solo paginas 1 y 3 (max. 2)",
                                    value = """
                                            {
                                              "objectKey": "tmp/company/company-seed-001/ink-estimates/user-1/entry-1/original.pdf",
                                              "widthCm": 10,
                                              "heightCm": 5,
                                              "sheetCount": 1000,
                                              "pages": [1, 3],
                                              "iccProfile": "FOGRA39.icc"
                                            }
                                            """
                            )
                    }
            )
    )
    public ResponseEntity<ApiSuccessEnvelope<InkEstimateResponse>> estimate(
            @org.springframework.web.bind.annotation.RequestBody InkEstimateJsonRequest body,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        InkEstimateJsonRequest request = body == null
                ? new InkEstimateJsonRequest(null, null, null, null, null, null, null, null)
                : body;
        InkEstimateResult result = estimateInkFromObjectKeyUseCase.execute(
                new EstimateInkFromObjectKeyCommand(
                        request.objectKey(),
                        request.sheetCount(),
                        request.widthCm(),
                        request.heightCm(),
                        request.dpi(),
                        request.gramsPerCm2(),
                        request.iccProfile(),
                        request.pages()
                ),
                authentication
        );
        return responseFactory.okStandard(httpRequest, InkEstimateResponse.from(result));
    }
}
