package com.inkcore.domain.order.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Cabecera de pedido comercial ({@code indicolors.customer_orders}), 1:1 con una OP.
 * Identidad visible: {@code odpNumber} = {@code ODP-{n}}.
 */
public final class CustomerOrder {

    private String customerOrderId;
    private String companyId;
    private String odpNumber;
    private String productionOrderId;
    private String clientId;
    private LocalDateTime createdAt;
    private String createdBy;

    public CustomerOrder() {
        this.customerOrderId = UUID.randomUUID().toString();
    }

    public static CustomerOrder createNew(
            String companyId,
            String odpNumber,
            String productionOrderId,
            String clientId,
            String createdBy,
            LocalDateTime createdAt
    ) {
        CustomerOrder order = new CustomerOrder();
        order.companyId = companyId;
        order.odpNumber = odpNumber;
        order.productionOrderId = productionOrderId;
        order.clientId = clientId;
        order.createdBy = createdBy;
        order.createdAt = createdAt;
        return order;
    }

    public String getCustomerOrderId() { return customerOrderId; }
    public void setCustomerOrderId(String customerOrderId) { this.customerOrderId = customerOrderId; }
    public String getCompanyId() { return companyId; }
    public void setCompanyId(String companyId) { this.companyId = companyId; }
    public String getOdpNumber() { return odpNumber; }
    public void setOdpNumber(String odpNumber) { this.odpNumber = odpNumber; }
    public String getProductionOrderId() { return productionOrderId; }
    public void setProductionOrderId(String productionOrderId) { this.productionOrderId = productionOrderId; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
