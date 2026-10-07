package com.inkcore.application.paper.usecase;

import com.inkcore.domain.paper.model.PaperSupplierPrice;
import com.inkcore.domain.paper.ports.out.PaperSupplierPriceRepositoryPort;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import com.inkcore.domain.supplier.ports.out.SupplierRepositoryPort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReplacePaperPricesUseCase {

    private final PaperSupplierPriceRepositoryPort priceRepository;
    private final PaperAccess access;
    private final PaperSupport support;
    private final SupplierRepositoryPort supplierRepository;
    private final Clock clock;

    public ReplacePaperPricesUseCase(
            PaperSupplierPriceRepositoryPort priceRepository,
            PaperAccess access,
            PaperSupport support,
            SupplierRepositoryPort supplierRepository,
            Clock clock
    ) {
        this.priceRepository = priceRepository;
        this.access = access;
        this.support = support;
        this.supplierRepository = supplierRepository;
        this.clock = clock;
    }

    @Transactional
    public List<PaperSupplierPrice> execute(
            String paperId,
            PaperCommands.ReplacePaperPricesCommand command,
            Authentication authentication
    ) {
        String companyId = support.companyId(authentication);
        String userId = support.userId(authentication);
        access.requirePaper(paperId, companyId);
        List<PaperCommands.ReplacePaperPriceItem> items =
                command.prices() == null ? List.of() : command.prices();
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Debe enviar al menos un precio por papel");
        }

        Map<String, PaperSupplierPrice> existingBySupplier = priceRepository
                .findByPaperId(companyId, paperId).stream()
                .collect(Collectors.toMap(PaperSupplierPrice::getSupplierId, Function.identity()));

        List<PaperCommands.ReplacePaperPriceItem> normalizedItems = new ArrayList<>(items);
        ensureSuppliers(companyId, normalizedItems);
        ensureSinglePreferred(normalizedItems);

        LocalDate today = LocalDate.now(clock);
        LocalDateTime now = LocalDateTime.now(clock);
        List<PaperSupplierPrice> desired = new ArrayList<>();
        for (PaperCommands.ReplacePaperPriceItem item : normalizedItems) {
            PaperSupplierPrice existing = existingBySupplier.get(item.supplierId());
            LocalDate priceDate = resolvePriceDate(item, existing, today);
            boolean state = Objects.requireNonNullElse(
                    item.state(), existing == null || existing.isState());
            if (existing == null) {
                desired.add(PaperSupplierPrice.createNew(
                        companyId,
                        paperId,
                        item.supplierId(),
                        item.sheetValue(),
                        item.packageUnit(),
                        item.freightPerSheet(),
                        item.minPurchaseSheets(),
                        item.paymentDays(),
                        item.deliveryDays(),
                        priceDate,
                        Boolean.TRUE.equals(item.preferred()),
                        state,
                        now
                ));
            } else {
                desired.add(existing.withPricing(
                        item.sheetValue(),
                        item.packageUnit(),
                        item.freightPerSheet(),
                        item.minPurchaseSheets(),
                        item.paymentDays(),
                        item.deliveryDays(),
                        priceDate,
                        Boolean.TRUE.equals(item.preferred()),
                        state,
                        now
                ));
            }
        }
        return priceRepository.replaceDiff(companyId, paperId, desired, userId);
    }

    private void ensureSuppliers(String companyId, List<PaperCommands.ReplacePaperPriceItem> items) {
        for (PaperCommands.ReplacePaperPriceItem item : items) {
            supplierRepository.findById(item.supplierId())
                    .filter(s -> companyId.equals(s.getCompanyId()))
                    .orElseThrow(() -> new ResourceNotFoundException("SUPPLIER_NOT_FOUND", "Proveedor no encontrado"));
        }
    }

    private static void ensureSinglePreferred(List<PaperCommands.ReplacePaperPriceItem> items) {
        long preferredCount = items.stream().filter(i -> Boolean.TRUE.equals(i.preferred())).count();
        if (preferredCount == 0) {
            PaperCommands.ReplacePaperPriceItem lowest = items.stream()
                    .min(Comparator.comparing(PaperCommands.ReplacePaperPriceItem::sheetValue))
                    .orElseThrow();
            int idx = items.indexOf(lowest);
            items.set(idx, new PaperCommands.ReplacePaperPriceItem(
                    lowest.supplierId(),
                    lowest.sheetValue(),
                    lowest.packageUnit(),
                    lowest.freightPerSheet(),
                    lowest.minPurchaseSheets(),
                    lowest.paymentDays(),
                    lowest.deliveryDays(),
                    lowest.priceDate(),
                    true,
                    lowest.state()
            ));
            return;
        }
        if (preferredCount > 1) {
            throw new IllegalArgumentException("Solo puede haber un proveedor preferido por papel");
        }
    }

    private static LocalDate resolvePriceDate(
            PaperCommands.ReplacePaperPriceItem item,
            PaperSupplierPrice existing,
            LocalDate today
    ) {
        if (item.priceDate() != null) {
            return item.priceDate();
        }
        if (existing == null) {
            return today;
        }
        boolean valueChanged = existing.getSheetValue().compareTo(item.sheetValue()) != 0;
        boolean freightChanged = existing.getFreightPerSheet().compareTo(
                item.freightPerSheet() == null ? BigDecimal.ZERO : item.freightPerSheet()) != 0;
        if (valueChanged || freightChanged) {
            return today;
        }
        return existing.getPriceDate();
    }
}
