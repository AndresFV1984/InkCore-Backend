package com.inkcore.infrastructure.in.rest.reports;

final class ProfitabilitySwaggerExamples {

    static final String LIST = """
            {
              "headers": {
                "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "statusCode": 200,
                "code": "OK",
                "description": "OK"
              },
              "timestamp": "2026-09-24T12:00:00Z",
              "data": [
                {
                  "productionOrderId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                  "orderNumber": "OP-42",
                  "clientId": "client-seed-001",
                  "sellerId": "seller-seed-001",
                  "workName": "Brochure corporativo",
                  "orderDate": "2026-09-15",
                  "estimatedMaterialCost": 1200000.00,
                  "estimatedMachineCost": 350000.00,
                  "estimatedWasteCost": 48000.00,
                  "estimatedTotalCost": 1598000.00,
                  "actualMaterialCost": 1200000.00,
                  "actualMachineCost": 410000.00,
                  "actualWasteCost": 96000.00,
                  "actualTotalCost": 1706000.00,
                  "quotedPrice": 1650000.00,
                  "estimatedMargin": 52000.00,
                  "actualMargin": -56000.00,
                  "actualMarginPct": -3.39,
                  "estimatedMermaCost": 48000.00,
                  "actualMermaCost": 52000.00,
                  "actualDesperdicioCost": 44000.00
                }
              ]
            }
            """;

    private ProfitabilitySwaggerExamples() {
    }
}
