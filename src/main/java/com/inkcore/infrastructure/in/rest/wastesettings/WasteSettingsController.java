package com.inkcore.infrastructure.in.rest.wastesettings;

import com.inkcore.application.wastesettings.usecase.GetCompanyWasteSettingsUseCase;
import com.inkcore.application.wastesettings.usecase.UpdateCompanyWasteSettingsUseCase;
import com.inkcore.domain.wastesettings.model.CompanyWasteSettings;
import com.inkcore.infrastructure.in.rest.envelope.ApiResponseFactory;
import com.inkcore.infrastructure.in.rest.envelope.ApiSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.ApiErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.ApiSecuredErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.WasteSettingsSuccessEnvelope;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/waste-settings")
@Tag(name = "Mermas", description = "Rangos sugeridos de merma por compañía")
@SecurityRequirement(name = "bearerAuth")
public class WasteSettingsController {

    private final GetCompanyWasteSettingsUseCase getUseCase;
    private final UpdateCompanyWasteSettingsUseCase updateUseCase;
    private final ApiResponseFactory responseFactory;

    public WasteSettingsController(
            GetCompanyWasteSettingsUseCase getUseCase,
            UpdateCompanyWasteSettingsUseCase updateUseCase,
            ApiResponseFactory responseFactory
    ) {
        this.getUseCase = getUseCase;
        this.updateUseCase = updateUseCase;
        this.responseFactory = responseFactory;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "getWasteSettings",
            summary = "Consulta los rangos sugeridos de merma de la compañía.",
            description = "Si la compañía no configuró nada, responde la sugerencia inicial: "
                    + "corte 2%-5% (default 2%) e impresión, preprensa, terminados y acabados 3%-8% (default 3%). "
                    + "Cada proceso tiene su propio rango. El sugerido se aplica al cotizar si el paso no envía porcentaje. "
                    + "cutMakereadySheets y operationalMakereadySheets son pliegos fijos de arranque (inicial 0). "
                    + "En corte e impresión la cantidad planificada es pliegos fijos + base * porcentaje / 100. "
                    + "Mínimo y máximo orientan la UI."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Rangos de merma",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = WasteSettingsSuccessEnvelope.class),
                    examples = @ExampleObject(name = "Mermas", value = WasteSettingsSwaggerExamples.OK)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<WasteSettingsResponse>> get(
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        return responseFactory.success(httpRequest, HttpStatus.OK, WasteSettingsResponse.from(getUseCase.execute(authentication)));
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(
            operationId = "updateWasteSettings",
            summary = "Actualiza los rangos sugeridos de merma.",
            description = "Solo administrador. En cada proceso el sugerido debe caer entre el mínimo y el máximo. "
                    + "Corte, impresión, preprensa, terminados y acabados se guardan por separado. "
                    + "Los pliegos fijos se suman al porcentaje solo en Corte de papel e Impresión."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Rangos actualizados",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = WasteSettingsSuccessEnvelope.class),
                    examples = @ExampleObject(name = "MermasActualizadas", value = WasteSettingsSwaggerExamples.OK)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<WasteSettingsResponse>> update(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = WasteSettingsRequest.class),
                            examples = @ExampleObject(name = "ActualizarMermas", value = WasteSettingsSwaggerExamples.UPDATE_BODY)
                    )
            )
            @Valid @RequestBody WasteSettingsRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        var saved = updateUseCase.execute(
                request.cutWasteMinPercentage(),
                request.cutWasteMaxPercentage(),
                request.cutWasteDefaultPercentage(),
                request.operationalWasteMinPercentage(),
                request.operationalWasteMaxPercentage(),
                request.operationalWasteDefaultPercentage(),
                request.prepressWasteMinPercentage(),
                request.prepressWasteMaxPercentage(),
                request.prepressWasteDefaultPercentage(),
                request.finishedWasteMinPercentage(),
                request.finishedWasteMaxPercentage(),
                request.finishedWasteDefaultPercentage(),
                request.finishingWasteMinPercentage(),
                request.finishingWasteMaxPercentage(),
                request.finishingWasteDefaultPercentage(),
                request.cutMakereadySheets(),
                request.operationalMakereadySheets(),
                authentication
        );
        return responseFactory.success(httpRequest, HttpStatus.OK, WasteSettingsResponse.from(saved));
    }

