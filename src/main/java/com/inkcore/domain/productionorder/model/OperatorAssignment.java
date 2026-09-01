package com.inkcore.domain.productionorder.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Operador asignado a una etapa ({@code indicolors.production_order_operators}).
 * Máximo uno por (orden, etapa).
 */
public final class OperatorAssignment {

    private String operatorAssignmentId;
    private String companyId;
    private String productionOrderId;
    private ProductionOrderStage stage;
    private String userId;
    private String roleCode;

    public OperatorAssignment() {
        this.operatorAssignmentId = UUID.randomUUID().toString();
    }

    public static OperatorAssignment of(
            String companyId,
            String productionOrderId,
            ProductionOrderStage stage,
            String userId
    ) {
        return of(companyId, productionOrderId, stage, userId, null);
    }

    public static OperatorAssignment of(
            String companyId,
            String productionOrderId,
            ProductionOrderStage stage,
            String userId,
            String roleCode
    ) {
        OperatorAssignment assignment = new OperatorAssignment();
        assignment.companyId = companyId;
        assignment.productionOrderId = productionOrderId;
        assignment.stage = stage;
        assignment.userId = userId;
        assignment.roleCode = blankToNull(roleCode);
        return assignment;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public String getOperatorAssignmentId() {
        return operatorAssignmentId;
    }

    public void setOperatorAssignmentId(String operatorAssignmentId) {
        this.operatorAssignmentId = operatorAssignmentId;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getProductionOrderId() {
        return productionOrderId;
    }

    public void setProductionOrderId(String productionOrderId) {
        this.productionOrderId = productionOrderId;
    }

    public ProductionOrderStage getStage() {
        return stage;
    }

    public void setStage(ProductionOrderStage stage) {
        this.stage = stage;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = blankToNull(roleCode);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OperatorAssignment that)) {
            return false;
        }
        return Objects.equals(productionOrderId, that.productionOrderId) && stage == that.stage;
    }

    @Override
    public int hashCode() {
        return Objects.hash(productionOrderId, stage);
    }
}
