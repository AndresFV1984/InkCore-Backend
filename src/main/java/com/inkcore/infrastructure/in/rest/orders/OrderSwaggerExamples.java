package com.inkcore.infrastructure.in.rest.orders;

public final class OrderSwaggerExamples {

    private OrderSwaggerExamples() {
    }

    public static final String ORDER_ID = "ef658d09-bf30-43de-ba7a-edd0f14a61bd";
    public static final String DELIVERY_ID = "del-uuid-001";
    public static final String PAYMENT_ID = "pay-uuid-001";
    public static final String CLIENT_ID = "client-seed-001";

    public static final String CREATE_DELIVERY_REQUEST = """
            {
              "deliveryType": "parcial",
              "quantityDelivered": 500,
              "unitPrice": 1200.00,
              "sellerId": "seller-seed-001",
              "deliveredAt": "2026-09-05T14:30:00",
              "notes": "Entrega recogida en bodega"
            }
            """;

    public static final String CREATE_DELIVERY_RESPONSE = """
            {
              "headers": {
                "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "statusCode": 201,
                "code": "OK",
                "description": "Success"
              },
              "timestamp": "2026-09-05T14:30:01Z",
              "data": {
                "delivery": {
                  "orderDeliveryId": "del-uuid-001",
                  "deliveryNumber": "ODP-42",
                  "productionOrderId": "ef658d09-bf30-43de-ba7a-edd0f14a61bd",
                  "movementType": "entrega",
                  "deliveryType": "parcial",
                  "reversedDeliveryId": null,
                  "quantityDelivered": 500,
                  "unitPrice": 1200.00,
                  "totalValue": 600000.00,
                  "availableBefore": 800,
                  "sellerId": "seller-seed-001",
                  "deliveredAt": "2026-09-05T14:30:00",
                  "deliveredBy": "operator-seed-003",
                  "notes": "Entrega recogida en bodega"
                },
                "accountsReceivable": {
                  "accountsReceivableId": "ar-uuid-001",
                  "cxcNumber": "CXC-7",
                  "productionOrderId": "ef658d09-bf30-43de-ba7a-edd0f14a61bd",
                  "clientId": "client-seed-001",
                  "totalUnits": 1000,
                  "deliveredUnits": 500,
                  "pendingUnits": 500,
                  "totalOwed": 600000.00,
                  "totalPaid": 0,
                  "totalRemaining": 600000.00,
                  "totalCashPaid": 0,
                  "totalWithheld": 0,
                  "totalAdvancePaid": 0,
                  "status": "pendiente",
                  "hasMovements": true,
                  "openedAt": "2026-09-05T14:30:00",
                  "dueDate": "2026-10-05",
                  "paymentTermDays": 30,
                  "collectionStatus": "por_vencer",
                  "daysOverdue": 0,
                  "agingBucket": "current",
                  "dueSoon": true,
                  "overdue": false,
                  "lastDeliveryAt": "2026-09-05T14:30:00",
                  "lastPaymentNumber": null,
                  "lastPaymentAt": null
                }
              }
            }
            """;

    public static final String INSUFFICIENT_AVAILABILITY_RESPONSE = """
            {
              "headers": {
                "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "statusCode": 422,
                "code": "ERROR",
                "description": "Unprocessable Entity"
              },
              "timestamp": "2026-09-08T15:00:00Z",
              "path": "/InkCore-backend/api/v1/production-orders/ef658d09-bf30-43de-ba7a-edd0f14a61bd/deliveries",
              "message": "No se puede entregar 500 unidades: solo hay 300 disponibles para la OP OP-42",
              "errors": [
                "INSUFFICIENT_AVAILABILITY"
              ]
            }
            """;

    public static final String DELIVERY_LIST_RESPONSE = """
            {
              "headers": {
                "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "statusCode": 200,
                "code": "OK",
                "description": "Success"
              },
              "timestamp": "2026-09-05T14:35:00Z",
              "data": [
                {
                  "orderDeliveryId": "del-uuid-001",
                  "deliveryNumber": "ODP-42",
                  "productionOrderId": "ef658d09-bf30-43de-ba7a-edd0f14a61bd",
                  "movementType": "entrega",
                  "deliveryType": "parcial",
                  "reversedDeliveryId": null,
                  "quantityDelivered": 500,
                  "unitPrice": 1200.00,
                  "totalValue": 600000.00,
                  "availableBefore": 800,
                  "sellerId": "seller-seed-001",
                  "deliveredAt": "2026-09-05T14:30:00",
                  "deliveredBy": "operator-seed-003",
                  "notes": "Entrega recogida en bodega"
                }
              ]
            }
            """;

