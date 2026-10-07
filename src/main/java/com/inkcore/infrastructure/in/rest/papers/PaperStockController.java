package com.inkcore.infrastructure.in.rest.papers;

import com.inkcore.application.paper.usecase.CreatePaperStockUseCase;
import com.inkcore.application.paper.usecase.DeletePaperStockUseCase;
import com.inkcore.application.paper.usecase.GetPaperStockByIdUseCase;
import com.inkcore.application.paper.usecase.ListPaperStockUseCase;
import com.inkcore.application.paper.usecase.PaperCommands;
import com.inkcore.application.paper.usecase.UpdatePaperStockUseCase;
import com.inkcore.infrastructure.in.rest.envelope.ApiResponseFactory;
import com.inkcore.infrastructure.in.rest.envelope.ApiSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.ApiErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.ApiSecuredErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.PaperStockListSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.PaperStockSuccessEnvelope;
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
@RequestMapping("/api/v1/papers/{paperId}/stock")
@Tag(name = "Papeles", description = "Catálogo (acceptsRemnants + minRemnant W/H/unit), precios, despieces, stock y remanentes.")
@SecurityRequirement(name = "bearerAuth")
public class PaperStockController {

    private static final String STOCK_EXAMPLE = """
            {
              "headers": {
                "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "statusCode": 201,
                "code": "CREATED",
                "description": "Paper stock created"
              },
              "timestamp": "2026-10-06T12:00:00Z",
              "data": {
                "paperStockId": "paper-stock-seed-001",
                "paperId": "paper-seed-001",
                "quantityInitial": 1000.00,
                "quantityAvailable": 1000.00,
                "unitCost": 1500.00,
                "entryDate": "2026-10-06",
                "state": true
              }
            }
            """;

    private final CreatePaperStockUseCase createUseCase;
    private final UpdatePaperStockUseCase updateUseCase;
    private final GetPaperStockByIdUseCase getUseCase;
    private final ListPaperStockUseCase listUseCase;
    private final DeletePaperStockUseCase deleteUseCase;
    private final ApiResponseFactory responseFactory;

    public PaperStockController(
            CreatePaperStockUseCase createUseCase,
            UpdatePaperStockUseCase updateUseCase,
            GetPaperStockByIdUseCase getUseCase,
            ListPaperStockUseCase listUseCase,
            DeletePaperStockUseCase deleteUseCase,
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
            operationId = "registerPaperStock",
            summary = "Registra un lote de inventario del papel.",
            description = "CRUD de lotes con state (activo/inactivo). Sin consumo automático todavía."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Lote creado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = PaperStockSuccessEnvelope.class),
                    examples = @ExampleObject(name = "LoteCreado", value = STOCK_EXAMPLE)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<PaperNestedResponses.PaperStockResponse>> register(
            @Parameter(description = "Identificador del papel", required = true, example = "paper-seed-001")
            @PathVariable String paperId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PaperRequests.CreatePaperStockRequest.class),
                            examples = @ExampleObject(
                                    name = "NuevoLote",
                                    value = """
                                            {
                                              "quantityInitial": 1000.00,
                                              "quantityAvailable": 1000.00,
                                              "unitCost": 1500.00,
                                              "entryDate": "2026-10-06",
                                              "state": true
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody PaperRequests.CreatePaperStockRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        var created = createUseCase.execute(
                paperId,
                new PaperCommands.CreatePaperStockCommand(
                        request.quantityInitial(), request.quantityAvailable(),
                        request.unitCost(), request.entryDate(), request.state()),
                authentication);
        return responseFactory.created(
                httpRequest, "CREATED", "Paper stock created",
                PaperNestedResponses.PaperStockResponse.from(created));
    }

    @PutMapping("/update/{paperStockId}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(operationId = "updatePaperStock", summary = "Actualiza un lote de inventario.")
    @ApiResponse(
            responseCode = "200",
            description = "Lote actualizado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = PaperStockSuccessEnvelope.class)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<PaperNestedResponses.PaperStockResponse>> update(
            @PathVariable String paperId,
            @Parameter(description = "Identificador del lote", required = true, example = "paper-stock-seed-001")
            @PathVariable String paperStockId,
            @Valid @RequestBody PaperRequests.UpdatePaperStockRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        var updated = updateUseCase.execute(
                paperId,
                paperStockId,
                new PaperCommands.UpdatePaperStockCommand(
                        request.quantityInitial(), request.quantityAvailable(),
                        request.unitCost(), request.entryDate(), request.state()),
                authentication);
        return responseFactory.success(
                httpRequest, HttpStatus.OK, PaperNestedResponses.PaperStockResponse.from(updated));
    }

    @GetMapping("/{paperStockId}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(operationId = "getPaperStock", summary = "Consulta un lote de inventario por ID.")
    @ApiResponse(
            responseCode = "200",
            description = "Lote encontrado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = PaperStockSuccessEnvelope.class)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<PaperNestedResponses.PaperStockResponse>> getById(
            @PathVariable String paperId,
            @PathVariable String paperStockId,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        return responseFactory.okStandard(
                httpRequest,
                PaperNestedResponses.PaperStockResponse.from(
                        getUseCase.execute(paperId, paperStockId, authentication)));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "listPaperStock",
            summary = "Lista el inventario del papel.",
            description = "Filtro opcional state=true|false."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Listado de lotes",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = PaperStockListSuccessEnvelope.class)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<List<PaperNestedResponses.PaperStockResponse>>> list(
            @PathVariable String paperId,
            @Parameter(description = "Filtrar por estado (true=activo)")
            @RequestParam(required = false) Boolean state,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        var stocks = listUseCase.execute(paperId, state, authentication);
        return responseFactory.okStandard(
                httpRequest, stocks.stream().map(PaperNestedResponses.PaperStockResponse::from).toList());
    }

    @DeleteMapping("/{paperStockId}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(operationId = "deletePaperStock", summary = "Elimina un lote de inventario.")
    @ApiResponse(responseCode = "204", description = "Eliminado")
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<Void> delete(
            @PathVariable String paperId,
            @PathVariable String paperStockId,
            Authentication authentication
    ) {
        deleteUseCase.execute(paperId, paperStockId, authentication);
        return ResponseEntity.noContent().build();
    }
}
