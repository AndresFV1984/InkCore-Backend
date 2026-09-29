package com.inkcore.infrastructure.out.persistence.productionorder.adapter;

import com.inkcore.domain.productionorder.model.CostSummary;
import com.inkcore.domain.productionorder.model.ProfitabilityRow;
import com.inkcore.domain.productionorder.ports.out.CostSummaryRepositoryPort;
import com.inkcore.infrastructure.out.persistence.productionorder.entity.ProductionOrderCostSummaryEntity;
import com.inkcore.infrastructure.out.persistence.productionorder.repository.JpaProductionOrderCostSummaryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class CostSummaryPersistenceAdapter implements CostSummaryRepositoryPort {

    private final JpaProductionOrderCostSummaryRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    public CostSummaryPersistenceAdapter(JpaProductionOrderCostSummaryRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CostSummary> findByProductionOrderId(String companyId, String productionOrderId) {
        return repository.findByProductionOrderIdAndCompanyId(productionOrderId, companyId).map(this::toDomain);
    }

    @Override
    @Transactional
    public void upsertQuotedPrice(String productionOrderId, String companyId, BigDecimal quotedPrice) {
        entityManager.createNativeQuery("""
                        INSERT INTO indicolors.production_order_cost_summary (
                            production_order_id, company_id, quoted_price, updated_at
                        ) VALUES (:productionOrderId, :companyId, :quotedPrice, now())
                        ON CONFLICT (production_order_id) DO UPDATE
                        SET quoted_price = EXCLUDED.quoted_price,
                            updated_at = now()
                        """)
                .setParameter("productionOrderId", productionOrderId)
                .setParameter("companyId", companyId)
                .setParameter("quotedPrice", quotedPrice)
                .executeUpdate();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProfitabilityRow> findProfitability(
            String companyId,
            LocalDate from,
            LocalDate to,
            String clientId,
            String sellerId
    ) {
        StringBuilder sql = new StringBuilder("""
                SELECT po.production_order_id, po.order_number, po.client_id, po.seller_id, po.work_name, po.order_date,
                       cs.estimated_material_cost, cs.estimated_machine_cost, cs.estimated_waste_cost, cs.estimated_total_cost,
                       cs.actual_material_cost, cs.actual_machine_cost, cs.actual_waste_cost, cs.actual_total_cost,
                       cs.quoted_price, cs.estimated_margin, cs.actual_margin, cs.actual_margin_pct,
                       cs.estimated_merma_cost, cs.actual_merma_cost, cs.actual_desperdicio_cost
                FROM indicolors.production_order_cost_summary cs
                JOIN indicolors.production_orders po ON po.production_order_id = cs.production_order_id
                WHERE cs.company_id = :companyId
                  AND po.order_date >= :fromDate
                  AND po.order_date <= :toDate
                """);
        if (clientId != null && !clientId.isBlank()) {
            sql.append(" AND po.client_id = :clientId");
        }
        if (sellerId != null && !sellerId.isBlank()) {
            sql.append(" AND po.seller_id = :sellerId");
        }
        sql.append(" ORDER BY cs.actual_margin ASC NULLS LAST, po.order_date ASC, po.order_number ASC");

        var query = entityManager.createNativeQuery(sql.toString())
                .setParameter("companyId", companyId)
                .setParameter("fromDate", Date.valueOf(from))
                .setParameter("toDate", Date.valueOf(to));
        if (clientId != null && !clientId.isBlank()) {
            query.setParameter("clientId", clientId);
        }
        if (sellerId != null && !sellerId.isBlank()) {
            query.setParameter("sellerId", sellerId);
        }

        List<ProfitabilityRow> rows = new ArrayList<>();
        for (Object raw : query.getResultList()) {
            Object[] columns = (Object[]) raw;
            rows.add(new ProfitabilityRow(
                    string(columns[0]),
                    string(columns[1]),
                    string(columns[2]),
                    string(columns[3]),
                    string(columns[4]),
                    toDate(columns[5]),
                    decimal(columns[6]),
                    decimal(columns[7]),
                    decimal(columns[8]),
                    decimal(columns[9]),
                    decimal(columns[10]),
                    decimal(columns[11]),
                    decimal(columns[12]),
                    decimal(columns[13]),
                    decimal(columns[14]),
                    decimal(columns[15]),
                    decimal(columns[16]),
                    decimal(columns[17]),
                    decimal(columns[18]),
                    decimal(columns[19]),
                    decimal(columns[20])
            ));
        }
        return rows;
    }

    private CostSummary toDomain(ProductionOrderCostSummaryEntity entity) {
        return new CostSummary(
                entity.getProductionOrderId(),
                entity.getCompanyId(),
                entity.getEstimatedMaterialCost(),
                entity.getEstimatedMachineCost(),
                entity.getEstimatedWasteCost(),
                entity.getEstimatedMermaCost(),
                entity.getEstimatedTotalCost(),
                entity.getActualMaterialCost(),
                entity.getActualMachineCost(),
                entity.getActualWasteCost(),
                entity.getActualMermaCost(),
                entity.getActualDesperdicioCost(),
                entity.getActualTotalCost(),
                entity.getQuotedPrice(),
                entity.getEstimatedMargin(),
                entity.getActualMargin(),
                entity.getActualMarginPct(),
                entity.getUpdatedAt()
        );
    }

    private static String string(Object value) {
        return value == null ? null : value.toString();
    }

    private static BigDecimal decimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        return new BigDecimal(value.toString());
    }

    private static LocalDate toDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate date) {
            return date;
        }
        if (value instanceof Date date) {
            return date.toLocalDate();
        }
        if (value instanceof java.util.Date date) {
            return new Date(date.getTime()).toLocalDate();
        }
        return LocalDate.parse(value.toString());
    }
}
