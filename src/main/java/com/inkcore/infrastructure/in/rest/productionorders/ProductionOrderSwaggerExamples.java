package com.inkcore.infrastructure.in.rest.productionorders;

/**
 * Ejemplos OpenAPI reutilizados por {@link ProductionOrderController}.
 */
final class ProductionOrderSwaggerExamples {

    static final String ORDER_ID = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";

    static final String SUCCESS_CREATED = """
            {
              "headers": {
                "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "statusCode": 201,
                "code": "CREATED",
                "description": "Production order created"
              },
              "timestamp": "2026-08-15T17:00:00Z",
              "data": {
                "productionOrderId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "companyId": "company-seed-001",
                "orderNumber": "OP-42",
                "version": 0,
                "clientId": "client-seed-001",
                "workName": "Brochure corporativo",
                "sellerId": null,
                "orderDate": "2026-08-15",
                "requestedQuantity": 1000,
                "proposalQuantity1": null,
                "proposalQuantity2": null,
                "specificationsCompletedAt": "2026-08-15T17:00:00",
                "status": "PENDING",
                "state": true,
                "plates": [],
                "paperRows": [],
                "prints": [],
                "postpressRecords": [],
                "operators": [],
                "stageDiscounts": []
              }
            }
            """;

    static final String SUCCESS_OK = """
            {
              "headers": {
                "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "statusCode": 200,
                "code": "OK",
                "description": "Success"
              },
              "timestamp": "2026-08-15T17:00:00Z",
              "data": {
                "productionOrderId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "companyId": "company-seed-001",
                "orderNumber": "OP-42",
                "version": 5,
                "clientId": "client-seed-001",
                "workName": "Flyer",
                "requestedQuantity": 1000,
                "cuttingCompletedAt": "2026-08-15T17:10:00",
                "printingCompletedAt": "2026-08-15T17:20:00",
                "finishedProductsCompletedAt": "2026-08-15T17:30:00",
                "finishingProcessesCompletedAt": "2026-08-15T17:40:00",
                "status": "PENDING",
                "state": true,
                "plates": [
                  {
                    "productionOrderPlateId": "plate-srv-1",
                    "plateTypeId": "plate-type-seed-001",
                    "colors": "4 COLORES",
                    "quantity": 1000
                  }
                ],
                "paperRows": [
                  {
                    "productionOrderPaperRowId": "pr-1",
                    "plateId": "plate-srv-1",
                    "cutRowKey": "plate-srv-1",
                    "paperTypeId": "papel-1",
                    "paperName": "Bond",
                    "cutLayoutId": "corte-1",
                    "cutLayoutName": "2x2",
                    "piecesPerSheet": 4,
                    "clientSuppliesPaper": true,
                    "isPaperCut": true,
                    "deliveredSheetsByClient": 100
                  }
                ],
                "prints": [
                  {
                    "productionOrderPrintId": "print-1",
                    "plateId": "plate-srv-1",
                    "completed": true,
                    "entries": [],
                    "inkEstimation": { "entries": [] }
                  }
                ],
                "postpressRecords": [
                  {
                    "productionOrderPostpressRecordId": "pp-term-1",
                    "plateId": "plate-srv-1",
                    "type": "FINISHED_PRODUCT",
                    "completed": true,
                    "lines": []
                  },
                  {
                    "productionOrderPostpressRecordId": "pp-acab-1",
                    "plateId": "plate-srv-1",
                    "type": "FINISHING_PROCESS",
                    "completed": true,
                    "lines": []
                  }
                ],
                "operators": [],
                "stageDiscounts": []
              }
            }
            """;

