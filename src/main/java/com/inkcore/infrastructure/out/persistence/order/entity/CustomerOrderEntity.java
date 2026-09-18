package com.inkcore.infrastructure.out.persistence.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;

@Entity
@Table(name = "customer_orders", schema = "indicolors")
public class CustomerOrderEntity implements Persistable<String> {

    @Id
    @Column(name = "customer_order_id", length = 64)
    private String customerOrderId;

    @Transient
    private boolean isNew = true;

    @Column(name = "company_id", nullable = false, length = 64)
    private String companyId;

    @Column(name = "odp_number", nullable = false, length = 32)
    private String odpNumber;

    @Column(name = "production_order_id", nullable = false, length = 64, unique = true)
    private String productionOrderId;

    @Column(name = "client_id", nullable = false, length = 64)
    private String clientId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", nullable = false, length = 64)
    private String createdBy;

    @Override
    public String getId() { return customerOrderId; }

    @Override
    public boolean isNew() { return isNew; }

    @PostLoad
    @PostPersist
    void markNotNew() { this.isNew = false; }

    public void markNew() { this.isNew = true; }

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
