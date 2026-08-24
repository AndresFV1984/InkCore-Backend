package com.inkcore.domain.productionorder.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Raíz del agregado Orden de Producción ({@code indicolors.production_orders}).
 * <p>
 * Concentra las especificaciones y las colecciones de cada paso del wizard. Los
 * totales calculados en servidor se asignan desde
 * {@code ProductionOrderCalculator} en los casos de uso.
 */
public final class ProductionOrder {

    public static final String STATUS_PENDING = "PENDING";
    public static final int DEFAULT_ROUNDING_MARGIN = 2;

    private String productionOrderId;
    private String companyId;
    private String orderNumber;
    private Long version;

    private String clientId;
    private String workName;
    private String sellerId;
    private LocalDate orderDate;
    private int requestedQuantity;
    private Integer proposalQuantity1;
    private Integer proposalQuantity2;
    private LocalDateTime specificationsCompletedAt;

    private LocalDateTime cuttingCompletedAt;
    private LocalDateTime printingCompletedAt;
    private LocalDateTime finishedProductsCompletedAt;
    private LocalDateTime finishingProcessesCompletedAt;

    private Boolean clientSuppliesPaperDefault;
    private Integer roundingMargin;

    private String status;
    private boolean state;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    private PrepressDetails prepress;
    private BillingDetails billing;
    private List<OperatorAssignment> operators = new ArrayList<>();
    private List<StageDiscount> stageDiscounts = new ArrayList<>();
    private List<Plate> plates = new ArrayList<>();
    private List<PaperRow> paperRows = new ArrayList<>();
    private List<PrintConfig> prints = new ArrayList<>();
    private List<PostpressRecord> postpressRecords = new ArrayList<>();

    private ProductionOrder() {
    }

    public static ProductionOrder createNew(
            String companyId,
            String orderNumber,
            String clientId,
            String workName,
            String sellerId,
            LocalDate orderDate,
            Integer requestedQuantity,
            Integer proposalQuantity1,
            Integer proposalQuantity2,
            String createdBy,
            LocalDateTime now
    ) {
        requireNotBlank(companyId, "La empresa es obligatoria");
        requireNotBlank(orderNumber, "El número de orden es obligatorio");
        requireNotBlank(clientId, "El cliente es obligatorio");
        requireNotBlank(workName, "El nombre del trabajo es obligatorio");
        if (requestedQuantity == null || requestedQuantity <= 0) {
            throw new IllegalArgumentException("La cantidad solicitada debe ser mayor que 0");
        }
        requireNonNegative(proposalQuantity1, "La cantidad de la propuesta 1 no puede ser negativa");
        requireNonNegative(proposalQuantity2, "La cantidad de la propuesta 2 no puede ser negativa");

        ProductionOrder order = new ProductionOrder();
        order.productionOrderId = UUID.randomUUID().toString();
        order.companyId = companyId.trim();
        order.orderNumber = orderNumber.trim();
        order.version = null;
        order.clientId = clientId.trim();
        order.workName = workName.trim();
        order.sellerId = blankToNull(sellerId);
        order.orderDate = orderDate == null ? now.toLocalDate() : orderDate;
        order.requestedQuantity = requestedQuantity;
        order.proposalQuantity1 = proposalQuantity1;
        order.proposalQuantity2 = proposalQuantity2;
        order.specificationsCompletedAt = now;
        order.roundingMargin = DEFAULT_ROUNDING_MARGIN;
        order.status = STATUS_PENDING;
        order.state = true;
        order.createdAt = now;
        order.updatedAt = now;
        order.createdBy = blankToNull(createdBy);
        order.updatedBy = blankToNull(createdBy);
        return order;
    }

    public static ProductionOrder reconstitute() {
        return new ProductionOrder();
    }

    /**
     * Reemplaza las especificaciones sin tocar los pasos posteriores.
     */
    public void updateSpecifications(
            String clientId,
            String workName,
            String sellerId,
            LocalDate orderDate,
            Integer requestedQuantity,
            Integer proposalQuantity1,
            Integer proposalQuantity2,
            LocalDateTime now
    ) {
        requireNotBlank(clientId, "El cliente es obligatorio");
        requireNotBlank(workName, "El nombre del trabajo es obligatorio");
        if (requestedQuantity == null || requestedQuantity <= 0) {
            throw new IllegalArgumentException("La cantidad solicitada debe ser mayor que 0");
        }
        requireNonNegative(proposalQuantity1, "La cantidad de la propuesta 1 no puede ser negativa");
        requireNonNegative(proposalQuantity2, "La cantidad de la propuesta 2 no puede ser negativa");

        this.clientId = clientId.trim();
        this.workName = workName.trim();
        this.sellerId = blankToNull(sellerId);
        if (orderDate != null) {
            this.orderDate = orderDate;
        }
        this.requestedQuantity = requestedQuantity;
        this.proposalQuantity1 = proposalQuantity1;
        this.proposalQuantity2 = proposalQuantity2;
        this.specificationsCompletedAt = now;
        this.updatedAt = now;
    }

    /**
     * Deja un único operador por etapa (UNIQUE order_id, stage).
     */
    public void upsertOperator(ProductionOrderStage stage, String userId) {
        if (stage == null) {
            return;
        }
        operators.removeIf(operator -> operator.getStage() == stage);
        if (userId == null || userId.isBlank()) {
            return;
        }
        operators.add(OperatorAssignment.of(companyId, productionOrderId, stage, userId.trim()));
    }

