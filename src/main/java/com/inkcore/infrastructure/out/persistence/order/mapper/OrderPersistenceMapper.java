package com.inkcore.infrastructure.out.persistence.order.mapper;

import com.inkcore.domain.order.model.AccountsReceivableStatus;
import com.inkcore.domain.order.model.AccountsReceivable;
import com.inkcore.domain.order.model.CxcDocumentConcept;
import com.inkcore.domain.order.model.CxcDocumentStatus;
import com.inkcore.domain.order.model.DeliveryMovementType;
import com.inkcore.domain.order.model.DeliveryType;
import com.inkcore.domain.order.model.CustomerOrder;
import com.inkcore.domain.order.model.NegotiatedAddition;
import com.inkcore.domain.order.model.NegotiatedAdditionApprovalChannel;
import com.inkcore.domain.order.model.NegotiatedAdditionMovementType;
import com.inkcore.domain.order.model.NegotiatedAdditionReason;
import com.inkcore.domain.order.model.OrderCxcDocument;
import com.inkcore.domain.order.model.OrderDelivery;
import com.inkcore.domain.order.model.OrderPayment;
import com.inkcore.domain.order.model.OrderPaymentApplication;
import com.inkcore.domain.order.model.PaymentApplicationStatus;
import com.inkcore.domain.order.model.PaymentMethod;
import com.inkcore.domain.order.model.PaymentType;
import com.inkcore.domain.order.model.WithholdingType;
import com.inkcore.infrastructure.out.persistence.order.entity.AccountsReceivableEntity;
import com.inkcore.infrastructure.out.persistence.order.entity.OrderCxcDocumentEntity;
import com.inkcore.infrastructure.out.persistence.order.entity.OrderDeliveryEntity;
import com.inkcore.infrastructure.out.persistence.order.entity.CustomerOrderEntity;
import com.inkcore.infrastructure.out.persistence.order.entity.NegotiatedAdditionEntity;
import com.inkcore.infrastructure.out.persistence.order.entity.OrderPaymentApplicationEntity;
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
        entity.setPaymentConcept(
                payment.getPaymentConcept() == null
                        ? CxcDocumentConcept.ENTREGA.getDbValue()
                        : payment.getPaymentConcept().getDbValue()
        );
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
        payment.setPaymentConcept(CxcDocumentConcept.fromValue(entity.getPaymentConcept()));
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

    public static OrderCxcDocumentEntity toEntity(OrderCxcDocument document) {
        OrderCxcDocumentEntity entity = new OrderCxcDocumentEntity();
        entity.markNew();
        copy(document, entity);
        return entity;
    }

    public static void copy(OrderCxcDocument document, OrderCxcDocumentEntity entity) {
        entity.setCxcDocumentId(document.getCxcDocumentId());
        entity.setCompanyId(document.getCompanyId());
        entity.setCxcNumber(document.getCxcNumber());
        entity.setProductionOrderId(document.getProductionOrderId());
        entity.setCustomerOrderId(document.getCustomerOrderId());
        entity.setClientId(document.getClientId());
        entity.setConcept(document.getConcept().getDbValue());
        entity.setSourceDeliveryId(document.getSourceDeliveryId());
        entity.setSourceAdditionId(document.getSourceAdditionId());
        entity.setTotal(document.getTotal());
        entity.setStatus(document.getStatus().getDbValue());
        entity.setIssuedAt(document.getIssuedAt());
        entity.setDueDate(document.getDueDate());
        entity.setPaymentTermDays(document.getPaymentTermDays());
        entity.setCreatedAt(document.getCreatedAt());
        entity.setUpdatedAt(document.getUpdatedAt());
    }

    public static OrderCxcDocument toDomain(OrderCxcDocumentEntity entity) {
        OrderCxcDocument document = new OrderCxcDocument();
        document.setCxcDocumentId(entity.getCxcDocumentId());
        document.setCompanyId(entity.getCompanyId());
        document.setCxcNumber(entity.getCxcNumber());
        document.setProductionOrderId(entity.getProductionOrderId());
        document.setCustomerOrderId(entity.getCustomerOrderId());
        document.setClientId(entity.getClientId());
        document.setConcept(CxcDocumentConcept.fromValue(entity.getConcept()));
        document.setSourceDeliveryId(entity.getSourceDeliveryId());
        document.setSourceAdditionId(entity.getSourceAdditionId());
        document.setTotal(entity.getTotal());
        document.setStatus(CxcDocumentStatus.fromValue(entity.getStatus()));
        document.setIssuedAt(entity.getIssuedAt());
        document.setDueDate(entity.getDueDate());
        document.setPaymentTermDays(entity.getPaymentTermDays());
        document.setCreatedAt(entity.getCreatedAt());
        document.setUpdatedAt(entity.getUpdatedAt());
        return document;
    }

    public static OrderPaymentApplicationEntity toEntity(OrderPaymentApplication application) {
        OrderPaymentApplicationEntity entity = new OrderPaymentApplicationEntity();
        entity.markNew();
        copy(application, entity);
        return entity;
    }

    public static void copy(OrderPaymentApplication application, OrderPaymentApplicationEntity entity) {
        entity.setApplicationId(application.getApplicationId());
        entity.setCompanyId(application.getCompanyId());
        entity.setPaymentId(application.getPaymentId());
        entity.setProductionOrderId(application.getProductionOrderId());
        entity.setCustomerOrderId(application.getCustomerOrderId());
        entity.setCxcDocumentId(application.getCxcDocumentId());
        entity.setAmount(application.getAmount());
        entity.setOriginApplicationId(application.getOriginApplicationId());
        entity.setStatus(application.getStatus().getDbValue());
        entity.setAppliedAt(application.getAppliedAt());
        entity.setCreatedAt(application.getCreatedAt());
    }

    public static OrderPaymentApplication toDomain(OrderPaymentApplicationEntity entity) {
        OrderPaymentApplication application = new OrderPaymentApplication();
        application.setApplicationId(entity.getApplicationId());
        application.setCompanyId(entity.getCompanyId());
        application.setPaymentId(entity.getPaymentId());
        application.setProductionOrderId(entity.getProductionOrderId());
        application.setCustomerOrderId(entity.getCustomerOrderId());
        application.setCxcDocumentId(entity.getCxcDocumentId());
        application.setAmount(entity.getAmount());
        application.setOriginApplicationId(entity.getOriginApplicationId());
        application.setStatus(PaymentApplicationStatus.fromValue(entity.getStatus()));
        application.setAppliedAt(entity.getAppliedAt());
        application.setCreatedAt(entity.getCreatedAt());
        return application;
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

    public static NegotiatedAdditionEntity toEntity(NegotiatedAddition addition) {
        NegotiatedAdditionEntity entity = new NegotiatedAdditionEntity();
        entity.markNew();
        copy(addition, entity);
        return entity;
    }

    public static void copy(NegotiatedAddition addition, NegotiatedAdditionEntity entity) {
        entity.setOrderNegotiatedAdditionId(addition.getOrderNegotiatedAdditionId());
        entity.setCompanyId(addition.getCompanyId());
        entity.setAdditionNumber(addition.getAdditionNumber());
        entity.setProductionOrderId(addition.getProductionOrderId());
        entity.setClientId(addition.getClientId());
        entity.setMovementType(addition.getMovementType() == null
                ? NegotiatedAdditionMovementType.REGISTRO.getDbValue()
                : addition.getMovementType().getDbValue());
        entity.setConcept(addition.getConcept());
        entity.setChangeReason(addition.getChangeReason() == null
                ? null
                : addition.getChangeReason().getDbValue());
        entity.setAmount(addition.getAmount());
        entity.setScheduleImpactDays(addition.getScheduleImpactDays());
        entity.setApprovedByClientName(addition.getApprovedByClientName());
        entity.setApprovalChannel(addition.getApprovalChannel() == null
                ? null
                : addition.getApprovalChannel().getDbValue());
        entity.setApprovalReference(addition.getApprovalReference());
        entity.setApprovedAt(addition.getApprovedAt());
        entity.setReversedAdditionId(addition.getReversedAdditionId());
        entity.setRegisteredBy(addition.getRegisteredBy());
        entity.setNotes(addition.getNotes());
        entity.setCreatedAt(addition.getCreatedAt());
        entity.setVoidedAt(addition.getVoidedAt());
        entity.setVoidedBy(addition.getVoidedBy());
    }

    public static NegotiatedAddition toDomain(NegotiatedAdditionEntity entity) {
        NegotiatedAddition addition = new NegotiatedAddition();
        addition.setOrderNegotiatedAdditionId(entity.getOrderNegotiatedAdditionId());
        addition.setCompanyId(entity.getCompanyId());
        addition.setAdditionNumber(entity.getAdditionNumber());
        addition.setProductionOrderId(entity.getProductionOrderId());
        addition.setClientId(entity.getClientId());
        addition.setMovementType(NegotiatedAdditionMovementType.fromValue(entity.getMovementType()));
        addition.setConcept(entity.getConcept());
        addition.setChangeReason(NegotiatedAdditionReason.fromValue(entity.getChangeReason()));
        addition.setAmount(entity.getAmount());
        addition.setScheduleImpactDays(entity.getScheduleImpactDays());
        addition.setApprovedByClientName(entity.getApprovedByClientName());
        addition.setApprovalChannel(NegotiatedAdditionApprovalChannel.fromValue(entity.getApprovalChannel()));
        addition.setApprovalReference(entity.getApprovalReference());
        addition.setApprovedAt(entity.getApprovedAt());
        addition.setReversedAdditionId(entity.getReversedAdditionId());
        addition.setRegisteredBy(entity.getRegisteredBy());
        addition.setNotes(entity.getNotes());
        addition.setCreatedAt(entity.getCreatedAt());
        addition.setVoidedAt(entity.getVoidedAt());
        addition.setVoidedBy(entity.getVoidedBy());
        return addition;
    }
}
