package com.inkcore.application.productionorder.usecase;

import com.inkcore.domain.client.ports.out.ClientRepositoryPort;
import com.inkcore.domain.productionorder.model.PaperRow;
import com.inkcore.domain.productionorder.model.Plate;
import com.inkcore.domain.productionorder.model.PostpressLine;
import com.inkcore.domain.productionorder.model.PostpressRecord;
import com.inkcore.domain.productionorder.model.PrintConfig;
import com.inkcore.domain.productionorder.model.PrintEntry;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.productionorder.ports.out.ProductionOrderPdfPort;
import com.inkcore.domain.productionorder.service.ProductionOrderCalculator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class GenerateProductionOrderBillingPdfUseCase {

    private final ProductionOrderSupport support;
    private final ProductionOrderPdfPort pdfPort;
    private final ClientRepositoryPort clientRepository;

    public GenerateProductionOrderBillingPdfUseCase(
            ProductionOrderSupport support,
            ProductionOrderPdfPort pdfPort,
            ClientRepositoryPort clientRepository
    ) {
        this.support = support;
        this.pdfPort = pdfPort;
        this.clientRepository = clientRepository;
    }

    @Transactional(readOnly = true)
    public byte[] execute(String productionOrderId, Authentication authentication) {
        String companyId = support.companyId(authentication);
        ProductionOrder order = support.requireOrder(productionOrderId, companyId);
        String clientName = clientRepository.findById(order.getClientId())
                .map(c -> c.getName())
                .orElse(order.getClientId());
        return pdfPort.generateBillingPdf(order, clientName, calculateTotal(order));
    }

    static BigDecimal calculateTotal(ProductionOrder order) {
        BigDecimal total = BigDecimal.ZERO;
        if (order.getPrepress() != null && order.getPrepress().getTotalPlatesValue() != null) {
            total = total.add(order.getPrepress().getTotalPlatesValue());
            if (order.getPrepress().getDesignCost() != null) {
                total = total.add(order.getPrepress().getDesignCost());
            }
            if (order.getPrepress().getAssemblyPriceCost() != null) {
                total = total.add(order.getPrepress().getAssemblyPriceCost());
            }
            total = ProductionOrderCalculator.applyDiscount(
                    total,
                    DiscountTypeValue(order.getPrepress().getPrepressDiscountType()),
                    order.getPrepress().getPrepressDiscountValue()
            );
        }
        for (PaperRow row : order.getPaperRows()) {
            total = total.add(ProductionOrderCalculator.nullSafe(row.getTotalPaperValue()));
            total = total.add(ProductionOrderCalculator.nullSafe(row.getTotalCutValue()));
        }
        for (PrintConfig print : order.getPrints()) {
            for (PrintEntry entry : print.getEntries()) {
                total = total.add(ProductionOrderCalculator.nullSafe(entry.getBasicPrintingPrice()));
                total = total.add(ProductionOrderCalculator.nullSafe(entry.getPantonePrintingPrice()));
                total = total.add(ProductionOrderCalculator.nullSafe(entry.getPantoneInkChargePrice()));
            }
            total = total.add(ProductionOrderCalculator.nullSafe(print.getSherpaTestPrice()));
            total = ProductionOrderCalculator.applyDiscount(
                    total,
                    DiscountTypeValue(print.getPrintingDiscountType()),
                    print.getPrintingDiscountValue()
            );
        }
        for (PostpressRecord record : order.getPostpressRecords()) {
            for (PostpressLine line : record.getLines()) {
                total = total.add(ProductionOrderCalculator.nullSafe(line.getChargedPrice()));
            }
        }
        for (Plate ignored : order.getPlates()) {
            // plates already summed via prepress.totalPlatesValue
        }
        if (order.getBilling() != null) {
            total = ProductionOrderCalculator.applyDiscount(
                    total,
                    DiscountTypeValue(order.getBilling().getBillingDiscountType()),
                    order.getBilling().getBillingDiscountValue()
            );
        }
        return ProductionOrderCalculator.money(total);
    }

    private static String DiscountTypeValue(com.inkcore.domain.productionorder.model.DiscountType type) {
        return com.inkcore.domain.productionorder.model.DiscountType.toValue(type);
    }
}