    /**
     * Deja un único descuento por etapa (UNIQUE order_id, stage).
     */
    public void upsertStageDiscount(ProductionOrderStage stage, DiscountType type, BigDecimal value) {
        if (stage == null) {
            return;
        }
        stageDiscounts.removeIf(discount -> discount.getStage() == stage);
        if (type == null && value == null) {
            return;
        }
        stageDiscounts.add(StageDiscount.of(companyId, productionOrderId, stage, type, value));
    }

    public Optional<Plate> findPlate(String plateId) {
        if (plateId == null) {
            return Optional.empty();
        }
        return plates.stream().filter(plate -> plateId.equals(plate.getPlateId())).findFirst();
    }

    public boolean hasOperators() {
        return !operators.isEmpty();
    }

    public boolean isBilled() {
        return billing != null && billing.getBillingCompletedAt() != null;
    }

    public int effectiveRoundingMargin() {
        return roundingMargin == null ? DEFAULT_ROUNDING_MARGIN : roundingMargin;
    }

    private static void requireNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void requireNonNegative(Integer value, String message) {
        if (value != null && value < 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public String getProductionOrderId() {
        return productionOrderId;
    }

    public void setProductionOrderId(String productionOrderId) {
        this.productionOrderId = productionOrderId;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getWorkName() {
        return workName;
    }

    public void setWorkName(String workName) {
        this.workName = workName;
    }

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDate orderDate) {
        this.orderDate = orderDate;
    }

    public int getRequestedQuantity() {
        return requestedQuantity;
    }

    public void setRequestedQuantity(int requestedQuantity) {
        this.requestedQuantity = requestedQuantity;
    }

    public Integer getProposalQuantity1() {
        return proposalQuantity1;
    }

    public void setProposalQuantity1(Integer proposalQuantity1) {
        this.proposalQuantity1 = proposalQuantity1;
    }

    public Integer getProposalQuantity2() {
        return proposalQuantity2;
    }

    public void setProposalQuantity2(Integer proposalQuantity2) {
        this.proposalQuantity2 = proposalQuantity2;
    }

    public LocalDateTime getSpecificationsCompletedAt() {
        return specificationsCompletedAt;
    }

    public void setSpecificationsCompletedAt(LocalDateTime specificationsCompletedAt) {
        this.specificationsCompletedAt = specificationsCompletedAt;
    }

    public LocalDateTime getCuttingCompletedAt() {
        return cuttingCompletedAt;
    }

    public void setCuttingCompletedAt(LocalDateTime cuttingCompletedAt) {
        this.cuttingCompletedAt = cuttingCompletedAt;
    }

    public LocalDateTime getPrintingCompletedAt() {
        return printingCompletedAt;
    }

    public void setPrintingCompletedAt(LocalDateTime printingCompletedAt) {
        this.printingCompletedAt = printingCompletedAt;
    }

    public LocalDateTime getFinishedProductsCompletedAt() {
        return finishedProductsCompletedAt;
    }

    public void setFinishedProductsCompletedAt(LocalDateTime finishedProductsCompletedAt) {
        this.finishedProductsCompletedAt = finishedProductsCompletedAt;
    }

    public LocalDateTime getFinishingProcessesCompletedAt() {
        return finishingProcessesCompletedAt;
    }

    public void setFinishingProcessesCompletedAt(LocalDateTime finishingProcessesCompletedAt) {
        this.finishingProcessesCompletedAt = finishingProcessesCompletedAt;
    }

    public Boolean getClientSuppliesPaperDefault() {
        return clientSuppliesPaperDefault;
    }

    public void setClientSuppliesPaperDefault(Boolean clientSuppliesPaperDefault) {
        this.clientSuppliesPaperDefault = clientSuppliesPaperDefault;
    }

    public Integer getRoundingMargin() {
        return roundingMargin;
    }

    public void setRoundingMargin(Integer roundingMargin) {
        this.roundingMargin = roundingMargin;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isState() {
        return state;
    }

    public void setState(boolean state) {
        this.state = state;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public PrepressDetails getPrepress() {
        return prepress;
    }

    public void setPrepress(PrepressDetails prepress) {
        this.prepress = prepress;
    }

    public BillingDetails getBilling() {
        return billing;
    }

    public void setBilling(BillingDetails billing) {
        this.billing = billing;
    }

    public List<OperatorAssignment> getOperators() {
        return operators;
    }

    public void setOperators(List<OperatorAssignment> operators) {
        this.operators = operators == null ? new ArrayList<>() : new ArrayList<>(operators);
    }

    public List<StageDiscount> getStageDiscounts() {
        return stageDiscounts;
    }

    public void setStageDiscounts(List<StageDiscount> stageDiscounts) {
        this.stageDiscounts = stageDiscounts == null ? new ArrayList<>() : new ArrayList<>(stageDiscounts);
    }

    public List<Plate> getPlates() {
        return plates;
    }

    public void setPlates(List<Plate> plates) {
        this.plates = plates == null ? new ArrayList<>() : new ArrayList<>(plates);
    }

    public List<PaperRow> getPaperRows() {
        return paperRows;
    }

    public void setPaperRows(List<PaperRow> paperRows) {
        this.paperRows = paperRows == null ? new ArrayList<>() : new ArrayList<>(paperRows);
    }

    public List<PrintConfig> getPrints() {
        return prints;
    }

    public void setPrints(List<PrintConfig> prints) {
        this.prints = prints == null ? new ArrayList<>() : new ArrayList<>(prints);
    }

    public List<PostpressRecord> getPostpressRecords() {
        return postpressRecords;
    }

    public void setPostpressRecords(List<PostpressRecord> postpressRecords) {
        this.postpressRecords = postpressRecords == null ? new ArrayList<>() : new ArrayList<>(postpressRecords);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProductionOrder that)) {
            return false;
        }
        return Objects.equals(productionOrderId, that.productionOrderId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productionOrderId);
    }
}
