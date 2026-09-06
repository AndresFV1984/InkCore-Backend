package com.inkcore.infrastructure.out.persistence.order.mapper;

import com.inkcore.domain.order.model.ArStatus;
import com.inkcore.domain.order.model.ArSummary;
import com.inkcore.domain.order.model.DeliveryType;
import com.inkcore.domain.order.model.OrderDelivery;
import com.inkcore.domain.order.model.OrderPayment;
import com.inkcore.domain.order.model.PaymentMethod;
import com.inkcore.domain.order.model.PaymentType;
import com.inkcore.infrastructure.out.persistence.order.entity.ArSummaryEntity;
import com.inkcore.infrastructure.out.persistence.order.entity.OrderDeliveryEntity;
import com.inkcore.infrastructure.out.persistence.order.entity.OrderPaymentEntity;

public final class OrderPersistenceMapper {

    private OrderPersistenceMapper() {
    }

    public static OrderDeliveryEntity toEntity(OrderDelivery delivery) {
        OrderDeliveryEntity entity = new OrderDeliveryEntity();
        entity.markNew();
        copy(delivery, entity);
        return entity;
    }

    public static void copy(OrderDelivery delivery, OrderDeliveryEntity entity) {
        entity.setOrderDeliveryId(delivery.getOrderDeliveryId());
        entity.setCompanyId(delivery.getCompanyId());
        entity.setProductionOrderId(delivery.getProductionOrderId());
        entity.setClientId(delivery.getClientId());
        entity.setSellerId(delivery.getSellerId());
        entity.setDeliveryType(delivery.getDeliveryType().getDbValue());
        entity.setQuantityDelivered(delivery.getQuantityDelivered());
        entity.setUnitPrice(delivery.getUnitPrice());
        entity.setTotalValue(delivery.getTotalValue());
        entity.setAvailableBefore(delivery.getAvailableBefore());
        entity.setWorkNameSnapshot(delivery.getWorkNameSnapshot());
        entity.setClientNameSnapshot(delivery.getClientNameSnapshot());
        entity.setDeliveredAt(delivery.getDeliveredAt());
        entity.setDeliveredBy(delivery.getDeliveredBy());
        entity.setNotes(delivery.getNotes());
        entity.setCreatedAt(delivery.getCreatedAt());
    }

    public static OrderDelivery toDomain(OrderDeliveryEntity entity) {
        OrderDelivery delivery = new OrderDelivery();
        delivery.setOrderDeliveryId(entity.getOrderDeliveryId());
        delivery.setCompanyId(entity.getCompanyId());
        delivery.setProductionOrderId(entity.getProductionOrderId());
        delivery.setClientId(entity.getClientId());
        delivery.setSellerId(entity.getSellerId());
        delivery.setDeliveryType(DeliveryType.fromValue(entity.getDeliveryType()));
        delivery.setQuantityDelivered(entity.getQuantityDelivered());
        delivery.setUnitPrice(entity.getUnitPrice());
        delivery.setTotalValue(entity.getTotalValue());
        delivery.setAvailableBefore(entity.getAvailableBefore());
        delivery.setWorkNameSnapshot(entity.getWorkNameSnapshot());
        delivery.setClientNameSnapshot(entity.getClientNameSnapshot());
        delivery.setDeliveredAt(entity.getDeliveredAt());
        delivery.setDeliveredBy(entity.getDeliveredBy());
        delivery.setNotes(entity.getNotes());
        delivery.setCreatedAt(entity.getCreatedAt());
        return delivery;
    }

    public static OrderPaymentEntity toEntity(OrderPayment payment) {
        OrderPaymentEntity entity = new OrderPaymentEntity();
        entity.markNew();
        copy(payment, entity);
        return entity;
    }

    public static void copy(OrderPayment payment, OrderPaymentEntity entity) {
        entity.setOrderPaymentId(payment.getOrderPaymentId());
        entity.setCompanyId(payment.getCompanyId());
        entity.setProductionOrderId(payment.getProductionOrderId());
        entity.setClientId(payment.getClientId());
        entity.setPaymentType(payment.getPaymentType().getDbValue());
        entity.setAmount(payment.getAmount());
        entity.setPaymentMethod(payment.getPaymentMethod().getDbValue());
        entity.setReference(payment.getReference());
        entity.setReversedPaymentId(payment.getReversedPaymentId());
        entity.setPaidAt(payment.getPaidAt());
        entity.setRegisteredBy(payment.getRegisteredBy());
        entity.setNotes(payment.getNotes());
        entity.setCreatedAt(payment.getCreatedAt());
    }

    public static OrderPayment toDomain(OrderPaymentEntity entity) {
        OrderPayment payment = new OrderPayment();
        payment.setOrderPaymentId(entity.getOrderPaymentId());
        payment.setCompanyId(entity.getCompanyId());
        payment.setProductionOrderId(entity.getProductionOrderId());
        payment.setClientId(entity.getClientId());
        payment.setPaymentType(PaymentType.fromValue(entity.getPaymentType()));
        payment.setAmount(entity.getAmount());
        payment.setPaymentMethod(PaymentMethod.fromValue(entity.getPaymentMethod()));
        payment.setReference(entity.getReference());
        payment.setReversedPaymentId(entity.getReversedPaymentId());
        payment.setPaidAt(entity.getPaidAt());
        payment.setRegisteredBy(entity.getRegisteredBy());
        payment.setNotes(entity.getNotes());
        payment.setCreatedAt(entity.getCreatedAt());
        return payment;
    }

    public static ArSummary toDomain(ArSummaryEntity entity) {
        ArSummary summary = new ArSummary();
        summary.setCompanyId(entity.getCompanyId());
        summary.setProductionOrderId(entity.getProductionOrderId());
        summary.setClientId(entity.getClientId());
        summary.setTotalUnits(entity.getTotalUnits());
        summary.setDeliveredUnits(entity.getDeliveredUnits());
        summary.setPendingUnits(entity.getPendingUnits());
        summary.setTotalOwed(entity.getTotalOwed());
        summary.setTotalPaid(entity.getTotalPaid());
        summary.setTotalRemaining(entity.getTotalRemaining());
        summary.setStatus(ArStatus.fromValue(entity.getStatus()));
        summary.setLastDeliveryAt(entity.getLastDeliveryAt());
        summary.setLastPaymentAt(entity.getLastPaymentAt());
        summary.setUpdatedAt(entity.getUpdatedAt());
        return summary;
    }
}