    public static final String REVERSE_DELIVERY_REQUEST = """
            {
              "reason": "Entrega duplicada"
            }
            """;

    public static final String REVERSE_DELIVERY_RESPONSE = """
            {
              "headers": {
                "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "statusCode": 201,
                "code": "OK",
                "description": "Success"
              },
              "timestamp": "2026-09-08T15:00:00Z",
              "data": {
                "delivery": {
                  "orderDeliveryId": "del-reversion-uuid-001",
                  "deliveryNumber": "ODP-43",
                  "productionOrderId": "ef658d09-bf30-43de-ba7a-edd0f14a61bd",
                  "movementType": "reversion",
                  "deliveryType": "parcial",
                  "reversedDeliveryId": "del-uuid-001",
                  "quantityDelivered": 500,
                  "unitPrice": 1200.00,
                  "totalValue": 600000.00,
                  "availableBefore": 300,
                  "sellerId": "seller-seed-001",
                  "deliveredAt": "2026-09-08T15:00:00",
                  "deliveredBy": "operator-seed-003",
                  "notes": "Anulado por: Entrega duplicada"
                },
                "accountsReceivable": {
                  "accountsReceivableId": "ar-uuid-001",
                  "cxcNumber": "CXC-7",
                  "productionOrderId": "ef658d09-bf30-43de-ba7a-edd0f14a61bd",
                  "clientId": "client-seed-001",
                  "totalUnits": 1000,
                  "deliveredUnits": 0,
                  "pendingUnits": 1000,
                  "totalOwed": 0,
                  "totalPaid": 0,
                  "totalRemaining": 0,
                  "totalCashPaid": 0,
                  "totalWithheld": 0,
                  "totalAdvancePaid": 0,
                  "status": "anulado",
                  "hasMovements": true,
                  "openedAt": "2026-09-05T14:30:00",
                  "dueDate": "2026-10-05",
                  "paymentTermDays": 30,
                  "collectionStatus": "anulada",
                  "daysOverdue": 0,
                  "agingBucket": "current",
                  "dueSoon": false,
                  "overdue": false,
                  "lastDeliveryAt": "2026-09-08T15:00:00",
                  "lastPaymentNumber": null,
                  "lastPaymentAt": null
                }
              }
            }
            """;

    public static final String REVERSE_DELIVERY_CONFLICT_RESPONSE = """
            {
              "headers": {
                "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "statusCode": 409,
                "code": "ERROR",
                "description": "Conflict"
              },
              "timestamp": "2026-09-08T15:00:00Z",
              "path": "/InkCore-backend/api/v1/production-orders/ef658d09-bf30-43de-ba7a-edd0f14a61bd/deliveries/del-uuid-001/reverse",
              "message": "Esta entrega ya fue anulada",
              "errors": [
                "ORDER_CONFLICT"
              ]
            }
            """;

    public static final String REVERSE_DELIVERY_BUSINESS_RULE_RESPONSE = """
            {
              "headers": {
                "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "statusCode": 422,
                "code": "ERROR",
                "description": "Unprocessable Entity"
              },
              "timestamp": "2026-09-08T15:00:00Z",
              "path": "/InkCore-backend/api/v1/production-orders/ef658d09-bf30-43de-ba7a-edd0f14a61bd/deliveries/del-uuid-001/reverse",
              "message": "No se puede anular la entrega: el saldo adeudado quedaría por debajo de lo ya abonado",
              "errors": [
                "ORDER_BUSINESS_RULE"
              ]
            }
            """;

    public static final String AVAILABILITY_RESPONSE = """
            {
              "headers": {
                "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "statusCode": 200,
                "code": "OK",
                "description": "Success"
              },
              "timestamp": "2026-09-08T15:00:00Z",
              "data": {
                "processed": 800,
                "delivered": 500,
                "available": 300
              }
            }
            """;

