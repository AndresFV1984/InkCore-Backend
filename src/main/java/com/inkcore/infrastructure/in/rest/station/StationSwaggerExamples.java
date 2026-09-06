package com.inkcore.infrastructure.in.rest.station;

final class StationSwaggerExamples {

    private StationSwaggerExamples() {
    }

    static final String PHASE_START_REQUEST = """
            {
              "productionOrderId": "ef658d09-bf30-43de-ba7a-edd0f14a61bd",
              "workName": "cuaderno",
              "phase": "preprensa",
              "processKey": "preprensa",
              "userId": "operator-seed-003",
              "productionStatus": "En Proceso",
              "occurredAt": "2026-09-02T11:14:39"
            }
            """;

    static final String PHASE_END_REQUEST = """
            {
              "productionOrderId": "ef658d09-bf30-43de-ba7a-edd0f14a61bd",
              "workName": "cuaderno",
              "phase": "preprensa",
              "processKey": "preprensa",
              "userId": "operator-seed-003",
              "productionStatus": "En Proceso",
              "occurredAt": "2026-09-02T18:00:00"
            }
            """;

    static final String ADVANCE_REQUEST = """
            {
              "productionOrderId": "ef658d09-bf30-43de-ba7a-edd0f14a61bd",
              "workName": "cuaderno",
              "phase": "preprensa",
              "processKey": "preprensa",
              "userId": "operator-seed-003",
              "units": 250,
              "productionStatus": "En Proceso",
              "note": "Lote 1",
              "occurredAt": "2026-09-02T15:22:10"
            }
            """;

    static final String PAUSE_REQUEST = """
            {
              "productionOrderId": "ef658d09-bf30-43de-ba7a-edd0f14a61bd",
              "workName": "cuaderno",
              "phase": "preprensa",
              "processKey": "preprensa",
              "userId": "operator-seed-003",
              "pauseReason": "problema_maquina",
              "productionStatus": "En Proceso",
              "note": "Atasco en unidad 2",
              "occurredAt": "2026-09-02T17:01:30"
            }
            """;

    static final String RESUME_REQUEST = """
            {
              "productionOrderId": "ef658d09-bf30-43de-ba7a-edd0f14a61bd",
              "workName": "cuaderno",
              "phase": "preprensa",
              "processKey": "preprensa",
              "userId": "operator-seed-003",
              "productionStatus": "En Proceso",
              "occurredAt": "2026-09-02T17:20:00"
            }
            """;

    static final String DELIVERY_PARTIAL_REQUEST = """
            {
              "productionOrderId": "ef658d09-bf30-43de-ba7a-edd0f14a61bd",
              "workName": "cuaderno",
              "phase": "terminados",
              "processKey": "terminado:catalog-item-001",
              "userId": "operator-seed-003",
              "units": 100,
              "productionStatus": "En Proceso",
              "occurredAt": "2026-09-02T16:00:00"
            }
            """;

    static final String DELIVERY_TOTAL_REQUEST = """
            {
              "productionOrderId": "ef658d09-bf30-43de-ba7a-edd0f14a61bd",
              "workName": "cuaderno",
              "phase": "terminados",
              "processKey": "terminado:catalog-item-001",
              "userId": "operator-seed-003",
              "units": 150,
              "productionStatus": "En Proceso",
              "occurredAt": "2026-09-02T16:30:00"
            }
            """;

    static final String SHIFT_MARK_REQUEST = """
            {
              "reason": "inicio_horario",
              "phase": "jornada",
              "processKey": "jornada",
              "isShiftEvent": true,
              "occurredAt": "2026-09-02T07:00:00"
            }
            """;

    static final String EVENT_CREATED = """
            {
              "headers": {
                "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "statusCode": 201,
                "code": "CREATED",
                "description": "Station event created"
              },
              "timestamp": "2026-09-02T17:01:30Z",
              "data": {
                "id": "d0a4d094-0f70-48f7-bafc-47f91f7c1eaa",
                "productionOrderId": "ef658d09-bf30-43de-ba7a-edd0f14a61bd",
                "orderId": "ef658d09-bf30-43de-ba7a-edd0f14a61bd",
                "workName": "cuaderno",
                "phase": "preprensa",
                "processKey": "preprensa",
                "catalogItemId": null,
                "catalogItemLabel": null,
                "userId": "operator-seed-003",
                "type": "paro",
                "at": "2026-09-02T17:01:30",
                "unidades": null,
                "actorUserId": "operator-seed-003",
                "actorName": "Operario Preprensa Litografía",
                "productionStatus": "En Proceso",
                "pauseReason": "problema_maquina",
                "note": "Atasco en unidad 2"
              }
            }
            """;

