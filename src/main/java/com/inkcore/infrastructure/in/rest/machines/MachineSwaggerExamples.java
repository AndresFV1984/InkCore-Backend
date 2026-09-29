package com.inkcore.infrastructure.in.rest.machines;

final class MachineSwaggerExamples {

    static final String MACHINE_ID = "814ad646-c4fe-42fa-9f13-4a44823e6bee";

    static final String REGISTER_BODY = """
            {
              "name": "Offset Heidelberg 4 colores",
              "machineType": "impresion",
              "manufacturer": "Heidelberg",
              "model": "SM 74",
              "purchaseCost": 250000000.00,
              "usefulLifeYears": 10.00,
              "annualMaintenanceCost": 8000000.00,
              "monthlyOperatorCost": 2500000.00,
              "energyCostPerHour": 15000.00,
              "productiveHoursPerYear": 1600.00,
              "state": true
            }
            """;

    static final String UPDATE_BODY = """
            {
              "name": "Offset Heidelberg 4 colores",
              "machineType": "impresion",
              "manufacturer": "Heidelberg",
              "model": "SM 74",
              "purchaseCost": 250000000.00,
              "usefulLifeYears": 10.00,
              "annualMaintenanceCost": 8500000.00,
              "monthlyOperatorCost": 2500000.00,
              "energyCostPerHour": 15000.00,
              "productiveHoursPerYear": 1600.00,
              "state": true
            }
            """;

    static final String CREATED = """
            {
              "headers": {
                "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "statusCode": 201,
                "code": "CREATED",
                "description": "Machine created"
              },
              "timestamp": "2026-09-24T12:00:00Z",
              "data": {
                "machineId": "814ad646-c4fe-42fa-9f13-4a44823e6bee",
                "companyId": "company-seed-001",
                "name": "Offset Heidelberg 4 colores",
                "machineType": "impresion",
                "manufacturer": "Heidelberg",
                "model": "SM 74",
                "purchaseCost": 250000000.00,
                "usefulLifeYears": 10.00,
                "annualMaintenanceCost": 8000000.00,
                "monthlyOperatorCost": 2500000.00,
                "energyCostPerHour": 15000.00,
                "productiveHoursPerYear": 1600.00,
                "costPerHour": 54375.00,
                "state": true,
                "creationDate": "2026-09-24",
                "updatedAt": "2026-09-24T12:00:00"
              }
            }
            """;

    static final String OK = """
            {
              "headers": {
                "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "statusCode": 200,
                "code": "OK",
                "description": "OK"
              },
              "timestamp": "2026-09-24T12:30:00Z",
              "data": {
                "machineId": "814ad646-c4fe-42fa-9f13-4a44823e6bee",
                "companyId": "company-seed-001",
                "name": "Offset Heidelberg 4 colores",
                "machineType": "impresion",
                "manufacturer": "Heidelberg",
                "model": "SM 74",
                "purchaseCost": 250000000.00,
                "usefulLifeYears": 10.00,
                "annualMaintenanceCost": 8500000.00,
                "monthlyOperatorCost": 2500000.00,
                "energyCostPerHour": 15000.00,
                "productiveHoursPerYear": 1600.00,
                "costPerHour": 54687.50,
                "state": true,
                "creationDate": "2026-09-24",
                "updatedAt": "2026-09-24T12:30:00"
              }
            }
            """;

    static final String LIST = """
            {
              "headers": {
                "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "statusCode": 200,
                "code": "OK",
                "description": "OK"
              },
              "timestamp": "2026-09-24T12:00:00Z",
              "data": {
                "content": [
                  {
                    "machineId": "814ad646-c4fe-42fa-9f13-4a44823e6bee",
                    "companyId": "company-seed-001",
                    "name": "Offset Heidelberg 4 colores",
                    "machineType": "impresion",
                    "manufacturer": "Heidelberg",
                    "model": "SM 74",
                    "purchaseCost": 250000000.00,
                    "usefulLifeYears": 10.00,
                    "annualMaintenanceCost": 8000000.00,
                    "monthlyOperatorCost": 2500000.00,
                    "energyCostPerHour": 15000.00,
                    "productiveHoursPerYear": 1600.00,
                    "costPerHour": 54375.00,
                    "state": true,
                    "creationDate": "2026-09-24",
                    "updatedAt": "2026-09-24T12:00:00"
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

    static final String RECALCULATED = """
            {
              "headers": {
                "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "statusCode": 200,
                "code": "OK",
                "description": "OK"
              },
              "timestamp": "2026-09-24T03:00:00Z",
              "data": {
                "machinesSnapshotted": 1,
                "machines": [
                  {
                    "machineId": "814ad646-c4fe-42fa-9f13-4a44823e6bee",
                    "companyId": "company-seed-001",
                    "name": "Offset Heidelberg 4 colores",
                    "machineType": "impresion",
                    "costPerHour": 54375.00,
                    "state": true
                  }
                ]
              }
            }
            """;

    private MachineSwaggerExamples() {
    }
}