    public static final String ACCOUNTS_RECEIVABLE_RESPONSE = """
            {
              "headers": {
                "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "statusCode": 200,
                "code": "OK",
                "description": "Success"
              },
              "timestamp": "2026-09-08T15:00:00Z",
              "data": {
                "accountsReceivableId": "ar-uuid-001",
                "cxcNumber": "CXC-7",
                "productionOrderId": "ef658d09-bf30-43de-ba7a-edd0f14a61bd",
                "clientId": "client-seed-001",
                "totalUnits": 1000,
                "deliveredUnits": 500,
                "pendingUnits": 500,
                "totalOwed": 600000.00,
                "totalPaid": 200000.00,
                "totalRemaining": 400000.00,
                "totalCashPaid": 150000.00,
                "totalWithheld": 50000.00,
                "totalAdvancePaid": 0.00,
                "status": "parcial",
                "hasMovements": true,
                "openedAt": "2026-09-05T14:30:00",
                "dueDate": "2026-10-05",
                "paymentTermDays": 30,
                "collectionStatus": "por_vencer",
                "daysOverdue": 0,
                "agingBucket": "current",
                "dueSoon": true,
                "overdue": false,
                "lastDeliveryAt": "2026-09-05T14:30:00",
                "lastPaymentNumber": "ABN-4",
                "lastPaymentAt": "2026-09-05T16:00:00"
              }
            }
            """;

    public static final String ACCOUNTS_RECEIVABLE_WITHOUT_MOVEMENTS_RESPONSE = """
            {
              "headers": {
                "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "statusCode": 200,
                "code": "OK",
                "description": "Success"
              },
              "timestamp": "2026-09-08T15:00:00Z",
              "data": {
                "accountsReceivableId": null,
                "cxcNumber": null,
                "productionOrderId": "ef658d09-bf30-43de-ba7a-edd0f14a61bd",
                "clientId": "client-seed-001",
                "totalUnits": 1000,
                "deliveredUnits": 0,
                "pendingUnits": 1000,
                "totalOwed": 0,
                "totalPaid": 0,
                "totalRemaining": 0,
                "totalCashPaid": 0,
                "totalWithheld": 0,
                "totalAdvancePaid": 0,
                "status": "sin_movimientos",
                "hasMovements": false,
                "openedAt": null,
                "dueDate": null,
                "paymentTermDays": 0,
                "collectionStatus": "sin_vencimiento",
                "daysOverdue": 0,
                "agingBucket": "current",
                "dueSoon": false,
                "overdue": false,
                "lastDeliveryAt": null,
                "lastPaymentNumber": null,
                "lastPaymentAt": null
              }
            }
            """;

    public static final String CREATE_PAYMENT_REQUEST = """
            {
              "amount": 200000.00,
              "paymentType": "abono",
              "paymentMethod": "transferencia",
              "reference": "COMP-00123",
              "paidAt": "2026-09-05T16:00:00",
              "notes": "Abono inicial"
            }
            """;

    public static final String CREATE_RETENCION_REQUEST = """
            {
              "amount": 50000.00,
              "paymentType": "retencion",
              "paymentMethod": "retencion",
              "withholdingType": "retefuente",
              "withholdingBase": 1000000.00,
              "withholdingRate": 2.5,
              "certificateRef": "CERT-2026-001",
              "paidAt": "2026-09-05T16:05:00",
              "notes": "Retención en la fuente"
            }
            """;

    public static final String CREATE_ANTICIPO_REQUEST = """
            {
              "amount": 100000.00,
              "paymentType": "anticipo",
              "paymentMethod": "transferencia",
              "reference": "ANT-7788",
              "paidAt": "2026-09-04T10:00:00",
              "notes": "Anticipo del cliente"
            }
            """;

