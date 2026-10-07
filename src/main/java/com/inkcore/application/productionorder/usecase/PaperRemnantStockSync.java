package com.inkcore.application.productionorder.usecase;

import com.inkcore.domain.paper.model.PaperRemnant;
import com.inkcore.domain.paper.ports.out.PaperRemnantRepositoryPort;
import com.inkcore.domain.productionorder.exception.ProductionOrderBusinessRuleException;
import com.inkcore.domain.productionorder.model.PaperRow;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Sincroniza {@code paper_remnants.quantity_available} con el uso en filas de corte.
 */
@Component
public class PaperRemnantStockSync {

    private final PaperRemnantRepositoryPort remnantRepository;

    public PaperRemnantStockSync(PaperRemnantRepositoryPort remnantRepository) {
        this.remnantRepository = remnantRepository;
    }

    /** Devuelve al stock las cantidades previamente descontadas por las filas. */
    public void releaseUsages(List<PaperRow> rows, LocalDateTime now) {
        Map<String, BigDecimal> byRemnant = aggregateUsages(rows);
        for (Map.Entry<String, BigDecimal> entry : byRemnant.entrySet()) {
            PaperRemnant remnant = requireRemnant(entry.getKey());
            remnantRepository.save(remnant.restore(entry.getValue(), now));
        }
    }

    /**
     * Descuenta del stock las cantidades de las filas nuevas.
     * Valida empresa y que el remanente pertenezca al papel de la fila.
     */
    public void applyUsages(String companyId, List<PaperRow> rows, LocalDateTime now) {
        Map<String, BigDecimal> byRemnant = aggregateUsages(rows);
        if (byRemnant.isEmpty()) {
            return;
        }
        Map<String, String> remnantPaperById = new LinkedHashMap<>();
        for (PaperRow row : rows) {
            String remnantId = blankToNull(row.getPaperRemnantId());
            if (remnantId == null) {
                continue;
            }
            String paperId = blankToNull(row.getPaperId());
            if (paperId == null) {
                throw new ProductionOrderBusinessRuleException(
                        "Regla de negocio incumplida",
                        List.of("paperRemnantId requiere paperId en la misma fila de corte")
                );
            }
            remnantPaperById.putIfAbsent(remnantId, paperId);
        }

        List<String> errors = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : byRemnant.entrySet()) {
            String remnantId = entry.getKey();
            PaperRemnant remnant = requireRemnant(remnantId);
            if (!companyId.equals(remnant.getCompanyId())) {
                throw new ResourceNotFoundException("PAPER_REMNANT_NOT_FOUND", "Remanente no encontrado");
            }
            String expectedPaperId = remnantPaperById.get(remnantId);
            if (expectedPaperId != null && !expectedPaperId.equals(remnant.getPaperId())) {
                errors.add("El remanente " + remnantId + " no pertenece al papel " + expectedPaperId);
                continue;
            }
            try {
                remnantRepository.save(remnant.consume(entry.getValue(), now));
            } catch (IllegalArgumentException ex) {
                errors.add(ex.getMessage());
            }
        }
        if (!errors.isEmpty()) {
            throw new ProductionOrderBusinessRuleException("Regla de negocio incumplida", errors);
        }
    }

    /** Tras anular: libera stock y limpia {@code remnantQuantityUsed} (evita doble devolución). */
    public void releaseUsagesOnCancel(List<PaperRow> rows, LocalDateTime now) {
        releaseUsages(rows, now);
        for (PaperRow row : rows) {
            if (row.getRemnantQuantityUsed() != null
                    && row.getRemnantQuantityUsed().compareTo(BigDecimal.ZERO) > 0) {
                row.setRemnantQuantityUsed(null);
            }
        }
    }

    private static Map<String, BigDecimal> aggregateUsages(List<PaperRow> rows) {
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        if (rows == null) {
            return map;
        }
        for (PaperRow row : rows) {
            String remnantId = blankToNull(row.getPaperRemnantId());
            BigDecimal qty = row.getRemnantQuantityUsed();
            if (remnantId == null || qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            map.merge(remnantId, qty, BigDecimal::add);
        }
        return map;
    }

    private PaperRemnant requireRemnant(String paperRemnantId) {
        return remnantRepository.findById(paperRemnantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "PAPER_REMNANT_NOT_FOUND",
                        "Remanente no encontrado"
                ));
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
