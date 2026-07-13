package com.indicore.domain.order.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad de dominio: orden de producción / pedido.
 */
public final class Order {

    private final UUID id;
    private final UUID clientId;
    private final String workName;
    private final Instant date;
    private final OrderStatus status;
    private final BigDecimal totalAmount;

    private Order(
            UUID id,
            UUID clientId,
            String workName,
            Instant date,
            OrderStatus status,
            BigDecimal totalAmount
    ) {
        this.id = id;
        this.clientId = clientId;
        this.workName = workName;
        this.date = date;
        this.status = status;
        this.totalAmount = totalAmount;
    }

    public static Order createNew(UUID clientId, String workName, BigDecimal totalAmount) {
        if (clientId == null) {
            throw new IllegalArgumentException("El cliente es obligatorio");
        }
        if (workName == null || workName.isBlank()) {
            throw new IllegalArgumentException("El nombre del trabajo es obligatorio");
        }
        return new Order(
                UUID.randomUUID(),
                clientId,
                workName.trim(),
                Instant.now(),
                OrderStatus.EN_CURSO,
                totalAmount != null ? totalAmount : BigDecimal.ZERO
        );
    }

    public static Order reconstitute(
            UUID id,
            UUID clientId,
            String workName,
            Instant date,
            OrderStatus status,
            BigDecimal totalAmount
    ) {
        return new Order(id, clientId, workName, date, status, totalAmount);
    }

    public Order withStatus(OrderStatus newStatus) {
        return new Order(id, clientId, workName, date, newStatus, totalAmount);
    }

    public UUID getId() {
        return id;
    }

    public UUID getClientId() {
        return clientId;
    }

    public String getWorkName() {
        return workName;
    }

    public Instant getDate() {
        return date;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Order order)) return false;
        return Objects.equals(id, order.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
