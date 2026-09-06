package com.inkcore.infrastructure.in.rest.orders;

public final class OrderSwaggerExamples {

    private OrderSwaggerExamples() {
    }

    public static final String ORDER_ID = "ef658d09-bf30-43de-ba7a-edd0f14a61bd";
    public static final String PAYMENT_ID = "pay-uuid-001";

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
                "orderDeliveryId": "del-uuid-001",
                "availableBefore": 800,
                "totalValue": 600000.00,
                "accountsReceivable": {
                  "deliveredUnits": 500,
                  "pendingUnits": 500,
                  "totalOwed": 600000.00,
                  "totalPaid": 0,
                  "totalRemaining": 600000.00,
                  "status": "pendiente"
                }
              }
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
                  "productionOrderId": "ef658d09-bf30-43de-ba7a-edd0f14a61bd",
                  "deliveryType": "parcial",
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

    public static final String AVAILABILITY_RESPONSE = """
            {
              "headers": {
                "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "statusCode": 200,
                "code": "OK",
                "description": "Success"
              },
              "timestamp": "2026-09-05T14:20:00Z",
              "data": {
                "processed": 800,
                "delivered": 500,
                "available": 300
              }
            }
            """;

    public static final String CREATE_PAYMENT_REQUEST = """
            {
              "amount": 200000.00,
              "paymentMethod": "transferencia",
              "reference": "COMP-00123",
              "paidAt": "2026-09-05T16:00:00",
              "notes": "Abono inicial"
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
                "orderPaymentId": "pay-uuid-001",
                "paymentType": "abono",
                "amount": 200000.00,
                "accountsReceivable": {
                  "deliveredUnits": 500,
                  "pendingUnits": 500,
                  "totalOwed": 600000.00,
                  "totalPaid": 200000.00,
                  "totalRemaining": 400000.00,
                  "status": "parcial"
                }
              }
            }
            """;

    public static final String REVERSE_PAYMENT_REQUEST = """
            {
              "reason": "Comprobante duplicado"
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
                  "productionOrderId": "ef658d09-bf30-43de-ba7a-edd0f14a61bd",
                  "paymentType": "abono",
                  "amount": 200000.00,
                  "paymentMethod": "transferencia",
                  "reference": "COMP-00123",
                  "reversedPaymentId": null,
                  "paidAt": "2026-09-05T16:00:00",
                  "registeredBy": "operator-seed-003",
                  "notes": "Abono inicial"
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
                    "status": "parcial",
                    "lastDeliveryAt": "2026-09-05T14:30:00",
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
                  "status": "parcial",
                  "lastDeliveryAt": "2026-09-05T14:30:00",
                  "lastPaymentAt": "2026-09-05T16:00:00"
                },
                "deliveries": [
                  {
                    "orderDeliveryId": "del-uuid-001",
                    "productionOrderId": "ef658d09-bf30-43de-ba7a-edd0f14a61bd",
                    "deliveryType": "parcial",
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
                    "productionOrderId": "ef658d09-bf30-43de-ba7a-edd0f14a61bd",
                    "paymentType": "abono",
                    "amount": 200000.00,
                    "paymentMethod": "transferencia",
                    "reference": "COMP-00123",
                    "reversedPaymentId": null,
                    "paidAt": "2026-09-05T16:00:00",
                    "registeredBy": "operator-seed-003",
                    "notes": "Abono inicial"
                  }
                ]
              }
            }
            """;
}
