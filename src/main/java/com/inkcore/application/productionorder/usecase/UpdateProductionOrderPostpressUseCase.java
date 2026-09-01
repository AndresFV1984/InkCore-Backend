package com.inkcore.application.productionorder.usecase;

import com.inkcore.domain.finish.model.Finish;
import com.inkcore.domain.finish.ports.out.FinishRepositoryPort;
import com.inkcore.domain.finishingprocess.model.FinishingProcess;
import com.inkcore.domain.finishingprocess.ports.out.FinishingProcessRepositoryPort;
import com.inkcore.domain.productionorder.exception.ProductionOrderBusinessRuleException;
import com.inkcore.domain.productionorder.model.DiscountType;
import com.inkcore.domain.productionorder.model.Plate;
import com.inkcore.domain.productionorder.model.PostpressLine;
import com.inkcore.domain.productionorder.model.PostpressRecord;
import com.inkcore.domain.productionorder.model.PostpressType;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.productionorder.service.ProductionOrderCalculator;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UpdateProductionOrderPostpressUseCase {

    private final ProductionOrderSupport support;
    private final ProductionOrderOperatorsApplier operatorsApplier;
    private final FinishRepositoryPort finishRepository;
    private final FinishingProcessRepositoryPort finishingProcessRepository;

    public UpdateProductionOrderPostpressUseCase(
            ProductionOrderSupport support,
            ProductionOrderOperatorsApplier operatorsApplier,
            FinishRepositoryPort finishRepository,
            FinishingProcessRepositoryPort finishingProcessRepository
    ) {
        this.support = support;
        this.operatorsApplier = operatorsApplier;
        this.finishRepository = finishRepository;
        this.finishingProcessRepository = finishingProcessRepository;
    }

    @Transactional
    public ProductionOrder execute(
            String productionOrderId,
            PostpressType type,
            UpdateProductionOrderPostpressCommand command,
            Authentication authentication
    ) {
        String companyId = support.companyId(authentication);
        String userId = support.userId(authentication);
        ProductionOrder order = support.requireOrder(productionOrderId, companyId);
        support.assertVersion(order, command.version());

        List<PostpressRecord> other = order.getPostpressRecords().stream()
                .filter(r -> r.getType() != type)
                .collect(Collectors.toCollection(ArrayList::new));
        List<PostpressRecord> built = buildRecords(order, companyId, type, command.records());
        other.addAll(built);
        order.setPostpressRecords(other);

        order.upsertStageDiscount(
                type.getStage(),
                DiscountType.fromValue(command.discountType()),
                command.discountValue()
        );
        operatorsApplier.apply(
                order,
                companyId,
                command.operators(),
                command.operatorUserId(),
                type.getStage()
        );
        if (Boolean.TRUE.equals(command.completed())) {
            if (type == PostpressType.FINISHED_PRODUCT) {
                order.setFinishedProductsCompletedAt(support.now());
            } else {
                order.setFinishingProcessesCompletedAt(support.now());
            }
        }
        order.setUpdatedAt(support.now());
        order.setUpdatedBy(userId);
        return support.repository().save(order);
    }

    private List<PostpressRecord> buildRecords(
            ProductionOrder order,
            String companyId,
            PostpressType type,
            List<UpdateProductionOrderPostpressCommand.PostpressRecordInput> inputs
    ) {
        if (inputs == null || inputs.isEmpty()) {
            return List.of();
        }
        List<String> errors = new ArrayList<>();
        Set<String> plateIds = new HashSet<>();
        List<PostpressRecord> records = new ArrayList<>();

        for (int i = 0; i < inputs.size(); i++) {
            var input = inputs.get(i);
            if (input.plateId() == null || order.findPlate(input.plateId()).isEmpty()) {
                errors.add("records[" + i + "].plateId: plancha no encontrada");
                continue;
            }
            if (!plateIds.add(input.plateId())) {
                errors.add("records[" + i + "].plateId: solo un registro por plancha y tipo");
            }
            Plate plate = order.findPlate(input.plateId()).orElseThrow();
            PostpressRecord record = new PostpressRecord();
            if (input.recordId() != null && !input.recordId().isBlank()) {
                record.setRecordId(input.recordId().trim());
            }
            record.setCompanyId(companyId);
            record.setProductionOrderId(order.getProductionOrderId());
            record.setPlateId(plate.getPlateId());
            record.setType(type);
            record.setCompleted(Boolean.TRUE.equals(input.completed()));
            record.setLines(buildLines(companyId, type, record.getRecordId(), plate, input.lines(), errors, i));
            records.add(record);
        }
        if (!errors.isEmpty()) {
            throw new ProductionOrderBusinessRuleException("Regla de negocio incumplida", errors);
        }
        return records;
    }

    private List<PostpressLine> buildLines(
            String companyId,
            PostpressType type,
            String recordId,
            Plate plate,
            List<UpdateProductionOrderPostpressCommand.PostpressLineInput> inputs,
            List<String> errors,
            int recordIndex
    ) {
        if (inputs == null || inputs.isEmpty()) {
            return List.of();
        }
        List<PostpressLine> lines = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
            var input = inputs.get(i);
            String source = input.source() == null ? "catalog" : input.source().trim();
            if (!"catalog".equals(source) && !"quick-access".equals(source)) {
                errors.add("records[" + recordIndex + "].lines[" + i + "].source: catalog|quick-access");
            }

            PostpressLine line = new PostpressLine();
            if (input.lineId() != null && !input.lineId().isBlank()) {
                line.setLineId(input.lineId().trim());
            }
            line.setCompanyId(companyId);
            line.setRecordId(recordId);
            line.setSource(source);
            line.setAreaFactor(input.areaFactor());
            line.setGoodSizes(Objects.requireNonNullElse(input.goodSizes(), plate.getGoodSizes()));

            if (type == PostpressType.FINISHED_PRODUCT) {
                applyFinish(companyId, line, input.catalogItemId(), errors, recordIndex, i);
                line.setPositive(input.positive());
                line.setCliche(input.cliche());
            } else {
                applyFinishingProcess(companyId, line, input.catalogItemId(), errors, recordIndex, i);
                line.setPositive(null);
                line.setCliche(null);
            }

            BigDecimal calculated = ProductionOrderCalculator.calculatePostpressPrice(
                    line.getValuePerCm2(),
                    line.getAreaFactor(),
                    line.getGoodSizes()
            );
            line.setCalculatedPrice(calculated);
            boolean min = ProductionOrderCalculator.appliesMinCost(calculated, line.getMinCost());
            line.setAppliedMinCost(min);
            line.setChargedPrice(min ? line.getMinCost() : calculated);
            lines.add(line);
        }
        return lines;
    }

    private void applyFinish(
            String companyId,
            PostpressLine line,
            String catalogItemId,
            List<String> errors,
            int recordIndex,
            int lineIndex
    ) {
        if (catalogItemId == null || catalogItemId.isBlank()) {
            errors.add("records[" + recordIndex + "].lines[" + lineIndex + "].catalogItemId: obligatorio");
            return;
        }
        Finish finish = finishRepository.findById(catalogItemId).orElse(null);
        if (finish == null || !companyId.equals(finish.getCompanyId())) {
            errors.add("records[" + recordIndex + "].lines[" + lineIndex
                    + "].catalogItemId: no pertenece a finished_products");
            return;
        }
        line.setCatalogItemId(finish.getFinishId());
        line.setItemName(finish.getName());
        line.setValuePerCm2(finish.getValuePerCm2());
        line.setMinCost(finish.getMinCost());
    }

    private void applyFinishingProcess(
            String companyId,
            PostpressLine line,
            String catalogItemId,
            List<String> errors,
            int recordIndex,
            int lineIndex
    ) {
        if (catalogItemId == null || catalogItemId.isBlank()) {
            errors.add("records[" + recordIndex + "].lines[" + lineIndex + "].catalogItemId: obligatorio");
            return;
        }
        FinishingProcess process = finishingProcessRepository.findById(catalogItemId).orElse(null);
        if (process == null || !companyId.equals(process.getCompanyId())) {
            errors.add("records[" + recordIndex + "].lines[" + lineIndex
                    + "].catalogItemId: no pertenece a finishing_processes");
            return;
        }
        line.setCatalogItemId(process.getFinishingProcessId());
        line.setItemName(process.getName());
        line.setValuePerCm2(process.getValuePerCm2());
        line.setMinCost(process.getMinCost());
    }

    @SuppressWarnings("unused")
    private static boolean looksLikeUvReserve(String name) {
        return name != null && name.toLowerCase(Locale.ROOT).contains("uv");
    }
}