    static final String SUCCESS_LIST = """
            {
              "headers": {
                "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "statusCode": 200,
                "code": "OK",
                "description": "Success"
              },
              "timestamp": "2026-08-15T17:00:00Z",
              "data": {
                "content": [
                  {
                    "productionOrderId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                    "companyId": "company-seed-001",
                    "orderNumber": "OP-42",
                    "version": 1,
                    "clientId": "client-seed-001",
                    "workName": "Brochure corporativo",
                    "orderDate": "2026-08-15",
                    "requestedQuantity": 1000,
                    "status": "PENDING",
                    "state": true
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

    static final String REGISTER_BODY = """
            {
              "clientId": "client-seed-001",
              "workName": "Brochure corporativo",
              "sellerId": null,
              "orderDate": "2026-08-15",
              "requestedQuantity": 1000,
              "proposalQuantity1": 1500,
              "proposalQuantity2": 2000,
              "operatorUserId": null
            }
            """;

    static final String SPECIFICATIONS_BODY = """
            {
              "version": 0,
              "clientId": "client-seed-001",
              "workName": "Brochure corporativo actualizado",
              "sellerId": "seller-seed-001",
              "orderDate": "2026-08-15",
              "requestedQuantity": 1200,
              "proposalQuantity1": 1500,
              "proposalQuantity2": 2000,
              "operatorUserId": null
            }
            """;

    static final String PREPRESS_BODY = """
            {
              "version": 1,
              "isNewDesign": true,
              "designName": "Diseño brochure 2026",
              "existingDesignOrderId": null,
              "hasDesignCost": false,
              "designCost": null,
              "clientSuppliesPlates": false,
              "clientPlateType": null,
              "assemblyPriceId": null,
              "dieCutLine": false,
              "uvReserve": false,
              "stamping": false,
              "embossing": false,
              "prepressDiscountType": "%",
              "prepressDiscountValue": 0,
              "completed": true,
              "operatorUserId": null,
              "plates": [
                {
                  "colors": "4 COLORES",
                  "plateTypeId": "plate-type-seed-001",
                  "quantity": 1000,
                  "cavities": 4,
                  "platesCount": 4,
                  "plateReplacement": false
                }
              ]
            }
            """;

    static final String PAPER_CUTTING_BODY = """
            {
              "version": 2,
              "clientSuppliesPaperDefault": false,
              "roundingMargin": 2,
              "completed": true,
              "discountType": "%",
              "discountValue": 0,
              "operatorUserId": null,
              "paperRows": [
                {
                  "plateId": "plate-of-order-001",
                  "cutRowKey": "cut-1",
                  "isMissingSupply": false,
                  "clientSuppliesPaper": false,
                  "paperTypeId": "paper-type-seed-001",
                  "cutLayoutId": "cut-layout-seed-001"
                }
              ]
            }
            """;

    static final String PRINTING_BODY = """
            {
              "version": 3,
              "completed": true,
              "operatorUserId": null,
              "prints": [
                {
                  "plateId": "plate-of-order-001",
                  "clientSuppliesSherpa": true,
                  "completed": true,
                  "inkEstimation": {
                    "entries": [
                      {
                        "entradaId": "entry-1",
                        "objectKey": "tmp/company/company-seed-001/ink-estimates/user-1/entry-1/original.pdf",
                        "previewObjectKey": "tmp/company/company-seed-001/ink-estimates/user-1/entry-1/preview.jpg",
                        "contentType": "application/pdf",
                        "sizeBytes": 1048576,
                        "fileName": "arte.pdf",
                        "totalGramsOrder": 624.75
                      }
                    ]
                  },
                  "entries": [
                    {
                      "shotsInkCount": 4,
                      "shotsInks": ["C", "M", "Y", "K"],
                      "reverseInkCount": 0,
                      "reverseInks": [],
                      "basicFlipType": "no-flip",
                      "basicThousandRateId": "thousand-rate-seed-001",
                      "pantoneFlipType": "no-flip"
                    }
                  ]
                }
              ]
            }
            """;

    static final String POSTPRESS_BODY = """
            {
              "version": 4,
              "completed": true,
              "discountType": "$",
              "discountValue": 0,
              "operatorUserId": null,
              "records": [
                {
                  "plateId": "plate-of-order-001",
                  "completed": true,
                  "lines": [
                    {
                      "catalogItemId": "finish-seed-001",
                      "source": "catalog",
                      "areaFactor": 1.0,
                      "goodSizes": 250
                    }
                  ]
                }
              ]
            }
            """;

    static final String BILLING_BODY = """
            {
              "version": 5,
              "billingDiscountType": "%",
              "billingDiscountValue": 0,
              "clientCostingMode": "exact",
              "clientDiscountType": "%",
              "clientDiscountValue": 0,
              "clientProfitabilityType": "%",
              "clientProfitabilityValue": 20,
              "advancePercentage": 50,
              "clientSignatureName": "Juan Pérez",
              "bankAccountId": "account-seed-001",
              "deliveryStartDate": "2026-08-20",
              "deliveryEndDate": "2026-08-25",
              "completed": true,
              "operatorUserId": null
            }
            """;

    static final String STATUS_BODY = """
            {
              "version": 6,
              "status": "IN_PROGRESS",
              "state": true
            }
            """;

    private ProductionOrderSwaggerExamples() {
    }
}
