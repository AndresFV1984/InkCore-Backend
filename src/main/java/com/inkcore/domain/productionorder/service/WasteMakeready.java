package com.inkcore.domain.productionorder.service;

import com.inkcore.domain.productionorder.model.PaperRow;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.productionorder.model.WasteRecord;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Los pliegos fijos usados en Corte o Impresión no tienen columna propia.
 * Se guardan en {@code note} de la merma planificada con el prefijo
 * {@code arranque:} y se quitan de la nota visible al reabrir la orden.
 */
public final class WasteMakeready {

    public static final String ORIGIN_EXCESO = "exceso";
    public static final String ORIGIN_RETRABAJO = "retrabajo";

    private static final String PREFIX = "arranque:";
    private static final String PHASE_CUTTING = "corte-papel";
    private static final String PHASE_PRINTING = "impresion";
    private static final String MERMA_CORTE = "merma_corte";
    private static final String MERMA_OPERATIVA = "merma_operativa";

    private WasteMakeready() {
    }

    public static String note(BigDecimal quantity) {
        return PREFIX + WasteValuation.money(quantity).toPlainString();
    }

    public static BigDecimal read(String note) {
        if (note == null || !note.startsWith(PREFIX)) {
            return null;
        }
        String raw = note.substring(PREFIX.length());
        int line = raw.indexOf('\n');
        if (line >= 0) {
            raw = raw.substring(0, line);
        }
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** Conserva el marcador de arranque y deja el motivo del cierre en la misma nota. */
    public static String attachReason(String note, String reason) {
        if (reason == null || reason.isBlank()) {
            return note;
        }
        String label = reason.trim();
        BigDecimal fixed = read(note);
        if (fixed != null) {
            return note(fixed) + "\nmotivo:" + label;
        }
        return label;
    }

    /** Motivo visible. El marcador de arranque no se muestra. */
    public static String visibleReason(String note) {
        if (note == null || note.isBlank()) {
            return null;
        }
        String marker = "\nmotivo:";
        int index = note.indexOf(marker);
        if (index >= 0) {
            String value = note.substring(index + marker.length()).trim();
            return value.isEmpty() ? null : value;
        }
        if (note.startsWith(PREFIX)) {
            return null;
        }
        return note.trim();
    }

    public static void restore(ProductionOrder order) {
        BigDecimal cut = WasteValuation.money(BigDecimal.ZERO);
        BigDecimal printing = WasteValuation.money(BigDecimal.ZERO);
        if (order.getWasteRecords() != null) {
            for (WasteRecord record : order.getWasteRecords()) {
                BigDecimal fixed = read(record.getNote());
                if (fixed == null) {
                    continue;
                }
                if (PHASE_CUTTING.equals(record.getPhase()) && MERMA_CORTE.equals(record.getWasteCategory())) {
                    cut = fixed;
                }
                if (PHASE_PRINTING.equals(record.getPhase()) && MERMA_OPERATIVA.equals(record.getWasteCategory())) {
                    printing = fixed;
                }
                record.setNote(visibleReason(record.getNote()));
            }
        }
        order.setPlannedCutMakereadyQuantity(cut);
        order.setPlannedOperationalMakereadyQuantity(printing);
        if (order.getPlannedOperationalWastePercentage() != null) {
            return;
        }
        if (order.getWasteRecords() == null) {
            return;
        }
        for (WasteRecord record : order.getWasteRecords()) {
            if (!PHASE_PRINTING.equals(record.getPhase()) || !MERMA_OPERATIVA.equals(record.getWasteCategory())) {
                continue;
            }
            int sheets = totalSheets(order);
            if (sheets <= 0 || record.getPlannedQuantity() == null) {
                return;
            }
            BigDecimal variable = record.getPlannedQuantity().subtract(printing);
            if (variable.signum() < 0) {
                variable = BigDecimal.ZERO;
            }
            order.setPlannedOperationalWastePercentage(variable
                    .multiply(new BigDecimal("100"))
                    .divide(BigDecimal.valueOf(sheets), 2, RoundingMode.HALF_UP));
            return;
        }
    }

    private static int totalSheets(ProductionOrder order) {
        int sheets = 0;
        if (order.getPaperRows() == null) {
            return 0;
        }
        for (PaperRow row : order.getPaperRows()) {
            if (row.getCalculatedSheetsCount() != null && row.getCalculatedSheetsCount() > 0) {
                sheets += row.getCalculatedSheetsCount();
            }
        }
        return sheets;
    }
}
