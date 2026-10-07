package com.inkcore.infrastructure.in.rest.papers;

import com.inkcore.application.paper.usecase.CreatePaperCutLayoutUseCase;
import com.inkcore.application.paper.usecase.DeletePaperCutLayoutUseCase;
import com.inkcore.application.paper.usecase.GetPaperCutLayoutByIdUseCase;
import com.inkcore.application.paper.usecase.ListPaperCutLayoutsUseCase;
import com.inkcore.application.paper.usecase.PaperCommands;
import com.inkcore.application.paper.usecase.UpdatePaperCutLayoutUseCase;
import com.inkcore.infrastructure.in.rest.envelope.ApiResponseFactory;
import com.inkcore.infrastructure.in.rest.envelope.ApiSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.ApiErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.ApiSecuredErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.PaperCutLayoutListSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.PaperCutLayoutSuccessEnvelope;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/papers/{paperId}/cut-layouts")
@Tag(name = "Papeles", description = "Catálogo (acceptsRemnants + minRemnant W/H/unit), precios, despieces, stock y remanentes.")
@SecurityRequirement(name = "bearerAuth")
public class PaperCutLayoutController {

    private static final String CUT_LAYOUT_EXAMPLE = """
            {
              "headers": {
                "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "statusCode": 201,
                "code": "CREATED",
                "description": "Paper cut layout created"
              },
              "timestamp": "2026-10-06T12:00:00Z",
              "data": {
                "paperCutLayoutId": "paper-cut-seed-001",
                "paperId": "paper-seed-001",
                "cutLayoutId": "cut-layout-seed-001",
                "orientation": "vertical",
                "wastePercentage": 2.00,
                "note": null,
                "state": true
              }
            }
            """;

    private final CreatePaperCutLayoutUseCase createUseCase;
    private final UpdatePaperCutLayoutUseCase updateUseCase;
    private final GetPaperCutLayoutByIdUseCase getUseCase;
    private final ListPaperCutLayoutsUseCase listUseCase;
    private final DeletePaperCutLayoutUseCase deleteUseCase;
    private final ApiResponseFactory responseFactory;

    public PaperCutLayoutController(
            CreatePaperCutLayoutUseCase createUseCase,
            UpdatePaperCutLayoutUseCase updateUseCase,
            GetPaperCutLayoutByIdUseCase getUseCase,
            ListPaperCutLayoutsUseCase listUseCase,
            DeletePaperCutLayoutUseCase deleteUseCase,
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
            operationId = "registerPaperCutLayout",
            summary = "Asocia un despiece al papel.",
            description = "wastePercentage es SOLO sugerencia de UI para prellenar formularios. "
                    + "El cálculo de merma en OP usa company_waste_settings + plannedWastePercentage del paso corte."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Despiece por papel creado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = PaperCutLayoutSuccessEnvelope.class),
                    examples = @ExampleObject(name = "DespieceCreado", value = CUT_LAYOUT_EXAMPLE)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<PaperNestedResponses.PaperCutLayoutResponse>> register(
            @Parameter(description = "Identificador del papel", required = true, example = "paper-seed-001")
            @PathVariable String paperId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PaperRequests.CreatePaperCutLayoutRequest.class),
                            examples = @ExampleObject(
                                    name = "AsociarDespiece",
                                    value = """
                                            {
                                              "cutLayoutId": "cut-layout-seed-001",
                                              "orientation": "vertical",
                                              "wastePercentage": 2.00,
                                              "note": null,
                                              "state": true
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody PaperRequests.CreatePaperCutLayoutRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        var created = createUseCase.execute(
                paperId,
                new PaperCommands.CreatePaperCutLayoutCommand(
                        request.cutLayoutId(), request.orientation(),
                        request.wastePercentage(), request.note(), request.state()),
                authentication);
        return responseFactory.created(
                httpRequest, "CREATED", "Paper cut layout created",
                PaperNestedResponses.PaperCutLayoutResponse.from(created));
    }

    @PutMapping("/update/{paperCutLayoutId}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "updatePaperCutLayout",
            summary = "Actualiza el despiece asociado al papel.",
            description = "No cambia cutLayoutId. Para cambiar despiece: DELETE + POST register."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Despiece actualizado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = PaperCutLayoutSuccessEnvelope.class)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<PaperNestedResponses.PaperCutLayoutResponse>> update(
            @Parameter(description = "Identificador del papel", required = true, example = "paper-seed-001")
            @PathVariable String paperId,
            @Parameter(description = "Identificador de la relación papel↔despiece", required = true,
                    example = "paper-cut-seed-001")
            @PathVariable String paperCutLayoutId,
            @Valid @RequestBody PaperRequests.UpdatePaperCutLayoutRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        var updated = updateUseCase.execute(
                paperId,
                paperCutLayoutId,
                new PaperCommands.UpdatePaperCutLayoutCommand(
                        request.orientation(), request.wastePercentage(), request.note(), request.state()),
                authentication);
        return responseFactory.success(
                httpRequest, HttpStatus.OK, PaperNestedResponses.PaperCutLayoutResponse.from(updated));
    }

    @GetMapping("/{paperCutLayoutId}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(operationId = "getPaperCutLayout", summary = "Consulta un despiece del papel por ID.")
    @ApiResponse(
            responseCode = "200",
            description = "Despiece encontrado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = PaperCutLayoutSuccessEnvelope.class)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<PaperNestedResponses.PaperCutLayoutResponse>> getById(
            @PathVariable String paperId,
            @PathVariable String paperCutLayoutId,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        return responseFactory.okStandard(
                httpRequest,
                PaperNestedResponses.PaperCutLayoutResponse.from(
                        getUseCase.execute(paperId, paperCutLayoutId, authentication)));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "listPaperCutLayouts",
            summary = "Lista los despieces asociados al papel.",
            description = "Usar en OP corte para elegir paperCutLayoutId (resuelve cutLayoutId en backend)."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Listado de despieces",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = PaperCutLayoutListSuccessEnvelope.class)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<List<PaperNestedResponses.PaperCutLayoutResponse>>> list(
            @PathVariable String paperId,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        var layouts = listUseCase.execute(paperId, authentication);
        return responseFactory.okStandard(
                httpRequest, layouts.stream().map(PaperNestedResponses.PaperCutLayoutResponse::from).toList());
    }

    @DeleteMapping("/{paperCutLayoutId}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(operationId = "deletePaperCutLayout", summary = "Elimina la asociación papel↔despiece.")
    @ApiResponse(responseCode = "204", description = "Eliminado")
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<Void> delete(
            @Parameter(description = "Identificador del papel", required = true, example = "paper-seed-001")
            @PathVariable String paperId,
            @Parameter(description = "Identificador de la relación", required = true, example = "paper-cut-seed-001")
            @PathVariable String paperCutLayoutId,
            Authentication authentication
    ) {
        deleteUseCase.execute(paperId, paperCutLayoutId, authentication);
        return ResponseEntity.noContent().build();
    }
}
