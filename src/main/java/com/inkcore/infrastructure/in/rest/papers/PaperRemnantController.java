package com.inkcore.infrastructure.in.rest.papers;

import com.inkcore.application.paper.usecase.CreatePaperRemnantUseCase;
import com.inkcore.application.paper.usecase.DeletePaperRemnantUseCase;
import com.inkcore.application.paper.usecase.GetPaperRemnantByIdUseCase;
import com.inkcore.application.paper.usecase.ListPaperRemnantsUseCase;
import com.inkcore.application.paper.usecase.PaperCommands;
import com.inkcore.application.paper.usecase.UpdatePaperRemnantUseCase;
import com.inkcore.infrastructure.in.rest.envelope.ApiResponseFactory;
import com.inkcore.infrastructure.in.rest.envelope.ApiSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.ApiErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.ApiSecuredErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.PaperRemnantListSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.PaperRemnantSuccessEnvelope;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/papers/{paperId}/remnants")
@Tag(name = "Papeles", description = "Catálogo (acceptsRemnants + minRemnant W/H/unit), precios, despieces, stock y remanentes.")
@SecurityRequirement(name = "bearerAuth")
public class PaperRemnantController {

    private static final String REMNANT_EXAMPLE = """
            {
              "headers": {
                "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "statusCode": 201,
                "code": "CREATED",
                "description": "Paper remnant created"
              },
              "timestamp": "2026-10-06T12:00:00Z",
              "data": {
                "paperRemnantId": "paper-remnant-seed-001",
                "paperId": "paper-seed-001",
                "width": 35.00,
                "height": 50.00,
                "unit": "cm",
                "quantityInitial": 12.00,
                "quantityAvailable": 12.00,
                "unitCost": 0.00,
                "sourceProductionOrderId": null,
                "sourcePaperRowId": null,
                "entryDate": "2026-10-06",
                "note": "Sobrante de corte Bond 70x100",
                "state": true
              }
            }
            """;

    private final CreatePaperRemnantUseCase createUseCase;
    private final UpdatePaperRemnantUseCase updateUseCase;
    private final GetPaperRemnantByIdUseCase getUseCase;
    private final ListPaperRemnantsUseCase listUseCase;
    private final DeletePaperRemnantUseCase deleteUseCase;
    private final ApiResponseFactory responseFactory;

    public PaperRemnantController(
            CreatePaperRemnantUseCase createUseCase,
            UpdatePaperRemnantUseCase updateUseCase,
            GetPaperRemnantByIdUseCase getUseCase,
            ListPaperRemnantsUseCase listUseCase,
            DeletePaperRemnantUseCase deleteUseCase,
            ApiResponseFactory responseFactory
    ) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.getUseCase = getUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
        this.responseFactory = responseFactory;
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "registerPaperRemnant",
            summary = "Registra un remanente reutilizable del papel.",
            description = "Mismo material del papel origen con medidas propias del sobrante de corte. "
                    + "acceptsRemnants / minRemnantWidth/Height/Unit del papel son guía de UI; "
                    + "el backend no bloquea medidas por debajo del mínimo (casos especiales). "
                    + "Solo exige que el papel exista."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Remanente creado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = PaperRemnantSuccessEnvelope.class),
                    examples = @ExampleObject(name = "RemanenteCreado", value = REMNANT_EXAMPLE)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<PaperNestedResponses.PaperRemnantResponse>> register(
            @Parameter(description = "Identificador del papel origen", required = true, example = "paper-seed-001")
            @PathVariable String paperId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PaperRequests.CreatePaperRemnantRequest.class),
                            examples = @ExampleObject(
                                    name = "NuevoRemanente",
                                    value = """
                                            {
                                              "width": 35.00,
                                              "height": 50.00,
                                              "unit": "cm",
                                              "quantityInitial": 12.00,
                                              "quantityAvailable": 12.00,
                                              "unitCost": 0.00,
                                              "note": "Sobrante de corte Bond 70x100",
                                              "state": true
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody PaperRequests.CreatePaperRemnantRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        var created = createUseCase.execute(
                paperId,
                new PaperCommands.CreatePaperRemnantCommand(
                        request.width(),
                        request.height(),
                        request.unit(),
                        request.quantityInitial(),
                        request.quantityAvailable(),
                        request.unitCost(),
                        request.sourceProductionOrderId(),
                        request.sourcePaperRowId(),
                        request.entryDate(),
                        request.note(),
                        request.state()),
                authentication);
        return responseFactory.created(
                httpRequest, "CREATED", "Paper remnant created",
                PaperNestedResponses.PaperRemnantResponse.from(created));
    }

    @PutMapping("/update/{paperRemnantId}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(operationId = "updatePaperRemnant", summary = "Actualiza un remanente reutilizable.")
    @ApiResponse(
            responseCode = "200",
            description = "Remanente actualizado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = PaperRemnantSuccessEnvelope.class)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<PaperNestedResponses.PaperRemnantResponse>> update(
            @PathVariable String paperId,
            @Parameter(description = "Identificador del remanente", required = true, example = "paper-remnant-seed-001")
            @PathVariable String paperRemnantId,
            @Valid @RequestBody PaperRequests.UpdatePaperRemnantRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        var updated = updateUseCase.execute(
                paperId,
                paperRemnantId,
                new PaperCommands.UpdatePaperRemnantCommand(
                        request.width(),
                        request.height(),
                        request.unit(),
                        request.quantityInitial(),
                        request.quantityAvailable(),
                        request.unitCost(),
                        request.sourceProductionOrderId(),
                        request.sourcePaperRowId(),
                        request.entryDate(),
                        request.note(),
                        request.state()),
                authentication);
        return responseFactory.success(
                httpRequest, HttpStatus.OK, PaperNestedResponses.PaperRemnantResponse.from(updated));
    }

    @GetMapping("/{paperRemnantId}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(operationId = "getPaperRemnant", summary = "Consulta un remanente por ID.")
    @ApiResponse(
            responseCode = "200",
            description = "Remanente encontrado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = PaperRemnantSuccessEnvelope.class)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<PaperNestedResponses.PaperRemnantResponse>> getById(
            @PathVariable String paperId,
            @PathVariable String paperRemnantId,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        return responseFactory.okStandard(
                httpRequest,
                PaperNestedResponses.PaperRemnantResponse.from(
                        getUseCase.execute(paperId, paperRemnantId, authentication)));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "listPaperRemnants",
            summary = "Lista remanentes del papel.",
            description = "Filtro opcional state=true|false."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Listado de remanentes",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = PaperRemnantListSuccessEnvelope.class)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<List<PaperNestedResponses.PaperRemnantResponse>>> list(
            @PathVariable String paperId,
            @Parameter(description = "Filtrar por estado (true=activo)")
            @RequestParam(required = false) Boolean state,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        var remnants = listUseCase.execute(paperId, state, authentication);
        return responseFactory.okStandard(
                httpRequest, remnants.stream().map(PaperNestedResponses.PaperRemnantResponse::from).toList());
    }

    @DeleteMapping("/{paperRemnantId}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(operationId = "deletePaperRemnant", summary = "Elimina un remanente.")
    @ApiResponse(responseCode = "204", description = "Eliminado")
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<Void> delete(
            @PathVariable String paperId,
            @PathVariable String paperRemnantId,
            Authentication authentication
    ) {
        deleteUseCase.execute(paperId, paperRemnantId, authentication);
        return ResponseEntity.noContent().build();
    }
}
