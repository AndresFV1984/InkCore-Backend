package com.inkcore.infrastructure.out.persistence.order.mapper;

import com.inkcore.domain.order.model.AccountsReceivableStatus;
import com.inkcore.domain.order.model.AccountsReceivable;
import com.inkcore.domain.order.model.DeliveryMovementType;
import com.inkcore.domain.order.model.DeliveryType;
import com.inkcore.domain.order.model.CustomerOrder;
import com.inkcore.domain.order.model.OrderDelivery;
import com.inkcore.domain.order.model.OrderPayment;
import com.inkcore.domain.order.model.PaymentMethod;
import com.inkcore.domain.order.model.PaymentType;
import com.inkcore.domain.order.model.WithholdingType;
import com.inkcore.infrastructure.out.persistence.order.entity.AccountsReceivableEntity;
import com.inkcore.infrastructure.out.persistence.order.entity.OrderDeliveryEntity;
import com.inkcore.infrastructure.out.persistence.order.entity.CustomerOrderEntity;
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
        entity.setDeliveryNumber(delivery.getDeliveryNumber());
        entity.setProductionOrderId(delivery.getProductionOrderId());
        entity.setClientId(delivery.getClientId());
        entity.setSellerId(delivery.getSellerId());
        entity.setMovementType(delivery.getMovementType() == null
                ? DeliveryMovementType.ENTREGA.getDbValue()
                : delivery.getMovementType().getDbValue());
        entity.setDeliveryType(delivery.getDeliveryType().getDbValue());
        entity.setReversedDeliveryId(delivery.getReversedDeliveryId());
        entity.setQuantityDelivered(delivery.getQuantityDelivered());
        entity.setUnitPrice(delivery.getUnitPrice());
        entity.setTotalValue(delivery.getTotalValue());
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
        delivery.setDeliveryNumber(entity.getDeliveryNumber());
        delivery.setProductionOrderId(entity.getProductionOrderId());
        delivery.setClientId(entity.getClientId());
        delivery.setSellerId(entity.getSellerId());
        delivery.setMovementType(DeliveryMovementType.fromValue(entity.getMovementType()));
        delivery.setDeliveryType(DeliveryType.fromValue(entity.getDeliveryType()));
        delivery.setReversedDeliveryId(entity.getReversedDeliveryId());
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
        entity.setPaymentNumber(payment.getPaymentNumber());
        entity.setProductionOrderId(payment.getProductionOrderId());
        entity.setClientId(payment.getClientId());
        entity.setPaymentType(payment.getPaymentType().getDbValue());
        entity.setAmount(payment.getAmount());
        entity.setPaymentMethod(payment.getPaymentMethod().getDbValue());
        entity.setReference(payment.getReference());
        entity.setReversedPaymentId(payment.getReversedPaymentId());
        entity.setWithholdingType(payment.getWithholdingType() == null
                ? null
                : payment.getWithholdingType().getDbValue());
        entity.setWithholdingBase(payment.getWithholdingBase());
        entity.setWithholdingRate(payment.getWithholdingRate());
        entity.setCertificateRef(payment.getCertificateRef());
        entity.setInvoiceId(payment.getInvoiceId());
        entity.setPaidAt(payment.getPaidAt());
        entity.setRegisteredBy(payment.getRegisteredBy());
        entity.setNotes(payment.getNotes());
        entity.setCreatedAt(payment.getCreatedAt());
    }

    public static OrderPayment toDomain(OrderPaymentEntity entity) {
        OrderPayment payment = new OrderPayment();
        payment.setOrderPaymentId(entity.getOrderPaymentId());
        payment.setCompanyId(entity.getCompanyId());
        payment.setPaymentNumber(entity.getPaymentNumber());
        payment.setProductionOrderId(entity.getProductionOrderId());
        payment.setClientId(entity.getClientId());
        payment.setPaymentType(PaymentType.fromValue(entity.getPaymentType()));
        payment.setAmount(entity.getAmount());
        payment.setPaymentMethod(PaymentMethod.fromValue(entity.getPaymentMethod()));
        payment.setReference(entity.getReference());
        payment.setReversedPaymentId(entity.getReversedPaymentId());
        payment.setWithholdingType(WithholdingType.fromValue(entity.getWithholdingType()));
        payment.setWithholdingBase(entity.getWithholdingBase());
        payment.setWithholdingRate(entity.getWithholdingRate());
        payment.setCertificateRef(entity.getCertificateRef());
        payment.setInvoiceId(entity.getInvoiceId());
        payment.setPaidAt(entity.getPaidAt());
        payment.setRegisteredBy(entity.getRegisteredBy());
        payment.setNotes(entity.getNotes());
        payment.setCreatedAt(entity.getCreatedAt());
        return payment;
    }

    public static AccountsReceivable toDomain(AccountsReceivableEntity entity) {
        AccountsReceivable summary = new AccountsReceivable();
        summary.setAccountsReceivableId(entity.getAccountsReceivableId());
        summary.setCompanyId(entity.getCompanyId());
        summary.setCxcNumber(entity.getCxcNumber());
        summary.setAbonosNumber(entity.getAbonosNumber());
        summary.setProductionOrderId(entity.getProductionOrderId());
        summary.setClientId(entity.getClientId());
        summary.setTotalUnits(entity.getTotalUnits());
        summary.setDeliveredUnits(entity.getDeliveredUnits());
        summary.setPendingUnits(entity.getPendingUnits());
        summary.setTotalOwed(entity.getTotalOwed());
        summary.setTotalPaid(entity.getTotalPaid());
        summary.setTotalRemaining(entity.getTotalRemaining());
        summary.setTotalCashPaid(entity.getTotalCashPaid() == null ? java.math.BigDecimal.ZERO : entity.getTotalCashPaid());
        summary.setTotalWithheld(entity.getTotalWithheld() == null ? java.math.BigDecimal.ZERO : entity.getTotalWithheld());
        summary.setTotalAdvancePaid(entity.getTotalAdvancePaid() == null ? java.math.BigDecimal.ZERO : entity.getTotalAdvancePaid());
        summary.setOpenedAt(entity.getOpenedAt());
        summary.setDueDate(entity.getDueDate());
        summary.setPaymentTermDays(entity.getPaymentTermDays());
        summary.setStatus(AccountsReceivableStatus.fromValue(entity.getStatus()));
        summary.setLastDeliveryAt(entity.getLastDeliveryAt());
        summary.setLastPaymentNumber(entity.getLastPaymentNumber());
        summary.setLastPaymentAt(entity.getLastPaymentAt());
        summary.setUpdatedAt(entity.getUpdatedAt());
        return summary;
    }

    public static CustomerOrderEntity toEntity(CustomerOrder order) {
        CustomerOrderEntity entity = new CustomerOrderEntity();
        entity.markNew();
        entity.setCustomerOrderId(order.getCustomerOrderId());
        entity.setCompanyId(order.getCompanyId());
        entity.setOdpNumber(order.getOdpNumber());
        entity.setProductionOrderId(order.getProductionOrderId());
        entity.setClientId(order.getClientId());
        entity.setCreatedAt(order.getCreatedAt());
        entity.setCreatedBy(order.getCreatedBy());
        return entity;
    }

    public static CustomerOrder toDomain(CustomerOrderEntity entity) {
        CustomerOrder order = new CustomerOrder();
        order.setCustomerOrderId(entity.getCustomerOrderId());
        order.setCompanyId(entity.getCompanyId());
        order.setOdpNumber(entity.getOdpNumber());
        order.setProductionOrderId(entity.getProductionOrderId());
        order.setClientId(entity.getClientId());
        order.setCreatedAt(entity.getCreatedAt());
        order.setCreatedBy(entity.getCreatedBy());
        return order;
    }
}