    public static final String CREATE_PAYMENT_RESPONSE = """
            {
              "headers": {
                "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "statusCode": 201,
                "code": "OK",
                "description": "Success"
              },
              "timestamp": "2026-09-05T16:00:01Z",
              "data": {
                "payment": {
                  "orderPaymentId": "pay-uuid-001",
                  "paymentNumber": "ABN-15",
                  "productionOrderId": "ef658d09-bf30-43de-ba7a-edd0f14a61bd",
                  "paymentType": "abono",
                  "amount": 200000.00,
                  "paymentMethod": "transferencia",
                  "reference": "COMP-00123",
                  "reversedPaymentId": null,
                  "withholdingType": null,
                  "withholdingBase": null,
                  "withholdingRate": null,
                  "certificateRef": null,
                  "invoiceId": null,
                  "paidAt": "2026-09-05T16:00:00",
                  "registeredBy": "operator-seed-003",
                  "notes": "Abono inicial"
                },
                "accountsReceivable": {
                  "accountsReceivableId": "ar-uuid-001",
                  "cxcNumber": "CXC-7",
                  "productionOrderId": "ef658d09-bf30-43de-ba7a-edd0f14a61bd",
                  "clientId": "client-seed-001",
                  "totalUnits": 1000,
                  "deliveredUnits": 500,
                  "pendingUnits": 500,
                  "totalOwed": 600000.00,
                  "totalPaid": 200000.00,
                  "totalRemaining": 400000.00,
                  "totalCashPaid": 200000.00,
                  "totalWithheld": 0,
                  "totalAdvancePaid": 0,
                  "status": "parcial",
                  "hasMovements": true,
                  "openedAt": "2026-09-05T14:30:00",
                  "dueDate": "2026-10-05",
                  "paymentTermDays": 30,
                  "collectionStatus": "por_vencer",
                  "daysOverdue": 0,
                  "agingBucket": "current",
                  "dueSoon": true,
                  "overdue": false,
                  "lastDeliveryAt": "2026-09-05T14:30:00",
                  "lastPaymentNumber": "ABN-4",
                  "lastPaymentAt": "2026-09-05T16:00:00"
                }
              }
            }
            """;

    public static final String REVERSE_PAYMENT_REQUEST = """
            {
              "reason": "Comprobante duplicado"
            }
            """;

    public static final String REVERSE_PAYMENT_RESPONSE = """
            {
              "headers": {
                "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "statusCode": 201,
                "code": "OK",
                "description": "Success"
              },
              "timestamp": "2026-09-08T15:00:00Z",
              "data": {
                "payment": {
                  "orderPaymentId": "reversion-uuid-001",
                  "paymentNumber": "ABN-16",
                  "productionOrderId": "ef658d09-bf30-43de-ba7a-edd0f14a61bd",
                  "paymentType": "reversion",
                  "amount": 200000.00,
                  "paymentMethod": "transferencia",
                  "reference": "COMP-00123",
                  "reversedPaymentId": "pay-uuid-001",
                  "withholdingType": null,
                  "withholdingBase": null,
                  "withholdingRate": null,
                  "certificateRef": null,
                  "invoiceId": null,
                  "paidAt": "2026-09-08T15:00:00",
                  "registeredBy": "operator-seed-003",
                  "notes": "Anulado por: Comprobante duplicado"
                },
                "accountsReceivable": {
                  "accountsReceivableId": "ar-uuid-001",
                  "cxcNumber": "CXC-7",
                  "productionOrderId": "ef658d09-bf30-43de-ba7a-edd0f14a61bd",
                  "clientId": "client-seed-001",
                  "totalUnits": 1000,
                  "deliveredUnits": 500,
                  "pendingUnits": 500,
                  "totalOwed": 600000.00,
                  "totalPaid": 0,
                  "totalRemaining": 600000.00,
                  "totalCashPaid": 0,
                  "totalWithheld": 0,
                  "totalAdvancePaid": 0,
                  "status": "pendiente",
                  "hasMovements": true,
                  "openedAt": "2026-09-05T14:30:00",
                  "dueDate": "2026-10-05",
                  "paymentTermDays": 30,
                  "collectionStatus": "por_vencer",
                  "daysOverdue": 0,
                  "agingBucket": "current",
                  "dueSoon": true,
                  "overdue": false,
                  "lastDeliveryAt": "2026-09-05T14:30:00",
                  "lastPaymentNumber": null,
                  "lastPaymentAt": null
                }
              }
            }
            """;