    @Schema(name = "WasteSettingsRequest")
    public record WasteSettingsRequest(
            @NotNull @Schema(description = "Sugerencia inferior de merma de corte", example = "2.00", requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal cutWasteMinPercentage,
            @NotNull @Schema(description = "Sugerencia superior de merma de corte", example = "5.00", requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal cutWasteMaxPercentage,
            @NotNull @Schema(description = "Default de corte aplicado en Corte de papel si el usuario no lo cambia", example = "2.00", requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal cutWasteDefaultPercentage,
            @NotNull @Schema(description = "Sugerencia inferior de merma de impresión", example = "3.00", requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal operationalWasteMinPercentage,
            @NotNull @Schema(description = "Sugerencia superior de merma de impresión", example = "8.00", requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal operationalWasteMaxPercentage,
            @NotNull @Schema(description = "Default de impresión si el usuario no envía porcentaje", example = "3.00", requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal operationalWasteDefaultPercentage,
            @NotNull @Schema(description = "Sugerencia inferior de merma de preprensa", example = "3.00", requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal prepressWasteMinPercentage,
            @NotNull @Schema(description = "Sugerencia superior de merma de preprensa", example = "8.00", requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal prepressWasteMaxPercentage,
            @NotNull @Schema(description = "Default de preprensa si el usuario no envía porcentaje", example = "3.00", requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal prepressWasteDefaultPercentage,
            @NotNull @Schema(description = "Sugerencia inferior de merma de terminados", example = "3.00", requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal finishedWasteMinPercentage,
            @NotNull @Schema(description = "Sugerencia superior de merma de terminados", example = "8.00", requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal finishedWasteMaxPercentage,
            @NotNull @Schema(description = "Default de terminados si el usuario no envía porcentaje", example = "3.00", requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal finishedWasteDefaultPercentage,
            @NotNull @Schema(description = "Sugerencia inferior de merma de acabados", example = "3.00", requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal finishingWasteMinPercentage,
            @NotNull @Schema(description = "Sugerencia superior de merma de acabados", example = "8.00", requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal finishingWasteMaxPercentage,
            @NotNull @Schema(description = "Default de acabados si el usuario no envía porcentaje", example = "3.00", requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal finishingWasteDefaultPercentage,
            @NotNull @Schema(description = "Pliegos fijos de arranque de Corte de papel. 0 no cambia la cantidad planificada.", example = "0.00", requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal cutMakereadySheets,
            @NotNull @Schema(description = "Pliegos fijos de arranque de Impresión. 0 no cambia la cantidad planificada.", example = "0.00", requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal operationalMakereadySheets
    ) {
    }

    @Schema(name = "WasteSettingsResponse")
    public record WasteSettingsResponse(
            @Schema(example = "company-seed-001")
            String companyId,
            @Schema(description = "Sugerencia inferior de merma de corte", example = "2.00")
            BigDecimal cutWasteMinPercentage,
            @Schema(description = "Sugerencia superior de merma de corte", example = "5.00")
            BigDecimal cutWasteMaxPercentage,
            @Schema(description = "Porcentaje aplicado en Corte de papel si el usuario no lo cambia", example = "2.00")
            BigDecimal cutWasteDefaultPercentage,
            @Schema(description = "Sugerencia inferior de merma de impresión", example = "3.00")
            BigDecimal operationalWasteMinPercentage,
            @Schema(description = "Sugerencia superior de merma de impresión", example = "8.00")
            BigDecimal operationalWasteMaxPercentage,
            @Schema(description = "Default de impresión si el usuario no envía porcentaje", example = "3.00")
            BigDecimal operationalWasteDefaultPercentage,
            @Schema(description = "Sugerencia inferior de merma de preprensa", example = "3.00")
            BigDecimal prepressWasteMinPercentage,
            @Schema(description = "Sugerencia superior de merma de preprensa", example = "8.00")
            BigDecimal prepressWasteMaxPercentage,
            @Schema(description = "Default de preprensa si el usuario no envía porcentaje", example = "3.00")
            BigDecimal prepressWasteDefaultPercentage,
            @Schema(description = "Sugerencia inferior de merma de terminados", example = "3.00")
            BigDecimal finishedWasteMinPercentage,
            @Schema(description = "Sugerencia superior de merma de terminados", example = "8.00")
            BigDecimal finishedWasteMaxPercentage,
            @Schema(description = "Default de terminados si el usuario no envía porcentaje", example = "3.00")
            BigDecimal finishedWasteDefaultPercentage,
            @Schema(description = "Sugerencia inferior de merma de acabados", example = "3.00")
            BigDecimal finishingWasteMinPercentage,
            @Schema(description = "Sugerencia superior de merma de acabados", example = "8.00")
            BigDecimal finishingWasteMaxPercentage,
            @Schema(description = "Default de acabados si el usuario no envía porcentaje", example = "3.00")
            BigDecimal finishingWasteDefaultPercentage,
            @Schema(description = "Pliegos fijos de arranque de Corte de papel", example = "0.00")
            BigDecimal cutMakereadySheets,
            @Schema(description = "Pliegos fijos de arranque de Impresión", example = "0.00")
            BigDecimal operationalMakereadySheets
    ) {
        static WasteSettingsResponse from(CompanyWasteSettings settings) {
            return new WasteSettingsResponse(
                    settings.getCompanyId(),
                    settings.getCutWasteMinPercentage(),
                    settings.getCutWasteMaxPercentage(),
                    settings.getCutWasteDefaultPercentage(),
                    settings.getOperationalWasteMinPercentage(),
                    settings.getOperationalWasteMaxPercentage(),
                    settings.getOperationalWasteDefaultPercentage(),
                    settings.getPrepressWasteMinPercentage(),
                    settings.getPrepressWasteMaxPercentage(),
                    settings.getPrepressWasteDefaultPercentage(),
                    settings.getFinishedWasteMinPercentage(),
                    settings.getFinishedWasteMaxPercentage(),
                    settings.getFinishedWasteDefaultPercentage(),
                    settings.getFinishingWasteMinPercentage(),
                    settings.getFinishingWasteMaxPercentage(),
                    settings.getFinishingWasteDefaultPercentage(),
                    settings.getCutMakereadySheets(),
                    settings.getOperationalMakereadySheets()
            );
        }
    }
}
