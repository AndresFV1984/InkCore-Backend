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
                "customerOrderId": null,
                "odpNumber": null,
                "version": 0,
                "clientId": "client-seed-001",
                "workName": "Brochure corporativo",
                "sellerId": null,
                "orderDate": "2026-08-15",
                "requestedQuantity": 1000,
                "totalToCharge": null,
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
                "customerOrderId": "c0a80163-7b2e-4f1a-9c3d-2e5f6a7b8c9d",
                "odpNumber": "ODP-7",
                "version": 5,
                "clientId": "client-seed-001",
                "workName": "Flyer",
                "requestedQuantity": 1000,
                "totalToCharge": 1500000.00,
                "cantidadDisponible": 500,
                "cuttingCompletedAt": "2026-08-15T17:10:00",
                "printingCompletedAt": "2026-08-15T17:20:00",
                "finishedProductsCompletedAt": "2026-08-15T17:30:00",
                "finishingProcessesCompletedAt": "2026-08-15T17:40:00",
                "status": "IN_PROGRESS",
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
                    "paperId": "paper-seed-001",
                    "supplierId": "supplier-seed-001",
                    "paperName": "Bond 75g",
                    "paperSize": "70x100 cm",
                    "sheetValue": 1500.00,
                    "packageUnit": 500,
                    "isCoated": false,
                    "freightPerSheetSnapshot": 0.00,
                    "priceDateSnapshot": "2026-08-04",
                    "priceRule": "PREFERRED",
                    "piecesPerSheetSnapshot": 4,
                    "netSheets": 250.00,
                    "wasteSheets": 5.00,
                    "totalSheets": 255.00,
                    "costPerPiece": 375.00,
                    "isCoatedSnapshot": false,
                    "cutLayoutId": "cut-layout-seed-001",
                    "cutLayoutName": "2x2",
                    "cutLayoutSize": "35x50 cm",
                    "piecesPerSheet": 4,
                    "cutValue": 50.00,
                    "clientSuppliesPaper": false,
                    "isPaperCut": false,
                    "calculatedSheetsCount": 255,
                    "totalPaperValue": 382500.00,
                    "totalCutValue": 0.00,
                    "plannedWastePercentage": 2.00
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
                    "customerOrderId": "c0a80163-7b2e-4f1a-9c3d-2e5f6a7b8c9d",
                    "odpNumber": "ODP-7",
                    "version": 1,
                    "clientId": "client-seed-001",
                    "workName": "Brochure corporativo",
                    "orderDate": "2026-08-15",
                    "requestedQuantity": 1000,
                    "totalToCharge": 1500000.00,
                    "cantidadDisponible": 500,
                    "status": "IN_PROGRESS",
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
              "operators": [
                { "stage": "PREPRESS", "userId": "11111111-1111-1111-1111-111111111111" },
                { "stage": "PRINTING", "userId": "22222222-2222-2222-2222-222222222222" }
              ]
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
              "machineUsages": [
                {
                  "machineId": "814ad646-c4fe-42fa-9f13-4a44823e6bee",
                  "estimatedSetupMinutes": 20,
                  "estimatedRunMinutes": 40
                }
              ],
              "plannedWastePercentage": 3.00,
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
              "plannedMakereadyQuantity": 400.00,
              "completed": true,
              "discountType": "%",
              "discountValue": 0,
              "operatorUserId": null,
              "machineUsages": [
                {
                  "machineId": "814ad646-c4fe-42fa-9f13-4a44823e6bee",
                  "estimatedSetupMinutes": 10,
                  "estimatedRunMinutes": 30
                }
              ],
              "paperRows": [
                {
                  "plateId": "plate-of-order-001",
                  "cutRowKey": "cut-1",
                  "isMissingSupply": false,
                  "clientSuppliesPaper": false,
                  "paperId": "paper-seed-001",
                  "supplierId": "supplier-seed-001",
                  "cutLayoutId": "cut-layout-seed-001",
                  "paperCutLayoutId": "paper-cut-layout-001",
                  "priceRule": "PREFERRED",
                  "plannedWastePercentage": 2.00
                }
              ]
            }
            """;

    static final String PRINTING_BODY = """
            {
              "version": 3,
              "completed": true,
              "operatorUserId": null,
              "plannedOperationalWastePercentage": 3.00,
              "plannedMakereadyQuantity": 400.00,
              "machineUsages": [
                {
                  "machineId": "814ad646-c4fe-42fa-9f13-4a44823e6bee",
                  "estimatedSetupMinutes": 15,
                  "estimatedRunMinutes": 120
                }
              ],
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
              "machineUsages": [
                {
                  "machineId": "814ad646-c4fe-42fa-9f13-4a44823e6bee",
                  "estimatedSetupMinutes": 5,
                  "estimatedRunMinutes": 25
                }
              ],
              "plannedWastePercentage": 3.00,
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

    static final String PREPRESS_OK = """
            {
              "headers": {
                "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "statusCode": 200,
                "code": "OK",
                "description": "Success"
              },
              "timestamp": "2026-09-24T12:00:00Z",
              "data": {
                "productionOrderId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "companyId": "company-seed-001",
                "version": 2,
                "workName": "Brochure corporativo",
                "status": "PENDING",
                "state": true,
                "machineUsages": [
                  {
                    "phase": "preprensa",
                    "machineId": "814ad646-c4fe-42fa-9f13-4a44823e6bee",
                    "machineNameSnapshot": "CTP",
                    "costPerHourSnapshot": 60000.00,
                    "estimatedSetupMinutes": 20,
                    "estimatedRunMinutes": 40,
                    "estimatedMachineCost": 60000.00,
                    "actualSetupMinutes": null,
                    "actualRunMinutes": null,
                    "actualMachineCost": null
                  }
                ],
                "wasteRecords": [
                  {
                    "phase": "preprensa",
                    "wasteCategory": "merma_operativa",
                    "wasteOrigin": "exceso",
                    "materialType": "plancha",
                    "plannedQuantity": 0.12,
                    "actualQuantity": null,
                    "unitCostSnapshot": 15000.00,
                    "plannedCost": 1800.00,
                    "actualCost": null,
                    "note": null
                  }
                ],
                "estimatedMachineCost": 60000.00,
                "estimatedWasteCost": 1800.00
              }
            }
            """;

    static final String POSTPRESS_OK = """
            {
              "headers": {
                "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "statusCode": 200,
                "code": "OK",
                "description": "Success"
              },
              "timestamp": "2026-09-24T12:00:00Z",
              "data": {
                "productionOrderId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "companyId": "company-seed-001",
                "version": 5,
                "workName": "Brochure corporativo",
                "status": "PENDING",
                "state": true,
                "machineUsages": [
                  {
                    "phase": "terminados",
                    "machineId": "814ad646-c4fe-42fa-9f13-4a44823e6bee",
                    "machineNameSnapshot": "Guillotina",
                    "costPerHourSnapshot": 45000.00,
                    "estimatedSetupMinutes": 5,
                    "estimatedRunMinutes": 25,
                    "estimatedMachineCost": 22500.00,
                    "actualSetupMinutes": null,
                    "actualRunMinutes": null,
                    "actualMachineCost": null
                  }
                ],
                "wasteRecords": [
                  {
                    "phase": "terminados",
                    "wasteCategory": "merma_operativa",
                    "wasteOrigin": "exceso",
                    "materialType": "acabado",
                    "plannedQuantity": 7.50,
                    "actualQuantity": null,
                    "unitCostSnapshot": 400.00,
                    "plannedCost": 3000.00,
                    "actualCost": null,
                    "note": null
                  }
                ],
                "estimatedMachineCost": 22500.00,
                "estimatedWasteCost": 3000.00
              }
            }
            """;

    static final String FINISHING_OK = """
            {
              "headers": {
                "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "statusCode": 200,
                "code": "OK",
                "description": "Success"
              },
              "timestamp": "2026-09-24T12:00:00Z",
              "data": {
                "productionOrderId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "companyId": "company-seed-001",
                "version": 6,
                "workName": "Brochure corporativo",
                "status": "PENDING",
                "state": true,
                "machineUsages": [
                  {
                    "phase": "acabados",
                    "machineId": "814ad646-c4fe-42fa-9f13-4a44823e6bee",
                    "machineNameSnapshot": "Barnizadora",
                    "costPerHourSnapshot": 45000.00,
                    "estimatedSetupMinutes": 5,
                    "estimatedRunMinutes": 25,
                    "estimatedMachineCost": 22500.00,
                    "actualSetupMinutes": null,
                    "actualRunMinutes": null,
                    "actualMachineCost": null
                  }
                ],
                "wasteRecords": [
                  {
                    "phase": "acabados",
                    "wasteCategory": "merma_operativa",
                    "wasteOrigin": "exceso",
                    "materialType": "acabado",
                    "plannedQuantity": 7.50,
                    "actualQuantity": null,
                    "unitCostSnapshot": 400.00,
                    "plannedCost": 3000.00,
                    "actualCost": null,
                    "note": null
                  }
                ],
                "estimatedMachineCost": 22500.00,
                "estimatedWasteCost": 3000.00
              }
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

    static final String COST_SUMMARY = """
            {
              "headers": {
                "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "statusCode": 200,
                "code": "OK",
                "description": "OK"
              },
              "timestamp": "2026-09-24T12:00:00Z",
              "data": {
                "productionOrderId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "companyId": "company-seed-001",
                "estimatedMaterialCost": 1200000.00,
                "estimatedMachineCost": 350000.00,
                "estimatedWasteCost": 48000.00,
                "estimatedMermaCost": 48000.00,
                "estimatedTotalCost": 1598000.00,
                "actualMaterialCost": 1200000.00,
                "actualMachineCost": 410000.00,
                "actualWasteCost": 96000.00,
                "actualMermaCost": 52000.00,
                "actualDesperdicioCost": 44000.00,
                "actualTotalCost": 1706000.00,
                "quotedPrice": 1650000.00,
                "estimatedMargin": 52000.00,
                "actualMargin": -56000.00,
                "actualMarginPct": -3.39,
                "updatedAt": "2026-09-24T12:00:00",
                "desperdicios": [
                  {
                    "phase": "impresion",
                    "wasteOrigin": "exceso",
                    "actualQuantity": 12.00,
                    "actualCost": 44000.00,
                    "note": "Defecto de impresión"
                  }
                ]
              }
            }
            """;

    static final String STATUS_BODY = """
            {
              "version": 6,
              "status": "ANULADA",
              "state": true
            }
            """;

    static final String STATUS_BODY_IN_PROGRESS = """
            {
              "version": 6,
              "status": "IN_PROGRESS",
              "state": true
            }
            """;

    static final String STATUS_BODY_COMPAT_CANCELLED = """
            {
              "version": 6,
              "status": "CANCELLED",
              "state": true
            }
            """;

    /** Respuesta de update-status al pasar a progreso: crea customer_orders. */
    static final String SUCCESS_STATUS_IN_PROGRESS = """
            {
              "headers": {
                "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "statusCode": 200,
                "code": "OK",
                "description": "Success"
              },
              "timestamp": "2026-09-09T22:00:00Z",
              "data": {
                "productionOrderId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "companyId": "company-seed-001",
                "orderNumber": "OP-42",
                "customerOrderId": "c0a80163-7b2e-4f1a-9c3d-2e5f6a7b8c9d",
                "odpNumber": "ODP-7",
                "version": 7,
                "clientId": "client-seed-001",
                "workName": "Brochure corporativo",
                "orderDate": "2026-08-15",
                "requestedQuantity": 1000,
                "cantidadDisponible": 0,
                "status": "IN_PROGRESS",
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

    /** Respuesta de update-status cuando la OP queda anulada (siempre status=ANULADA). */
    static final String SUCCESS_STATUS_ANULADA = """
            {
              "headers": {
                "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "statusCode": 200,
                "code": "OK",
                "description": "Success"
              },
              "timestamp": "2026-09-09T22:00:00Z",
              "data": {
                "productionOrderId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "companyId": "company-seed-001",
                "orderNumber": "OP-42",
                "customerOrderId": null,
                "odpNumber": null,
                "version": 7,
                "clientId": "client-seed-001",
                "workName": "Brochure corporativo",
                "orderDate": "2026-08-15",
                "requestedQuantity": 1000,
                "cantidadDisponible": 500,
                "status": "ANULADA",
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

    static final String REPRINT_BODY = """
            {
              "phase": "impresion",
              "quantity": 50.00,
              "wasteReason": "defecto_impresion",
              "machineId": "814ad646-c4fe-42fa-9f13-4a44823e6bee"
            }
            """;

    static final String REPRINT_CREATED = """
            {
              "headers": {
                "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "statusCode": 201,
                "code": "CREATED",
                "description": "Retrabajo registrado"
              },
              "timestamp": "2026-09-25T17:00:00Z",
              "data": {
                "id": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
                "phase": "impresion",
                "wasteCategory": "desperdicio",
                "wasteOrigin": "retrabajo",
                "materialType": "papel",
                "plannedQuantity": 0.00,
                "actualQuantity": 50.00,
                "unitCostSnapshot": 3666.67,
                "plannedCost": 0.00,
                "actualCost": 183333.50,
                "note": "Defecto de impresión"
              }
            }
            """;

    private ProductionOrderSwaggerExamples() {
    }
}