    public static final String PAYMENT_LIST_RESPONSE = """
            {
              "headers": {
                "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "statusCode": 200,
                "code": "OK",
                "description": "Success"
              },
              "timestamp": "2026-09-05T16:10:00Z",
              "data": [
                {
                  "orderPaymentId": "pay-uuid-001",
                  "paymentNumber": "ABN-15",
                  "productionOrderId": "ef658d09-bf30-43de-ba7a-edd0f14a61bd",
                  "paymentType": "abono",
                  "amount": 200000.00,
                  "paymentMethod": "transferencia",
                  "reference": "COMP-00123",
                  "reversedPaymentId": null,
                  "withholdingType": null,
                  "withholdingBase": null,
                  "withholdingRate": null,
                  "certificateRef": null,
                  "invoiceId": null,
                  "paidAt": "2026-09-05T16:00:00",
                  "registeredBy": "operator-seed-003",
                  "notes": "Abono inicial"
                },
                {
                  "orderPaymentId": "pay-ret-uuid-001",
                  "paymentNumber": "ABN-17",
                  "productionOrderId": "ef658d09-bf30-43de-ba7a-edd0f14a61bd",
                  "paymentType": "retencion",
                  "amount": 50000.00,
                  "paymentMethod": "retencion",
                  "reference": null,
                  "reversedPaymentId": null,
                  "withholdingType": "retefuente",
                  "withholdingBase": 1000000.00,
                  "withholdingRate": 2.5000,
                  "certificateRef": "CERT-2026-001",
                  "invoiceId": null,
                  "paidAt": "2026-09-05T16:05:00",
                  "registeredBy": "operator-seed-003",
                  "notes": "Retención en la fuente"
                }
              ]
            }
            """;

    public static final String AR_LIST_RESPONSE = """
            {
              "headers": {
                "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "statusCode": 200,
                "code": "OK",
                "description": "Success"
              },
              "timestamp": "2026-09-05T16:15:00Z",
              "data": {
                "content": [
                  {
                    "accountsReceivableId": "ar-uuid-001",
                    "cxcNumber": "CXC-7",
                    "productionOrderId": "ef658d09-bf30-43de-ba7a-edd0f14a61bd",
                    "orderNumber": "OP-142",
                    "clientId": "client-seed-001",
                    "clientName": "Distribuciones ACME",
                    "totalUnits": 1000,
                    "deliveredUnits": 500,
                    "pendingUnits": 500,
                    "totalOwed": 600000.00,
                    "totalPaid": 200000.00,
                    "totalRemaining": 400000.00,
                    "totalCashPaid": 150000.00,
                    "totalWithheld": 50000.00,
                    "totalAdvancePaid": 0.00,
                    "status": "parcial",
                    "openedAt": "2026-09-05T14:30:00",
                    "dueDate": "2026-10-05",
                    "paymentTermDays": 30,
                    "collectionStatus": "vencida",
                    "daysOverdue": 12,
                    "agingBucket": "1-30",
                    "overdue": true,
                    "dueSoon": false,
                    "lastDeliveryNumber": "ODP-42",
                    "lastDeliveryAt": "2026-09-05T14:30:00",
                    "lastPaymentNumber": "ABN-4",
                    "lastPaymentAt": "2026-09-05T16:00:00"
                  }
                ],
                "page": 0,
                "size": 20,
                "totalElements": 1,
                "totalPages": 1,
                "hasNext": false
              }
            }
            """;

