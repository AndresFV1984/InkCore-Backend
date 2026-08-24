package com.inkcore.infrastructure.in.rest.productionorders;

import com.inkcore.application.productionorder.usecase.CreateProductionOrderCommand;
import com.inkcore.application.productionorder.usecase.CreateProductionOrderUseCase;
import com.inkcore.application.productionorder.usecase.DeleteProductionOrderUseCase;
import com.inkcore.application.productionorder.usecase.GenerateProductionOrderBillingPdfUseCase;
import com.inkcore.application.productionorder.usecase.GetProductionOrderUseCase;
import com.inkcore.application.productionorder.usecase.ListProductionOrdersUseCase;
import com.inkcore.application.productionorder.usecase.UpdateProductionOrderBillingCommand;
import com.inkcore.application.productionorder.usecase.UpdateProductionOrderBillingUseCase;
import com.inkcore.application.productionorder.usecase.UpdateProductionOrderPaperCuttingCommand;
import com.inkcore.application.productionorder.usecase.UpdateProductionOrderPaperCuttingUseCase;
import com.inkcore.application.productionorder.usecase.UpdateProductionOrderPostpressCommand;
import com.inkcore.application.productionorder.usecase.UpdateProductionOrderPostpressUseCase;
import com.inkcore.application.productionorder.usecase.UpdateProductionOrderPrepressCommand;
import com.inkcore.application.productionorder.usecase.UpdateProductionOrderPrepressUseCase;
import com.inkcore.application.productionorder.usecase.UpdateProductionOrderPrintingCommand;
import com.inkcore.application.productionorder.usecase.UpdateProductionOrderPrintingUseCase;
import com.inkcore.application.productionorder.usecase.UpdateProductionOrderSpecificationsCommand;
import com.inkcore.application.productionorder.usecase.UpdateProductionOrderSpecificationsUseCase;
import com.inkcore.application.productionorder.usecase.UpdateProductionOrderStatusCommand;
import com.inkcore.application.productionorder.usecase.UpdateProductionOrderStatusUseCase;
import com.inkcore.domain.productionorder.model.PostpressType;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.infrastructure.in.rest.envelope.ApiResponseFactory;
import com.inkcore.infrastructure.in.rest.envelope.ApiSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.ApiErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.ApiSecuredErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.ProductionOrderListSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.ProductionOrderSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.productionorders.ProductionOrderRequests.PaperRowRequest;
import com.inkcore.infrastructure.in.rest.productionorders.ProductionOrderRequests.PlateRequest;
import com.inkcore.infrastructure.in.rest.productionorders.ProductionOrderRequests.PostpressLineRequest;
import com.inkcore.infrastructure.in.rest.productionorders.ProductionOrderRequests.PostpressRecordRequest;
import com.inkcore.infrastructure.in.rest.productionorders.ProductionOrderRequests.PrintEntryRequest;
import com.inkcore.infrastructure.in.rest.productionorders.ProductionOrderRequests.PrintRequest;
import com.inkcore.infrastructure.in.rest.productionorders.ProductionOrderRequests.RegisterRequest;
import com.inkcore.infrastructure.in.rest.productionorders.ProductionOrderRequests.UpdateBillingRequest;
import com.inkcore.infrastructure.in.rest.productionorders.ProductionOrderRequests.UpdatePaperCuttingRequest;
import com.inkcore.infrastructure.in.rest.productionorders.ProductionOrderRequests.UpdatePostpressRequest;
import com.inkcore.infrastructure.in.rest.productionorders.ProductionOrderRequests.UpdatePrepressRequest;
import com.inkcore.infrastructure.in.rest.productionorders.ProductionOrderRequests.UpdatePrintingRequest;
import com.inkcore.infrastructure.in.rest.productionorders.ProductionOrderRequests.UpdateSpecificationsRequest;
import com.inkcore.infrastructure.in.rest.productionorders.ProductionOrderRequests.UpdateStatusRequest;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
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

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/production-orders")
@Tag(name = "Órdenes de producción", description = "Wizard OP: especificaciones, preprensa, corte, impresión, terminados, acabados y cobro")
@SecurityRequirement(name = "bearerAuth")
public class ProductionOrderController {

