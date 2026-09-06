package com.inkcore.domain.station.service;

import com.inkcore.domain.productionorder.model.PostpressLine;
import com.inkcore.domain.productionorder.model.PostpressRecord;
import com.inkcore.domain.productionorder.model.PostpressType;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.station.model.StationCatalogItemKind;
import com.inkcore.domain.station.model.StationPhase;

import java.util.Locale;
import java.util.Optional;

/**
 * Resuelve y valida {@code processKey} contra la OP cargada.
 */
public final class StationProcessKeyResolver {

    public static final String JORNADA_KEY = "jornada";

    private StationProcessKeyResolver() {
    }

    public record ResolvedProcessKey(
            String processKey,
            String phase,
            StationCatalogItemKind catalogItemKind,
            String catalogItemId,
            String catalogItemLabel,
            int totalUnits
    ) {
    }

    public static ResolvedProcessKey resolve(
            ProductionOrder order,
            String phaseValue,
            String processKeyValue
    ) {
        if (processKeyValue == null || processKeyValue.isBlank()) {
            throw new IllegalArgumentException("processKey es obligatorio");
        }
        String processKey = processKeyValue.trim();
        if (JORNADA_KEY.equalsIgnoreCase(processKey)) {
            return new ResolvedProcessKey(
                    JORNADA_KEY,
                    StationPhase.JORNADA.getApiValue(),
                    null,
                    null,
                    null,
                    0
            );
        }

        StationPhase phase = StationPhase.fromApiValue(phaseValue);
        String lowerKey = processKey.toLowerCase(Locale.ROOT);

        if (lowerKey.startsWith("terminado:") || lowerKey.startsWith("acabado:")) {
            return resolveCatalogItem(order, phase, processKey, lowerKey);
        }

        if (phase == StationPhase.TERMINADOS || phase == StationPhase.ACABADOS) {
            boolean hasPostpressItems = order.getPostpressRecords().stream()
                    .anyMatch(record -> matchesPhase(record, phase));
            if (hasPostpressItems && processKey.equals(phase.getApiValue())) {
                throw new IllegalArgumentException(
                        "Debe usar processKey terminado:{id} o acabado:{id} cuando la OP tiene ítems de catálogo"
                );
            }
        }

        return new ResolvedProcessKey(
                processKey,
                phase.getApiValue(),
                null,
                null,
                null,
                order.getRequestedQuantity()
        );
    }

    private static ResolvedProcessKey resolveCatalogItem(
            ProductionOrder order,
            StationPhase phase,
            String processKey,
            String lowerKey
    ) {
        String suffix = lowerKey.contains(":") ? processKey.substring(processKey.indexOf(':') + 1).trim() : "";
        if (suffix.isBlank()) {
            throw new IllegalArgumentException("processKey de catálogo inválido: " + processKey);
        }

        StationCatalogItemKind kind = lowerKey.startsWith("terminado:")
                ? StationCatalogItemKind.TERMINADO
                : StationCatalogItemKind.ACABADO;

        PostpressType expectedType = kind == StationCatalogItemKind.TERMINADO
                ? PostpressType.FINISHED_PRODUCT
                : PostpressType.FINISHING_PROCESS;

        Optional<PostpressRecord> byRecord = order.getPostpressRecords().stream()
                .filter(record -> expectedType == record.getType())
                .filter(record -> suffix.equals(record.getRecordId()))
                .findFirst();

        if (byRecord.isPresent()) {
            PostpressRecord record = byRecord.get();
            PostpressLine line = record.getLines().isEmpty() ? null : record.getLines().get(0);
            // Cupo de estación por ítem = cantidad de la OP (independiente entre ítems).
            // No usar line.goodSizes aquí: ese valor sigue siendo solo para costeo
            // (ProductionOrderCalculator.calculatePostpressPrice en create/update de OP).
            int totalUnits = order.getRequestedQuantity();
            String catalogItemId = line != null ? line.getCatalogItemId() : record.getRecordId();
            String label = line != null ? line.getItemName() : record.getRecordId();
            return new ResolvedProcessKey(processKey, phase.getApiValue(), kind, catalogItemId, label, totalUnits);
        }

        for (PostpressRecord record : order.getPostpressRecords()) {
            if (expectedType != record.getType()) {
                continue;
            }
            for (PostpressLine line : record.getLines()) {
                if (suffix.equals(line.getCatalogItemId()) || suffix.equals(line.getLineId())) {
                    return new ResolvedProcessKey(
                            processKey,
                            phase.getApiValue(),
                            kind,
                            line.getCatalogItemId(),
                            line.getItemName(),
                            order.getRequestedQuantity()
                    );
                }
            }
        }

        throw new IllegalArgumentException("processKey no existe en postpressRecords de la OP: " + processKey);
    }

    private static boolean matchesPhase(PostpressRecord record, StationPhase phase) {
        if (phase == StationPhase.TERMINADOS) {
            return record.getType() == PostpressType.FINISHED_PRODUCT;
        }
        if (phase == StationPhase.ACABADOS) {
            return record.getType() == PostpressType.FINISHING_PROCESS;
        }
        return false;
    }
}
