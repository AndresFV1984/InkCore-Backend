package com.inkcore.infrastructure.in.rest.wastesettings;

final class WasteSettingsSwaggerExamples {

    static final String UPDATE_BODY = """
            {
              "cutWasteMinPercentage": 2.00,
              "cutWasteMaxPercentage": 5.00,
              "cutWasteDefaultPercentage": 2.00,
              "operationalWasteMinPercentage": 3.00,
              "operationalWasteMaxPercentage": 8.00,
              "operationalWasteDefaultPercentage": 3.00,
              "prepressWasteMinPercentage": 3.00,
              "prepressWasteMaxPercentage": 8.00,
              "prepressWasteDefaultPercentage": 3.00,
              "finishedWasteMinPercentage": 3.00,
              "finishedWasteMaxPercentage": 8.00,
              "finishedWasteDefaultPercentage": 3.00,
              "finishingWasteMinPercentage": 3.00,
              "finishingWasteMaxPercentage": 8.00,
              "finishingWasteDefaultPercentage": 3.00,
              "cutMakereadySheets": 0.00,
              "operationalMakereadySheets": 0.00
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
              "timestamp": "2026-09-24T12:00:00Z",
              "data": {
                "companyId": "company-seed-001",
                "cutWasteMinPercentage": 2.00,
                "cutWasteMaxPercentage": 5.00,
                "cutWasteDefaultPercentage": 2.00,
                "operationalWasteMinPercentage": 3.00,
                "operationalWasteMaxPercentage": 8.00,
                "operationalWasteDefaultPercentage": 3.00,
                "prepressWasteMinPercentage": 3.00,
                "prepressWasteMaxPercentage": 8.00,
                "prepressWasteDefaultPercentage": 3.00,
                "finishedWasteMinPercentage": 3.00,
                "finishedWasteMaxPercentage": 8.00,
                "finishedWasteDefaultPercentage": 3.00,
                "finishingWasteMinPercentage": 3.00,
                "finishingWasteMaxPercentage": 8.00,
                "finishingWasteDefaultPercentage": 3.00,
                "cutMakereadySheets": 0.00,
                "operationalMakereadySheets": 0.00
              }
            }
            """;

    private WasteSettingsSwaggerExamples() {
    }
}
