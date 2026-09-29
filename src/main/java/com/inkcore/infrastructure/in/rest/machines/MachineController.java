package com.inkcore.infrastructure.in.rest.machines;

import com.inkcore.application.machine.usecase.CreateMachineCommand;
import com.inkcore.application.machine.usecase.CreateMachineUseCase;
import com.inkcore.application.machine.usecase.GetMachineByIdUseCase;
import com.inkcore.application.machine.usecase.ListMachinesUseCase;
import com.inkcore.application.machine.usecase.RecalculateMachineRatesUseCase;
import com.inkcore.application.machine.usecase.UpdateMachineCommand;
import com.inkcore.application.machine.usecase.UpdateMachineUseCase;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.infrastructure.in.rest.envelope.ApiErrorEnvelope;
import com.inkcore.infrastructure.in.rest.envelope.ApiResponseFactory;
import com.inkcore.infrastructure.in.rest.envelope.ApiSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.ApiErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.ApiSecuredErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.MachineListSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.MachineRateRecalculationSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.MachineSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.shared.PageResponse;
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
@RequestMapping("/api/v1/machines")
@Tag(name = "Máquinas", description = "Catálogo de máquinas y costo por hora")
@SecurityRequirement(name = "bearerAuth")
public class MachineController {

    private final CreateMachineUseCase createMachineUseCase;
    private final UpdateMachineUseCase updateMachineUseCase;
    private final ListMachinesUseCase listMachinesUseCase;
    private final GetMachineByIdUseCase getMachineByIdUseCase;
    private final RecalculateMachineRatesUseCase recalculateMachineRatesUseCase;
    private final ApiResponseFactory responseFactory;

