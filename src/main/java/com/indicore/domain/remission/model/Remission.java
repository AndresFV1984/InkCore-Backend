package com.indicore.domain.remission.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad de dominio: remisión de entrega.
 */
public final class Remission {

    private final UUID id;
    private final UUID orderId;
    private final UUID clientId;
    private final Instant date;
    private final String observations;
    private final RemissionStatus status;
    private final BigDecimal totalAmount;

    private Remission(
            UUID id,
            UUID orderId,
            UUID clientId,
            Instant date,
            String observations,
            RemissionStatus status,
            BigDecimal totalAmount
    ) {
        this.id = id;
        this.orderId = orderId;
        this.clientId = clientId;
        this.date = date;
        this.observations = observations;
        this.status = status;
        this.totalAmount = totalAmount;
    }

    public static Remission createNew(
            UUID orderId,
            UUID clientId,
            String observations,
            BigDecimal totalAmount
    ) {
        if (orderId == null || clientId == null) {
            throw new IllegalArgumentException("Orden y cliente son obligatorios");
        }
        return new Remission(
                UUID.randomUUID(),
                orderId,
                clientId,
                Instant.now(),
                observations != null ? observations.trim() : "",
                RemissionStatus.PENDIENTE,
                totalAmount != null ? totalAmount : BigDecimal.ZERO
        );
    }

    public static Remission reconstitute(
            UUID id,
            UUID orderId,
            UUID clientId,
            Instant date,
            String observations,
            RemissionStatus status,
            BigDecimal totalAmount
    ) {
        return new Remission(id, orderId, clientId, date, observations, status, totalAmount);
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public UUID getClientId() {
        return clientId;
    }

    public Instant getDate() {
        return date;
    }

    public String getObservations() {
        return observations;
    }

    public RemissionStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Remission remission)) return false;
        return Objects.equals(id, remission.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