    private final CreateProductionOrderUseCase createUseCase;
    private final GetProductionOrderUseCase getUseCase;
    private final ListProductionOrdersUseCase listUseCase;
    private final UpdateProductionOrderSpecificationsUseCase updateSpecificationsUseCase;
    private final UpdateProductionOrderPrepressUseCase updatePrepressUseCase;
    private final UpdateProductionOrderPaperCuttingUseCase updatePaperCuttingUseCase;
    private final UpdateProductionOrderPrintingUseCase updatePrintingUseCase;
    private final UpdateProductionOrderPostpressUseCase updatePostpressUseCase;
    private final UpdateProductionOrderBillingUseCase updateBillingUseCase;
    private final UpdateProductionOrderStatusUseCase updateStatusUseCase;
    private final DeleteProductionOrderUseCase deleteUseCase;
    private final GenerateProductionOrderBillingPdfUseCase generatePdfUseCase;
    private final ApiResponseFactory responseFactory;

    public ProductionOrderController(
            CreateProductionOrderUseCase createUseCase,
            GetProductionOrderUseCase getUseCase,
            ListProductionOrdersUseCase listUseCase,
            UpdateProductionOrderSpecificationsUseCase updateSpecificationsUseCase,
            UpdateProductionOrderPrepressUseCase updatePrepressUseCase,
            UpdateProductionOrderPaperCuttingUseCase updatePaperCuttingUseCase,
            UpdateProductionOrderPrintingUseCase updatePrintingUseCase,
            UpdateProductionOrderPostpressUseCase updatePostpressUseCase,
            UpdateProductionOrderBillingUseCase updateBillingUseCase,
            UpdateProductionOrderStatusUseCase updateStatusUseCase,
            DeleteProductionOrderUseCase deleteUseCase,
            GenerateProductionOrderBillingPdfUseCase generatePdfUseCase,
            ApiResponseFactory responseFactory
    ) {
        this.createUseCase = createUseCase;
        this.getUseCase = getUseCase;
        this.listUseCase = listUseCase;
        this.updateSpecificationsUseCase = updateSpecificationsUseCase;
        this.updatePrepressUseCase = updatePrepressUseCase;
        this.updatePaperCuttingUseCase = updatePaperCuttingUseCase;
        this.updatePrintingUseCase = updatePrintingUseCase;
        this.updatePostpressUseCase = updatePostpressUseCase;
        this.updateBillingUseCase = updateBillingUseCase;
        this.updateStatusUseCase = updateStatusUseCase;
        this.deleteUseCase = deleteUseCase;
        this.generatePdfUseCase = generatePdfUseCase;
        this.responseFactory = responseFactory;
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "registerProductionOrder",
            summary = "Crea una OP con Especificaciones.",
            description = "Crea el núcleo de la Orden de Producción. companyId y createdBy salen del JWT. "
                    + "No enviar orderNumber: el backend genera un consecutivo corto único por empresa "
                    + "(OP-1, OP-2, OP-42, …). Requiere clientId, workName y requestedQuantity (>0)."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Orden creada",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ProductionOrderSuccessEnvelope.class),
                    examples = @ExampleObject(name = "OrdenCreada", value = ProductionOrderSwaggerExamples.SUCCESS_CREATED)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<ProductionOrderResponse>> register(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Especificaciones iniciales. Sin companyId ni orderNumber.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RegisterRequest.class),
                            examples = @ExampleObject(name = "RegistrarOP", value = ProductionOrderSwaggerExamples.REGISTER_BODY)
                    )
            )
            @Valid @RequestBody RegisterRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        ProductionOrder created = createUseCase.execute(toCreateCommand(request), authentication);
        return responseFactory.created(httpRequest, "CREATED", "Production order created",
                ProductionOrderResponse.from(created));
    }

    @GetMapping("/get/{productionOrderId}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "getProductionOrder",
            summary = "Obtiene la OP agregada completa.",
            description = "Devuelve el agregado completo para rehidratar el wizard: prepress, plates "
                    + "(productionOrderPlateId), paperRows, prints, postpressRecords "
                    + "(FINISHED_PRODUCT / FINISHING_PROCESS), billing, operators, stageDiscounts, "
                    + "timestamps de progreso y version. Solo de la empresa del usuario autenticado."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Orden encontrada",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ProductionOrderSuccessEnvelope.class),
                    examples = @ExampleObject(name = "OrdenCompleta", value = ProductionOrderSwaggerExamples.SUCCESS_OK)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<ProductionOrderResponse>> get(
            @Parameter(description = "ID de la orden", example = ProductionOrderSwaggerExamples.ORDER_ID)
            @PathVariable String productionOrderId,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        return responseFactory.success(httpRequest, HttpStatus.OK,
                ProductionOrderResponse.from(getUseCase.execute(productionOrderId, authentication)));
    }

    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "listProductionOrders",
            summary = "Lista paginada de OPs de la empresa del usuario.",
            description = "Filtros opcionales: orderNumber (búsqueda parcial, ej. OP-42 o 42), status, "
                    + "clientId, fromDate/toDate (orderDate), state. Siempre acotado al companyId del JWT."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Listado paginado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ProductionOrderListSuccessEnvelope.class),
                    examples = @ExampleObject(name = "ListadoOP", value = ProductionOrderSwaggerExamples.SUCCESS_LIST)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<PageResponse<ProductionOrderResponse>>> list(
            @Parameter(description = "Búsqueda por número de OP (parcial, sin distinguir mayúsculas)", example = "OP-42")
            @RequestParam(required = false) String orderNumber,
            @Parameter(description = "Estado en planta", example = "PENDING")
            @RequestParam(required = false) String status,
            @Parameter(description = "Filtro por cliente")
            @RequestParam(required = false) String clientId,
            @Parameter(description = "Fecha orden desde (inclusive)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @Parameter(description = "Fecha orden hasta (inclusive)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @Parameter(description = "true=activas, false=archivadas")
            @RequestParam(required = false) Boolean state,
            @Parameter(description = "Página 0-based", example = "0")
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @Parameter(description = "Tamaño (máx. 100)", example = "20")
            @RequestParam(required = false, defaultValue = "20") Integer size,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        var result = listUseCase.execute(status, clientId, orderNumber, fromDate, toDate, state,
                PageQuery.of(page, size), authentication);
        PageResponse<ProductionOrderResponse> data = PageResponse.from(result, ProductionOrderResponse::from);
        return responseFactory.okStandard(httpRequest, data);
    }

    @PutMapping("/update-specifications/{productionOrderId}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "updateProductionOrderSpecifications",
            summary = "Actualiza Especificaciones.",
            description = "Requiere version (optimistic locking). 409 si la versión no coincide."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Especificaciones actualizadas",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ProductionOrderSuccessEnvelope.class),
                    examples = @ExampleObject(name = "EspecificacionesOk", value = ProductionOrderSwaggerExamples.SUCCESS_OK)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<ProductionOrderResponse>> updateSpecifications(
            @Parameter(description = "ID de la orden", example = ProductionOrderSwaggerExamples.ORDER_ID)
            @PathVariable String productionOrderId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UpdateSpecificationsRequest.class),
                            examples = @ExampleObject(name = "UpdateSpecs", value = ProductionOrderSwaggerExamples.SPECIFICATIONS_BODY)
                    )
            )
            @Valid @RequestBody UpdateSpecificationsRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        ProductionOrder updated = updateSpecificationsUseCase.execute(
                productionOrderId,
                new UpdateProductionOrderSpecificationsCommand(
                        request.version(), request.clientId(), request.workName(), request.sellerId(),
                        request.orderDate(), request.requestedQuantity(), request.proposalQuantity1(),
                        request.proposalQuantity2(), request.operatorUserId()
                ),
                authentication
        );
        return responseFactory.success(httpRequest, HttpStatus.OK, ProductionOrderResponse.from(updated));
    }

    @PutMapping("/update-prepress/{productionOrderId}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "updateProductionOrderPrepress",
            summary = "Actualiza Preprensa y planchas.",
            description = "Upsert de prepress_details + reemplazo de plates. "
                    + "isNewDesign=false exige existingDesignOrderId y clientPlateType. "
                    + "plateReplacement=true exige replacementQuantity. Snapshots de plate_types/assembly_prices. "
                    + "Al reemplazar planchas se limpian corte/impresión/postprensa."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Preprensa actualizada",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ProductionOrderSuccessEnvelope.class)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<ProductionOrderResponse>> updatePrepress(
            @Parameter(description = "ID de la orden", example = ProductionOrderSwaggerExamples.ORDER_ID)
            @PathVariable String productionOrderId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UpdatePrepressRequest.class),
                            examples = @ExampleObject(name = "UpdatePrepress", value = ProductionOrderSwaggerExamples.PREPRESS_BODY)
                    )
            )
            @Valid @RequestBody UpdatePrepressRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        ProductionOrder updated = updatePrepressUseCase.execute(
                productionOrderId, toPrepressCommand(request), authentication);
        return responseFactory.success(httpRequest, HttpStatus.OK, ProductionOrderResponse.from(updated));
    }

    @PutMapping("/update-paper-cutting/{productionOrderId}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "updateProductionOrderPaperCutting",
            summary = "Actualiza Corte de papel.",
            description = "Reemplaza paper_rows. Totales (sheets/paper/cut) calculados en servidor. "
                    + "Filas de faltante: isMissingSupply + parentRowId."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Corte actualizado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ProductionOrderSuccessEnvelope.class)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<ProductionOrderResponse>> updatePaperCutting(
            @Parameter(description = "ID de la orden", example = ProductionOrderSwaggerExamples.ORDER_ID)
            @PathVariable String productionOrderId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UpdatePaperCuttingRequest.class),
                            examples = @ExampleObject(name = "UpdateCutting", value = ProductionOrderSwaggerExamples.PAPER_CUTTING_BODY)
                    )
            )
            @Valid @RequestBody UpdatePaperCuttingRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        ProductionOrder updated = updatePaperCuttingUseCase.execute(
                productionOrderId, toPaperCuttingCommand(request), authentication);
        return responseFactory.success(httpRequest, HttpStatus.OK, ProductionOrderResponse.from(updated));
    }

    @PutMapping("/update-printing/{productionOrderId}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "updateProductionOrderPrinting",
            summary = "Actualiza Impresión.",
            description = "1 print por plancha + entries 0..N. inkEstimation.entries[] solo metadatos + "
                    + "objectKey/previewObjectKey (sin Base64); keys tmp/ se promueven al guardar. "
                    + "Tiro+retiro debe igualar colors de la plancha."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Impresión actualizada",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ProductionOrderSuccessEnvelope.class)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<ProductionOrderResponse>> updatePrinting(
            @Parameter(description = "ID de la orden", example = ProductionOrderSwaggerExamples.ORDER_ID)
            @PathVariable String productionOrderId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UpdatePrintingRequest.class),
                            examples = @ExampleObject(name = "UpdatePrinting", value = ProductionOrderSwaggerExamples.PRINTING_BODY)
                    )
            )
            @Valid @RequestBody UpdatePrintingRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        ProductionOrder updated = updatePrintingUseCase.execute(
                productionOrderId, toPrintingCommand(request), authentication);
        return responseFactory.success(httpRequest, HttpStatus.OK, ProductionOrderResponse.from(updated));
    }

    @PutMapping("/update-finished-products/{productionOrderId}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "updateProductionOrderFinishedProducts",
            summary = "Actualiza Terminados.",
            description = "type=FINISHED_PRODUCT. catalogItemId debe existir en finished_products. "
                    + "calculatedPrice/chargedPrice/appliedMinCost se calculan en servidor. Vinculación por plateId."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Terminados actualizados",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ProductionOrderSuccessEnvelope.class)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<ProductionOrderResponse>> updateFinishedProducts(
            @Parameter(description = "ID de la orden", example = ProductionOrderSwaggerExamples.ORDER_ID)
            @PathVariable String productionOrderId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UpdatePostpressRequest.class),
                            examples = @ExampleObject(name = "UpdateFinishedProducts", value = ProductionOrderSwaggerExamples.POSTPRESS_BODY)
                    )
            )
            @Valid @RequestBody UpdatePostpressRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        ProductionOrder updated = updatePostpressUseCase.execute(
                productionOrderId, PostpressType.FINISHED_PRODUCT, toPostpressCommand(request), authentication);
        return responseFactory.success(httpRequest, HttpStatus.OK, ProductionOrderResponse.from(updated));
    }

    @PutMapping("/update-finishing-processes/{productionOrderId}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "updateProductionOrderFinishingProcesses",
            summary = "Actualiza Acabados.",
            description = "type=FINISHING_PROCESS. catalogItemId debe existir en finishing_processes. "
                    + "positive/cliche no aplican. Vinculación por plateId."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Acabados actualizados",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ProductionOrderSuccessEnvelope.class)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<ProductionOrderResponse>> updateFinishingProcesses(
            @Parameter(description = "ID de la orden", example = ProductionOrderSwaggerExamples.ORDER_ID)
            @PathVariable String productionOrderId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UpdatePostpressRequest.class),
                            examples = @ExampleObject(name = "UpdateFinishingProcesses", value = ProductionOrderSwaggerExamples.POSTPRESS_BODY)
                    )
            )
            @Valid @RequestBody UpdatePostpressRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        ProductionOrder updated = updatePostpressUseCase.execute(
                productionOrderId, PostpressType.FINISHING_PROCESS, toPostpressCommand(request), authentication);
        return responseFactory.success(httpRequest, HttpStatus.OK, ProductionOrderResponse.from(updated));
    }

    @PutMapping("/update-billing/{productionOrderId}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "updateProductionOrderBilling",
            summary = "Actualiza Cobro.",
            description = "Upsert de billing_details 1:1. clientCostingMode=exact|volume. "
                    + "Totales de cobro se calculan en servidor (no se persisten)."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Cobro actualizado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ProductionOrderSuccessEnvelope.class)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<ProductionOrderResponse>> updateBilling(
            @Parameter(description = "ID de la orden", example = ProductionOrderSwaggerExamples.ORDER_ID)
            @PathVariable String productionOrderId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UpdateBillingRequest.class),
                            examples = @ExampleObject(name = "UpdateBilling", value = ProductionOrderSwaggerExamples.BILLING_BODY)
                    )
            )
            @Valid @RequestBody UpdateBillingRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        ProductionOrder updated = updateBillingUseCase.execute(
                productionOrderId,
                new UpdateProductionOrderBillingCommand(
                        request.version(), request.billingDiscountType(), request.billingDiscountValue(),
                        request.clientCostingMode(), request.clientDiscountType(), request.clientDiscountValue(),
                        request.clientProfitabilityType(), request.clientProfitabilityValue(),
                        request.clientVolumeCosting(), request.deliveryStartDate(), request.deliveryEndDate(),
                        request.advancePercentage(), request.clientSignatureName(), request.bankAccountId(),
                        request.completed(), request.operatorUserId()
                ),
                authentication
        );
        return responseFactory.success(httpRequest, HttpStatus.OK, ProductionOrderResponse.from(updated));
    }

    @PutMapping("/update-status/{productionOrderId}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "updateProductionOrderStatus",
            summary = "Cambia status de planta y/o baja lógica (state=false).",
            description = "Baja lógica sin restricción. Alternativa al DELETE físico cuando la OP no es elegible."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Estado actualizado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ProductionOrderSuccessEnvelope.class)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<ProductionOrderResponse>> updateStatus(
            @Parameter(description = "ID de la orden", example = ProductionOrderSwaggerExamples.ORDER_ID)
            @PathVariable String productionOrderId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UpdateStatusRequest.class),
                            examples = @ExampleObject(name = "UpdateStatus", value = ProductionOrderSwaggerExamples.STATUS_BODY)
                    )
            )
            @Valid @RequestBody UpdateStatusRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        ProductionOrder updated = updateStatusUseCase.execute(
                productionOrderId,
                new UpdateProductionOrderStatusCommand(request.version(), request.status(), request.state()),
                authentication
        );
        return responseFactory.success(httpRequest, HttpStatus.OK, ProductionOrderResponse.from(updated));
    }

    @DeleteMapping("/{productionOrderId}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "deleteProductionOrder",
            summary = "Baja física condicionada (sin operadores y sin cobro).",
            description = "Solo si no hay filas en production_order_operators y billing_completed_at es NULL. "
                    + "Si no es elegible → 422. Cascade ON DELETE CASCADE en BD. "
                    + "Si no aplica, usar update-status con state=false."
    )
    @ApiResponse(responseCode = "204", description = "Eliminada físicamente")
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID de la orden", example = ProductionOrderSwaggerExamples.ORDER_ID)
            @PathVariable String productionOrderId,
            Authentication authentication
    ) {
        deleteUseCase.execute(productionOrderId, authentication);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/generate-billing-pdf/{productionOrderId}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "generateProductionOrderBillingPdf",
            summary = "Genera el PDF de cobro.",
            description = "Responde application/pdf (attachment). Totales calculados en servidor."
    )
    @ApiResponse(
            responseCode = "200",
            description = "PDF de cobro",
            content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE)
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<byte[]> generateBillingPdf(
            @Parameter(description = "ID de la orden", example = ProductionOrderSwaggerExamples.ORDER_ID)
            @PathVariable String productionOrderId,
            Authentication authentication
    ) {
        byte[] pdf = generatePdfUseCase.execute(productionOrderId, authentication);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"production-order-" + productionOrderId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    private static CreateProductionOrderCommand toCreateCommand(RegisterRequest request) {
        return new CreateProductionOrderCommand(
                request.clientId(), request.workName(), request.sellerId(), request.orderDate(),
                request.requestedQuantity(), request.proposalQuantity1(), request.proposalQuantity2(),
                request.operatorUserId()
        );
    }

    private static UpdateProductionOrderPrepressCommand toPrepressCommand(UpdatePrepressRequest request) {
        List<UpdateProductionOrderPrepressCommand.PlateInput> plates = request.plates() == null
                ? List.of()
                : request.plates().stream().map(ProductionOrderController::toPlateInput).toList();
        return new UpdateProductionOrderPrepressCommand(
                request.version(), request.isNewDesign(), request.designName(), request.existingDesignOrderId(),
                request.hasDesignCost(), request.designCost(), request.clientSuppliesPlates(),
                request.clientPlateType(), request.newPlateCost(), request.assemblyPriceId(),
                request.dieCutLine(), request.uvReserve(), request.stamping(), request.embossing(),
                request.prepressDiscountType(), request.prepressDiscountValue(), request.completed(),
                request.operatorUserId(), plates
        );
    }

    private static UpdateProductionOrderPrepressCommand.PlateInput toPlateInput(PlateRequest p) {
        return new UpdateProductionOrderPrepressCommand.PlateInput(
                p.plateId(), p.colors(), p.plateTypeId(), p.quantity(), p.cavities(), p.surplus(),
                p.platesCount(), p.detail(), p.observation(), p.manualEntry(), p.plateSupply(),
                p.plateReplacement(), p.replacementQuantity(), p.plateName(), p.plateSize(), p.platePrice()
        );
    }

    private static UpdateProductionOrderPaperCuttingCommand toPaperCuttingCommand(UpdatePaperCuttingRequest request) {
        List<UpdateProductionOrderPaperCuttingCommand.PaperRowInput> rows = request.paperRows() == null
                ? List.of()
                : request.paperRows().stream().map(ProductionOrderController::toPaperRowInput).toList();
        return new UpdateProductionOrderPaperCuttingCommand(
                request.version(), request.clientSuppliesPaperDefault(), request.roundingMargin(),
                request.completed(), request.operatorUserId(), request.discountType(), request.discountValue(), rows
        );
    }

    private static UpdateProductionOrderPaperCuttingCommand.PaperRowInput toPaperRowInput(PaperRowRequest r) {
        return new UpdateProductionOrderPaperCuttingCommand.PaperRowInput(
                r.paperRowId(), r.plateId(), r.parentRowId(), r.cutRowKey(), r.isMissingSupply(),
                r.missingSheetsQuantity(), r.clientSuppliesPaper(), r.paperTypeId(), r.cutLayoutId(),
                r.isPaperCut(), r.deliveredSheetsByClient(), r.manualGoodSizes(), r.manualSurplus()
        );
    }

    private static UpdateProductionOrderPrintingCommand toPrintingCommand(UpdatePrintingRequest request) {
        List<UpdateProductionOrderPrintingCommand.PrintInput> prints = request.prints() == null
                ? List.of()
                : request.prints().stream().map(ProductionOrderController::toPrintInput).toList();
        return new UpdateProductionOrderPrintingCommand(
                request.version(), request.completed(), request.operatorUserId(), prints
        );
    }

    private static UpdateProductionOrderPrintingCommand.PrintInput toPrintInput(PrintRequest p) {
        List<UpdateProductionOrderPrintingCommand.PrintEntryInput> entries = p.entries() == null
                ? List.of()
                : p.entries().stream().map(ProductionOrderController::toPrintEntryInput).toList();
        return new UpdateProductionOrderPrintingCommand.PrintInput(
                p.printId(), p.plateId(), p.clientSuppliesSherpa(), p.sherpaTestPrice(),
                p.machineOutputValue(), p.inkEstimation(), p.printingDiscountType(),
                p.printingDiscountValue(), p.completed(), entries
        );
    }

    private static UpdateProductionOrderPrintingCommand.PrintEntryInput toPrintEntryInput(PrintEntryRequest e) {
        return new UpdateProductionOrderPrintingCommand.PrintEntryInput(
                e.printEntryId(), e.shotsInkCount(), e.shotsInks(), e.reverseInkCount(), e.reverseInks(),
                e.basicFlipType(), e.basicThousandRateId(), e.pantoneFlipType(),
                e.clientSuppliesPantoneInk(), e.pantoneInkChargePrice(), e.pantoneThousandRateId()
        );
    }

    private static UpdateProductionOrderPostpressCommand toPostpressCommand(UpdatePostpressRequest request) {
        List<UpdateProductionOrderPostpressCommand.PostpressRecordInput> records = request.records() == null
                ? List.of()
                : request.records().stream().map(ProductionOrderController::toPostpressRecordInput).toList();
        return new UpdateProductionOrderPostpressCommand(
                request.version(), request.completed(), request.operatorUserId(),
                request.discountType(), request.discountValue(), records
        );
    }

    private static UpdateProductionOrderPostpressCommand.PostpressRecordInput toPostpressRecordInput(
            PostpressRecordRequest r
    ) {
        List<UpdateProductionOrderPostpressCommand.PostpressLineInput> lines = r.lines() == null
                ? List.of()
                : r.lines().stream().map(ProductionOrderController::toPostpressLineInput).toList();
        return new UpdateProductionOrderPostpressCommand.PostpressRecordInput(
                r.recordId(), r.plateId(), r.completed(), lines
        );
    }

    private static UpdateProductionOrderPostpressCommand.PostpressLineInput toPostpressLineInput(
            PostpressLineRequest l
    ) {
        return new UpdateProductionOrderPostpressCommand.PostpressLineInput(
                l.lineId(), l.catalogItemId(), l.source(), l.areaFactor(), l.goodSizes(),
                l.positive(), l.cliche()
        );
    }
}