    static final String EVENT_LIST = """
            {
              "headers": {
                "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "statusCode": 200,
                "code": "OK",
                "description": "Success"
              },
              "timestamp": "2026-09-02T17:05:00Z",
              "data": {
                "content": [
                  {
                    "id": "59a86564-4487-41a7-8ec9-b04734c01b91",
                    "productionOrderId": "ef658d09-bf30-43de-ba7a-edd0f14a61bd",
                    "orderId": "ef658d09-bf30-43de-ba7a-edd0f14a61bd",
                    "workName": "cuaderno",
                    "phase": "preprensa",
                    "processKey": "preprensa",
                    "userId": "operator-seed-003",
                    "type": "inicio_fase",
                    "at": "2026-09-02T11:14:39",
                    "unidades": null,
                    "actorUserId": "operator-seed-003",
                    "actorName": "Operario Preprensa Litografía",
                    "productionStatus": "IN_PROGRESS",
                    "pauseReason": null,
                    "note": null
                  }
                ],
                "page": 0,
                "size": 500,
                "totalElements": 1,
                "totalPages": 1,
                "hasNext": false
              }
            }
            """;

    static final String INBOX_PAGE = """
            {
              "headers": {
                "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "statusCode": 200,
                "code": "OK",
                "description": "Success"
              },
              "timestamp": "2026-09-01T12:00:00Z",
              "data": {
                "content": [
                  {
                    "productionOrderId": "ef658d09-bf30-43de-ba7a-edd0f14a61bd",
                    "displayNumber": "OP-001",
                    "workName": "cuaderno",
                    "clientId": "client-seed-001",
                    "clientName": "Editorial Horizonte Ltda.",
                    "designName": "Diseño verano",
                    "productionStatus": "En Proceso",
                    "executionAllowed": true,
                    "cantidadDisponible": 1500,
                    "assignedProcessSummary": {
                      "done": 0,
                      "active": 1,
                      "pending": 3,
                      "progressPct": 10
                    }
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

    static final String BITACORA = """
            {
              "headers": {
                "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "statusCode": 200,
                "code": "OK",
                "description": "Success"
              },
              "timestamp": "2026-09-01T12:00:00Z",
              "data": {
                "processKey": "terminado:rec-barniz-001",
                "catalogItemId": "rec-barniz-001",
                "catalogItemLabel": "Barniz UV",
                "laborTimeMs": 7200000,
                "pausedTimeMs": 900000,
                "pauseCount": 2,
                "isPausedNow": false,
                "entries": [
                  {
                    "id": "evt-uuid-1",
                    "type": "avance_unidades",
                    "at": "2026-09-01T10:15:00",
                    "units": 250,
                    "note": null
                  }
                ],
                "pauses": [
                  {
                    "id": "evt-uuid-pausa-1",
                    "reason": "falla_maquina",
                    "at": "2026-09-01T09:40:00",
                    "note": "Cuchilla"
                  }
                ],
                "page": 0,
                "size": 10,
                "totalElements": 42,
                "totalPages": 5,
                "hasNext": true
              }
            }
            """;

    static final String TIMELINE_PAGE = """
            {
              "headers": {
                "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "statusCode": 200,
                "code": "OK",
                "description": "Success"
              },
              "timestamp": "2026-09-02T17:05:00Z",
              "data": {
                "content": [
                  {
                    "id": "evt-uuid-avance-1",
                    "productionOrderId": "po-uuid-001",
                    "orderId": "po-uuid-001",
                    "workName": "Volantes A5",
                    "phase": "terminado",
                    "processKey": "terminado:rec-barniz-001",
                    "catalogItemId": "rec-barniz-001",
                    "catalogItemLabel": "Barniz UV",
                    "userId": "operator-seed-003",
                    "type": "avance_unidades",
                    "at": "2026-09-01T10:15:00",
                    "unidades": 250,
                    "actorUserId": "operator-seed-003",
                    "actorName": "Operario Terminados",
                    "productionStatus": "IN_PROGRESS",
                    "pauseReason": null,
                    "note": null
                  }
                ],
                "page": 0,
                "size": 10,
                "totalElements": 42,
                "totalPages": 5,
                "hasNext": true
              }
            }
            """;

    static final String LABOR_SETTLEMENT = """
            {
              "headers": {
                "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "statusCode": 200,
                "code": "OK",
                "description": "Success"
              },
              "timestamp": "2026-09-01T12:00:00Z",
              "data": {
                "userId": "operator-seed-003",
                "from": "2026-09-01T00:00:00",
                "to": "2026-09-07T23:59:59.999999999",
                "shiftOpen": false,
                "grossShiftMs": 14400000,
                "pausedMs": 3600000,
                "netLaborMs": 10800000,
                "intervals": []
              }
            }
            """;
}