    public MachineController(
            CreateMachineUseCase createMachineUseCase,
            UpdateMachineUseCase updateMachineUseCase,
            ListMachinesUseCase listMachinesUseCase,
            GetMachineByIdUseCase getMachineByIdUseCase,
            RecalculateMachineRatesUseCase recalculateMachineRatesUseCase,
            ApiResponseFactory responseFactory
    ) {
        this.createMachineUseCase = createMachineUseCase;
        this.updateMachineUseCase = updateMachineUseCase;
        this.listMachinesUseCase = listMachinesUseCase;
        this.getMachineByIdUseCase = getMachineByIdUseCase;
        this.recalculateMachineRatesUseCase = recalculateMachineRatesUseCase;
        this.responseFactory = responseFactory;
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "registerMachine",
            summary = "Crea una máquina.",
            description = "companyId sale del JWT. costPerHour lo calcula la base y vuelve en la respuesta. "
                    + "Sin borrado físico: desactivar con state=false en la actualización."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Máquina creada",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = MachineSuccessEnvelope.class),
                    examples = @ExampleObject(name = "MaquinaCreada", value = MachineSwaggerExamples.CREATED)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<MachineResponse>> register(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Componentes del costo por hora. costPerHour no se envía.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MachineRequests.CreateMachineRequest.class),
                            examples = @ExampleObject(name = "NuevaMaquina", value = MachineSwaggerExamples.REGISTER_BODY)
                    )
            )
            @Valid @RequestBody MachineRequests.CreateMachineRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        var created = createMachineUseCase.execute(toCreate(request), authentication);
        return responseFactory.created(httpRequest, "CREATED", "Machine created", MachineResponse.from(created));
    }

    @PutMapping("/update/{machineId}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "updateMachine",
            summary = "Actualiza una máquina.",
            description = "Incluye desactivar con state=false. Si cambia un componente del costo, "
                    + "el trigger deja auditoría en machine_cost_history."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Máquina actualizada",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = MachineSuccessEnvelope.class),
                    examples = @ExampleObject(name = "MaquinaActualizada", value = MachineSwaggerExamples.OK)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<MachineResponse>> update(
            @Parameter(description = "Identificador de la máquina", required = true, example = MachineSwaggerExamples.MACHINE_ID)
            @PathVariable String machineId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MachineRequests.UpdateMachineRequest.class),
                            examples = @ExampleObject(name = "ActualizarMaquina", value = MachineSwaggerExamples.UPDATE_BODY)
                    )
            )
            @Valid @RequestBody MachineRequests.UpdateMachineRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        var updated = updateMachineUseCase.execute(toUpdate(machineId, request), authentication);
        return responseFactory.success(httpRequest, HttpStatus.OK, MachineResponse.from(updated));
    }

    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "listMachines",
            summary = "Lista máquinas de la compañía del JWT.",
            description = "Filtros opcionales: state (activas o inactivas) y machineType (fase)."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Listado de máquinas",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = MachineListSuccessEnvelope.class),
                    examples = @ExampleObject(name = "ListadoMaquinas", value = MachineSwaggerExamples.LIST)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<PageResponse<MachineResponse>>> list(
            @Parameter(description = "true=activas, false=inactivas, omitir=todas")
            @RequestParam(required = false) Boolean state,
            @Parameter(
                    description = "Fase de la máquina",
                    schema = @Schema(allowableValues = {"preprensa", "corte-papel", "impresion", "terminados", "acabados"})
            )
            @RequestParam(required = false) String machineType,
            @Parameter(description = "Página 0-based", example = "0")
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @Parameter(description = "Tamaño de página (máx. 100)", example = "20")
            @RequestParam(required = false, defaultValue = "20") Integer size,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        PageResponse<MachineResponse> data = PageResponse.from(
                listMachinesUseCase.execute(state, machineType, PageQuery.of(page, size), authentication),
                MachineResponse::from
        );
        return responseFactory.okStandard(httpRequest, data);
    }

    @GetMapping("/get/{machineId}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "getMachine",
            summary = "Consulta el detalle de una máquina.",
            description = "Incluye costPerHour calculado por la base."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Máquina encontrada",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = MachineSuccessEnvelope.class),
                    examples = @ExampleObject(name = "Maquina", value = MachineSwaggerExamples.OK)
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "Máquina no encontrada",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorEnvelope.class))
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<MachineResponse>> get(
            @Parameter(description = "Identificador de la máquina", required = true, example = MachineSwaggerExamples.MACHINE_ID)
            @PathVariable String machineId,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        return responseFactory.success(
                httpRequest,
                HttpStatus.OK,
                MachineResponse.from(getMachineByIdUseCase.execute(machineId, authentication))
        );
    }

    @PostMapping("/recalculate-rates")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "recalculateMachineRates",
            summary = "Recalcula y audita las tarifas de las máquinas activas.",
            description = "Deja un snapshot en machine_cost_history para la compañía del JWT, "
                    + "aunque costPerHour no haya cambiado. El mismo proceso corre el 1 de enero, abril, julio y octubre."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Tarifas auditadas",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = MachineRateRecalculationSuccessEnvelope.class),
                    examples = @ExampleObject(name = "TarifasRecalculadas", value = MachineSwaggerExamples.RECALCULATED)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<RecalculateRatesResponse>> recalculate(
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        var result = recalculateMachineRatesUseCase.execute(authentication);
        List<MachineResponse> machines = result.machines().stream().map(MachineResponse::from).toList();
        return responseFactory.success(
                httpRequest,
                HttpStatus.OK,
                new RecalculateRatesResponse(result.machinesSnapshotted(), machines)
        );
    }

    private static CreateMachineCommand toCreate(MachineRequests.CreateMachineRequest request) {
        return new CreateMachineCommand(
                request.name(), request.machineType(), request.manufacturer(), request.model(),
                request.purchaseCost(), request.usefulLifeYears(), request.annualMaintenanceCost(),
                request.monthlyOperatorCost(), request.energyCostPerHour(), request.productiveHoursPerYear(),
                request.state()
        );
    }

    private static UpdateMachineCommand toUpdate(String machineId, MachineRequests.UpdateMachineRequest request) {
        return new UpdateMachineCommand(
                machineId, request.name(), request.machineType(), request.manufacturer(), request.model(),
                request.purchaseCost(), request.usefulLifeYears(), request.annualMaintenanceCost(),
                request.monthlyOperatorCost(), request.energyCostPerHour(), request.productiveHoursPerYear(),
                Boolean.TRUE.equals(request.state())
        );
    }

    @Schema(name = "MachineRateRecalculationResponse")
    public record RecalculateRatesResponse(
            @Schema(description = "Máquinas activas a las que se les dejó snapshot en machine_cost_history", example = "1")
            int machinesSnapshotted,
            List<MachineResponse> machines
    ) {
    }
}