    public static final String AR_DETAIL_RESPONSE = """
            {
              "headers": {
                "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "statusCode": 200,
                "code": "OK",
                "description": "Success"
              },
              "timestamp": "2026-09-05T16:20:00Z",
              "data": {
                "summary": {
                  "accountsReceivableId": "ar-uuid-001",
                  "cxcNumber": "CXC-7",
                  "productionOrderId": "ef658d09-bf30-43de-ba7a-edd0f14a61bd",
                  "orderNumber": "OP-142",
                  "clientId": "client-seed-001",
                  "clientName": "Distribuciones ACME",
                  "totalUnits": 1000,
                  "deliveredUnits": 500,
                  "pendingUnits": 500,
                  "totalOwed": 600000.00,
                  "totalPaid": 200000.00,
                  "totalRemaining": 400000.00,
                  "totalCashPaid": 150000.00,
                  "totalWithheld": 50000.00,
                  "totalAdvancePaid": 0.00,
                  "status": "parcial",
                  "openedAt": "2026-09-05T14:30:00",
                  "dueDate": "2026-10-05",
                  "paymentTermDays": 30,
                  "collectionStatus": "por_vencer",
                  "daysOverdue": 0,
                  "agingBucket": "current",
                  "overdue": false,
                  "dueSoon": true,
                  "lastDeliveryNumber": "ODP-42",
                  "lastDeliveryAt": "2026-09-05T14:30:00",
                  "lastPaymentNumber": "ABN-4",
                  "lastPaymentAt": "2026-09-05T16:00:00"
                },
                "deliveries": [
                  {
                    "orderDeliveryId": "del-uuid-001",
                    "deliveryNumber": "ODP-42",
                    "productionOrderId": "ef658d09-bf30-43de-ba7a-edd0f14a61bd",
                    "movementType": "entrega",
                    "deliveryType": "parcial",
                    "reversedDeliveryId": null,
                    "quantityDelivered": 500,
                    "unitPrice": 1200.00,
                    "totalValue": 600000.00,
                    "availableBefore": 800,
                    "sellerId": "seller-seed-001",
                    "deliveredAt": "2026-09-05T14:30:00",
                    "deliveredBy": "operator-seed-003",
                    "notes": "Entrega recogida en bodega"
                  }
                ],
                "payments": [
                  {
                    "orderPaymentId": "pay-uuid-001",
                    "paymentNumber": "ABN-15",
                    "productionOrderId": "ef658d09-bf30-43de-ba7a-edd0f14a61bd",
                    "paymentType": "abono",
                    "amount": 150000.00,
                    "paymentMethod": "transferencia",
                    "reference": "COMP-00123",
                    "reversedPaymentId": null,
                    "withholdingType": null,
                    "withholdingBase": null,
                    "withholdingRate": null,
                    "certificateRef": null,
                    "invoiceId": null,
                    "paidAt": "2026-09-05T16:00:00",
                    "registeredBy": "operator-seed-003",
                    "notes": "Abono inicial"
                  },
                  {
                    "orderPaymentId": "pay-ret-uuid-001",
                    "paymentNumber": "ABN-17",
                    "productionOrderId": "ef658d09-bf30-43de-ba7a-edd0f14a61bd",
                    "paymentType": "retencion",
                    "amount": 50000.00,
                    "paymentMethod": "retencion",
                    "reference": null,
                    "reversedPaymentId": null,
                    "withholdingType": "retefuente",
                    "withholdingBase": 1000000.00,
                    "withholdingRate": 2.5000,
                    "certificateRef": "CERT-2026-001",
                    "invoiceId": null,
                    "paidAt": "2026-09-05T16:05:00",
                    "registeredBy": "operator-seed-003",
                    "notes": "Retención en la fuente"
                  }
                ]
              }
            }
            """;

    public static final String CLIENT_ACCOUNTS_RECEIVABLE_RESPONSE = """
            {
              "headers": {
                "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "statusCode": 200,
                "code": "OK",
                "description": "Success"
              },
              "timestamp": "2026-09-08T15:00:00Z",
              "data": {
                "clientId": "client-seed-001",
                "clientName": "Distribuciones ACME",
                "orders": [
                  {
                    "accountsReceivableId": "ar-uuid-001",
                    "cxcNumber": "CXC-7",
                    "productionOrderId": "ef658d09-bf30-43de-ba7a-edd0f14a61bd",
                    "orderNumber": "OP-142",
                    "totalUnits": 1000,
                    "deliveredUnits": 500,
                    "pendingUnits": 500,
                    "totalOwed": 600000.00,
                    "totalPaid": 200000.00,
                    "totalRemaining": 400000.00,
                    "status": "parcial"
                  }
                ],
                "totalOwed": 600000.00,
                "totalPaid": 200000.00,
                "totalRemaining": 400000.00
              }
            }
            """;
}
