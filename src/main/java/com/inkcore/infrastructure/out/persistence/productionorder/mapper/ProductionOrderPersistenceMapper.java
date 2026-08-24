package com.inkcore.infrastructure.out.persistence.productionorder.mapper;

import com.inkcore.domain.productionorder.model.BillingDetails;
import com.inkcore.domain.productionorder.model.ClientCostingMode;
import com.inkcore.domain.productionorder.model.ClientPlateType;
import com.inkcore.domain.productionorder.model.DiscountType;
import com.inkcore.domain.productionorder.model.FlipType;
import com.inkcore.domain.productionorder.model.OperatorAssignment;
import com.inkcore.domain.productionorder.model.PaperRow;
import com.inkcore.domain.productionorder.model.Plate;
import com.inkcore.domain.productionorder.model.PostpressLine;
import com.inkcore.domain.productionorder.model.PostpressRecord;
import com.inkcore.domain.productionorder.model.PostpressType;
import com.inkcore.domain.productionorder.model.PrepressDetails;
import com.inkcore.domain.productionorder.model.PrintConfig;
import com.inkcore.domain.productionorder.model.PrintEntry;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.productionorder.model.ProductionOrderStage;
import com.inkcore.domain.productionorder.model.StageDiscount;
import com.inkcore.infrastructure.out.persistence.productionorder.entity.ProductionOrderBillingDetailsEntity;
import com.inkcore.infrastructure.out.persistence.productionorder.entity.ProductionOrderEntity;
import com.inkcore.infrastructure.out.persistence.productionorder.entity.ProductionOrderOperatorEntity;
import com.inkcore.infrastructure.out.persistence.productionorder.entity.ProductionOrderPaperRowEntity;
import com.inkcore.infrastructure.out.persistence.productionorder.entity.ProductionOrderPlateEntity;
import com.inkcore.infrastructure.out.persistence.productionorder.entity.ProductionOrderPostpressLineEntity;
import com.inkcore.infrastructure.out.persistence.productionorder.entity.ProductionOrderPostpressRecordEntity;
import com.inkcore.infrastructure.out.persistence.productionorder.entity.ProductionOrderPrepressDetailsEntity;
import com.inkcore.infrastructure.out.persistence.productionorder.entity.ProductionOrderPrintEntity;
import com.inkcore.infrastructure.out.persistence.productionorder.entity.ProductionOrderPrintEntryEntity;
import com.inkcore.infrastructure.out.persistence.productionorder.entity.ProductionOrderStageDiscountEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Conversión entre el agregado Orden de Producción y sus entidades JPA.
 * Escrito a mano: el proyecto no usa MapStruct.
 */
@Component
public class ProductionOrderPersistenceMapper {

    public ProductionOrder toDomain(ProductionOrderEntity entity) {
        ProductionOrder order = ProductionOrder.reconstitute();
        order.setProductionOrderId(entity.getProductionOrderId());
        order.setCompanyId(entity.getCompanyId());
        order.setOrderNumber(entity.getOrderNumber());
        order.setVersion(entity.getVersion());
        order.setClientId(entity.getClientId());
        order.setWorkName(entity.getWorkName());
        order.setSellerId(entity.getSellerId());
        order.setOrderDate(entity.getOrderDate());
        order.setRequestedQuantity(entity.getRequestedQuantity());
        order.setProposalQuantity1(entity.getProposalQuantity1());
        order.setProposalQuantity2(entity.getProposalQuantity2());
        order.setSpecificationsCompletedAt(entity.getSpecificationsCompletedAt());
        order.setCuttingCompletedAt(entity.getCuttingCompletedAt());
        order.setPrintingCompletedAt(entity.getPrintingCompletedAt());
        order.setFinishedProductsCompletedAt(entity.getFinishedProductsCompletedAt());
        order.setFinishingProcessesCompletedAt(entity.getFinishingProcessesCompletedAt());
        order.setClientSuppliesPaperDefault(entity.getClientSuppliesPaperDefault());
        order.setRoundingMargin(entity.getRoundingMargin());
        order.setStatus(entity.getStatus());
        order.setState(entity.isState());
        order.setCreatedAt(entity.getCreatedAt());
        order.setUpdatedAt(entity.getUpdatedAt());
        order.setCreatedBy(entity.getCreatedBy());
        order.setUpdatedBy(entity.getUpdatedBy());
        return order;
    }

    public ProductionOrderEntity toNewEntity(ProductionOrder order) {
        ProductionOrderEntity entity = new ProductionOrderEntity();
        entity.setProductionOrderId(order.getProductionOrderId());
        // Dejar version = null: si se asigna (p. ej. 0L), Hibernate trata la entidad
        // como detached y falla con "detached entity passed to persist".
        copyScalars(order, entity);
        return entity;
    }

    /**
     * No copia {@code version}: la gestiona Hibernate con {@code @Version}.
     */
    public void copyScalars(ProductionOrder order, ProductionOrderEntity entity) {
        entity.setCompanyId(order.getCompanyId());
        entity.setOrderNumber(order.getOrderNumber());
        entity.setClientId(order.getClientId());
        entity.setWorkName(order.getWorkName());
        entity.setSellerId(order.getSellerId());
        entity.setOrderDate(order.getOrderDate());
        entity.setRequestedQuantity(order.getRequestedQuantity());
        entity.setProposalQuantity1(order.getProposalQuantity1());
        entity.setProposalQuantity2(order.getProposalQuantity2());
        entity.setSpecificationsCompletedAt(order.getSpecificationsCompletedAt());
        entity.setCuttingCompletedAt(order.getCuttingCompletedAt());
        entity.setPrintingCompletedAt(order.getPrintingCompletedAt());
        entity.setFinishedProductsCompletedAt(order.getFinishedProductsCompletedAt());
        entity.setFinishingProcessesCompletedAt(order.getFinishingProcessesCompletedAt());
        entity.setClientSuppliesPaperDefault(order.getClientSuppliesPaperDefault());
        entity.setRoundingMargin(order.getRoundingMargin());
        entity.setStatus(order.getStatus());
        entity.setState(order.isState());
        entity.setCreatedAt(order.getCreatedAt());
        entity.setUpdatedAt(order.getUpdatedAt());
        entity.setCreatedBy(order.getCreatedBy());
        entity.setUpdatedBy(order.getUpdatedBy());
    }

    public PrepressDetails toDomain(ProductionOrderPrepressDetailsEntity entity) {
        PrepressDetails details = new PrepressDetails();
        details.setProductionOrderId(entity.getProductionOrderId());
        details.setCompanyId(entity.getCompanyId());
        details.setNewDesign(entity.getNewDesign());
        details.setDesignName(entity.getDesignName());
        details.setExistingDesignOrderId(entity.getExistingDesignOrderId());
        details.setHasDesignCost(entity.isHasDesignCost());
        details.setDesignCost(entity.getDesignCost());
        details.setClientSuppliesPlates(entity.getClientSuppliesPlates());
        details.setClientPlateType(ClientPlateType.fromValue(entity.getClientPlateType()));
        details.setNewPlateCost(entity.getNewPlateCost());
        details.setAssemblyPriceId(entity.getAssemblyPriceId());
        details.setAssemblyPriceName(entity.getAssemblyPriceName());
        details.setAssemblyPriceCost(entity.getAssemblyPriceCost());
        details.setDieCutLine(entity.isDieCutLine());
        details.setUvReserve(entity.isUvReserve());
        details.setStamping(entity.isStamping());
        details.setEmbossing(entity.isEmbossing());
        details.setTotalPlatesValue(entity.getTotalPlatesValue());
        details.setPrepressDiscountType(DiscountType.fromValue(entity.getPrepressDiscountType()));
        details.setPrepressDiscountValue(entity.getPrepressDiscountValue());
        details.setPrepressCompletedAt(entity.getPrepressCompletedAt());
        return details;
    }

    public ProductionOrderPrepressDetailsEntity toEntity(
            PrepressDetails details,
            ProductionOrderPrepressDetailsEntity existing,
            LocalDateTime now
    ) {
        ProductionOrderPrepressDetailsEntity entity =
                existing == null ? new ProductionOrderPrepressDetailsEntity() : existing;
        entity.setProductionOrderId(details.getProductionOrderId());
        entity.setCompanyId(details.getCompanyId());
        entity.setNewDesign(details.getNewDesign());
        entity.setDesignName(details.getDesignName());
        entity.setExistingDesignOrderId(details.getExistingDesignOrderId());
        entity.setHasDesignCost(details.isHasDesignCost());
        entity.setDesignCost(details.getDesignCost());
        entity.setClientSuppliesPlates(details.getClientSuppliesPlates());
        entity.setClientPlateType(ClientPlateType.toValue(details.getClientPlateType()));
        entity.setNewPlateCost(details.getNewPlateCost());
        entity.setAssemblyPriceId(details.getAssemblyPriceId());
        entity.setAssemblyPriceName(details.getAssemblyPriceName());
        entity.setAssemblyPriceCost(details.getAssemblyPriceCost());
        entity.setDieCutLine(details.isDieCutLine());
        entity.setUvReserve(details.isUvReserve());
        entity.setStamping(details.isStamping());
        entity.setEmbossing(details.isEmbossing());
        entity.setTotalPlatesValue(details.getTotalPlatesValue());
        entity.setPrepressDiscountType(DiscountType.toValue(details.getPrepressDiscountType()));
        entity.setPrepressDiscountValue(details.getPrepressDiscountValue());
        entity.setPrepressCompletedAt(details.getPrepressCompletedAt());
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(now);
        }
        entity.setUpdatedAt(now);
        return entity;
    }

    public BillingDetails toDomain(ProductionOrderBillingDetailsEntity entity) {
        BillingDetails details = new BillingDetails();
        details.setProductionOrderId(entity.getProductionOrderId());
        details.setCompanyId(entity.getCompanyId());
        details.setBillingDiscountType(DiscountType.fromValue(entity.getBillingDiscountType()));
        details.setBillingDiscountValue(entity.getBillingDiscountValue());
        details.setClientCostingMode(ClientCostingMode.fromValue(entity.getClientCostingMode()));
        details.setClientDiscountType(DiscountType.fromValue(entity.getClientDiscountType()));
        details.setClientDiscountValue(entity.getClientDiscountValue());
        details.setClientProfitabilityType(DiscountType.fromValue(entity.getClientProfitabilityType()));
        details.setClientProfitabilityValue(entity.getClientProfitabilityValue());
        details.setClientVolumeCosting(entity.getClientVolumeCosting());
        details.setDeliveryStartDate(entity.getDeliveryStartDate());
        details.setDeliveryEndDate(entity.getDeliveryEndDate());
        details.setAdvancePercentage(entity.getAdvancePercentage());
        details.setClientSignatureName(entity.getClientSignatureName());
        details.setBankAccountId(entity.getBankAccountId());
        details.setBillingCompletedAt(entity.getBillingCompletedAt());
        return details;
    }

    public ProductionOrderBillingDetailsEntity toEntity(
            BillingDetails details,
            ProductionOrderBillingDetailsEntity existing,
            LocalDateTime now
    ) {
        ProductionOrderBillingDetailsEntity entity =
                existing == null ? new ProductionOrderBillingDetailsEntity() : existing;
        entity.setProductionOrderId(details.getProductionOrderId());
        entity.setCompanyId(details.getCompanyId());
        entity.setBillingDiscountType(DiscountType.toValue(details.getBillingDiscountType()));
        entity.setBillingDiscountValue(details.getBillingDiscountValue());
        entity.setClientCostingMode(ClientCostingMode.toValue(details.getClientCostingMode()));
        entity.setClientDiscountType(DiscountType.toValue(details.getClientDiscountType()));
        entity.setClientDiscountValue(details.getClientDiscountValue());
        entity.setClientProfitabilityType(DiscountType.toValue(details.getClientProfitabilityType()));
        entity.setClientProfitabilityValue(details.getClientProfitabilityValue());
        entity.setClientVolumeCosting(details.getClientVolumeCosting());
        entity.setDeliveryStartDate(details.getDeliveryStartDate());
        entity.setDeliveryEndDate(details.getDeliveryEndDate());
        entity.setAdvancePercentage(details.getAdvancePercentage());
        entity.setClientSignatureName(details.getClientSignatureName());
        entity.setBankAccountId(details.getBankAccountId());
        entity.setBillingCompletedAt(details.getBillingCompletedAt());
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(now);
        }
        entity.setUpdatedAt(now);
        return entity;
    }

    public OperatorAssignment toDomain(ProductionOrderOperatorEntity entity) {
        OperatorAssignment assignment = new OperatorAssignment();
        assignment.setOperatorAssignmentId(entity.getProductionOrderOperatorId());
        assignment.setCompanyId(entity.getCompanyId());
        assignment.setProductionOrderId(entity.getProductionOrderId());
        assignment.setStage(ProductionOrderStage.fromValue(entity.getStage()));
        assignment.setUserId(entity.getUserId());
        return assignment;
    }

    public ProductionOrderOperatorEntity toEntity(OperatorAssignment assignment, LocalDateTime now) {
        ProductionOrderOperatorEntity entity = new ProductionOrderOperatorEntity();
        entity.setProductionOrderOperatorId(assignment.getOperatorAssignmentId());
        entity.setCompanyId(assignment.getCompanyId());
        entity.setProductionOrderId(assignment.getProductionOrderId());
        entity.setStage(assignment.getStage() == null ? null : assignment.getStage().name());
        entity.setUserId(assignment.getUserId());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }

    public StageDiscount toDomain(ProductionOrderStageDiscountEntity entity) {
        StageDiscount discount = new StageDiscount();
        discount.setStageDiscountId(entity.getProductionOrderStageDiscountId());
        discount.setCompanyId(entity.getCompanyId());
        discount.setProductionOrderId(entity.getProductionOrderId());
        discount.setStage(ProductionOrderStage.fromValue(entity.getStage()));
        discount.setDiscountType(DiscountType.fromValue(entity.getDiscountType()));
        discount.setDiscountValue(entity.getDiscountValue());
        return discount;
    }

    public ProductionOrderStageDiscountEntity toEntity(StageDiscount discount, LocalDateTime now) {
        ProductionOrderStageDiscountEntity entity = new ProductionOrderStageDiscountEntity();
        entity.setProductionOrderStageDiscountId(discount.getStageDiscountId());
        entity.setCompanyId(discount.getCompanyId());
        entity.setProductionOrderId(discount.getProductionOrderId());
        entity.setStage(discount.getStage() == null ? null : discount.getStage().name());
        entity.setDiscountType(DiscountType.toValue(discount.getDiscountType()));
        entity.setDiscountValue(discount.getDiscountValue());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }

    public Plate toDomain(ProductionOrderPlateEntity entity) {
        Plate plate = new Plate();
        plate.setPlateId(entity.getProductionOrderPlateId());
        plate.setCompanyId(entity.getCompanyId());
        plate.setProductionOrderId(entity.getProductionOrderId());
        plate.setColors(entity.getColors());
        plate.setPlateTypeId(entity.getPlateTypeId());
        plate.setPlateName(entity.getPlateName());
        plate.setPlateSize(entity.getPlateSize());
        plate.setPlatePrice(entity.getPlatePrice());
        plate.setQuantity(entity.getQuantity());
        plate.setCavities(entity.getCavities());
        plate.setGoodSizes(entity.getGoodSizes());
        plate.setSurplus(entity.getSurplus());
        plate.setPlatesCount(entity.getPlatesCount());
        plate.setTotalValue(entity.getTotalValue());
        plate.setDetail(entity.getDetail());
        plate.setObservation(entity.getObservation());
        plate.setManualEntry(entity.isManualEntry());
        plate.setPlateSupply(entity.getPlateSupply());
        plate.setPlateReplacement(entity.isPlateReplacement());
        plate.setReplacementQuantity(entity.getReplacementQuantity());
        return plate;
    }

    public ProductionOrderPlateEntity toEntity(Plate plate, LocalDateTime now) {
        ProductionOrderPlateEntity entity = new ProductionOrderPlateEntity();
        entity.setProductionOrderPlateId(plate.getPlateId());
        entity.setCompanyId(plate.getCompanyId());
        entity.setProductionOrderId(plate.getProductionOrderId());
        entity.setColors(plate.getColors());
        entity.setPlateTypeId(plate.getPlateTypeId());
        entity.setPlateName(plate.getPlateName());
        entity.setPlateSize(plate.getPlateSize());
        entity.setPlatePrice(plate.getPlatePrice());
        entity.setQuantity(plate.getQuantity());
        entity.setCavities(plate.getCavities());
        entity.setGoodSizes(plate.getGoodSizes());
        entity.setSurplus(plate.getSurplus());
        entity.setPlatesCount(plate.getPlatesCount());
        entity.setTotalValue(plate.getTotalValue());
        entity.setDetail(plate.getDetail());
        entity.setObservation(plate.getObservation());
        entity.setManualEntry(plate.isManualEntry());
        entity.setPlateSupply(plate.getPlateSupply());
        entity.setPlateReplacement(plate.isPlateReplacement());
        entity.setReplacementQuantity(plate.getReplacementQuantity());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }

    public PaperRow toDomain(ProductionOrderPaperRowEntity entity) {
        PaperRow row = new PaperRow();
        row.setPaperRowId(entity.getProductionOrderPaperRowId());
        row.setCompanyId(entity.getCompanyId());
        row.setProductionOrderId(entity.getProductionOrderId());
        row.setPlateId(entity.getPlateId());
        row.setParentRowId(entity.getParentRowId());
        row.setCutRowKey(entity.getCutRowKey());
        row.setMissingSupply(entity.isMissingSupply());
        row.setMissingSheetsQuantity(entity.getMissingSheetsQuantity());
        row.setClientSuppliesPaper(entity.isClientSuppliesPaper());
        row.setPaperTypeId(entity.getPaperTypeId());
        row.setPaperName(entity.getPaperName());
        row.setPaperSize(entity.getPaperSize());
        row.setSheetValue(entity.getSheetValue());
        row.setPackageUnit(entity.getPackageUnit());
        row.setCoated(entity.getCoated());
        row.setCutLayoutId(entity.getCutLayoutId());
        row.setCutLayoutName(entity.getCutLayoutName());
        row.setCutLayoutSize(entity.getCutLayoutSize());
        row.setPiecesPerSheet(entity.getPiecesPerSheet());
        row.setCutValue(entity.getCutValue());
        row.setPaperCut(entity.getPaperCut());
        row.setDeliveredSheetsByClient(entity.getDeliveredSheetsByClient());
        row.setManualGoodSizes(entity.getManualGoodSizes());
        row.setManualSurplus(entity.getManualSurplus());
        row.setCalculatedSheetsCount(entity.getCalculatedSheetsCount());
        row.setTotalPaperValue(entity.getTotalPaperValue());
        row.setTotalCutValue(entity.getTotalCutValue());
        return row;
    }

    public ProductionOrderPaperRowEntity toEntity(PaperRow row, LocalDateTime now) {
        ProductionOrderPaperRowEntity entity = new ProductionOrderPaperRowEntity();
        entity.setProductionOrderPaperRowId(row.getPaperRowId());
        entity.setCompanyId(row.getCompanyId());
        entity.setProductionOrderId(row.getProductionOrderId());
        entity.setPlateId(row.getPlateId());
        entity.setParentRowId(row.getParentRowId());
        entity.setCutRowKey(row.getCutRowKey());
        entity.setMissingSupply(row.isMissingSupply());
        entity.setMissingSheetsQuantity(row.getMissingSheetsQuantity());
        entity.setClientSuppliesPaper(row.isClientSuppliesPaper());
        entity.setPaperTypeId(row.getPaperTypeId());
        entity.setPaperName(row.getPaperName());
        entity.setPaperSize(row.getPaperSize());
        entity.setSheetValue(row.getSheetValue());
        entity.setPackageUnit(row.getPackageUnit());
        entity.setCoated(row.getCoated());
        entity.setCutLayoutId(row.getCutLayoutId());
        entity.setCutLayoutName(row.getCutLayoutName());
        entity.setCutLayoutSize(row.getCutLayoutSize());
        entity.setPiecesPerSheet(row.getPiecesPerSheet());
        entity.setCutValue(row.getCutValue());
        entity.setPaperCut(row.getPaperCut());
        entity.setDeliveredSheetsByClient(row.getDeliveredSheetsByClient());
        entity.setManualGoodSizes(row.getManualGoodSizes());
        entity.setManualSurplus(row.getManualSurplus());
        entity.setCalculatedSheetsCount(row.getCalculatedSheetsCount());
        entity.setTotalPaperValue(row.getTotalPaperValue());
        entity.setTotalCutValue(row.getTotalCutValue());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }

    public PrintConfig toDomain(ProductionOrderPrintEntity entity) {
        PrintConfig print = new PrintConfig();
        print.setPrintId(entity.getProductionOrderPrintId());
        print.setCompanyId(entity.getCompanyId());
        print.setProductionOrderId(entity.getProductionOrderId());
        print.setPlateId(entity.getPlateId());
        print.setClientSuppliesSherpa(entity.getClientSuppliesSherpa());
        print.setSherpaTestPrice(entity.getSherpaTestPrice());
        print.setMachineOutputValue(entity.getMachineOutputValue());
        print.setInkEstimation(entity.getInkEstimation());
        print.setPrintingDiscountType(DiscountType.fromValue(entity.getPrintingDiscountType()));
        print.setPrintingDiscountValue(entity.getPrintingDiscountValue());
        print.setCompleted(entity.isCompleted());
        print.setEntries(new ArrayList<>());
        return print;
    }

    public ProductionOrderPrintEntity toEntity(PrintConfig print, LocalDateTime now) {
        ProductionOrderPrintEntity entity = new ProductionOrderPrintEntity();
        entity.setProductionOrderPrintId(print.getPrintId());
        entity.setCompanyId(print.getCompanyId());
        entity.setProductionOrderId(print.getProductionOrderId());
        entity.setPlateId(print.getPlateId());
        entity.setClientSuppliesSherpa(print.getClientSuppliesSherpa());
        entity.setSherpaTestPrice(print.getSherpaTestPrice());
        entity.setMachineOutputValue(print.getMachineOutputValue());
        entity.setInkEstimation(print.getInkEstimation());
        entity.setPrintingDiscountType(DiscountType.toValue(print.getPrintingDiscountType()));
        entity.setPrintingDiscountValue(print.getPrintingDiscountValue());
        entity.setCompleted(print.isCompleted());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }

    public PrintEntry toDomain(ProductionOrderPrintEntryEntity entity) {
        PrintEntry entry = new PrintEntry();
        entry.setPrintEntryId(entity.getProductionOrderPrintEntryId());
        entry.setCompanyId(entity.getCompanyId());
        entry.setPrintId(entity.getPrintId());
        entry.setShotsInkCount(entity.getShotsInkCount());
        entry.setShotsInks(entity.getShotsInks());
        entry.setReverseInkCount(entity.getReverseInkCount());
        entry.setReverseInks(entity.getReverseInks());
        entry.setBasicFlipType(FlipType.fromValue(entity.getBasicFlipType()));
        entry.setBasicThousandRateId(entity.getBasicThousandRateId());
        entry.setBasicRateName(entity.getBasicRateName());
        entry.setBasicRatePrice(entity.getBasicRatePrice());
        entry.setBasicRateGripperFlipPrice(entity.getBasicRateGripperFlipPrice());
        entry.setBasicRateSquareFlipPrice(entity.getBasicRateSquareFlipPrice());
        entry.setBasicCalculatedThousands(entity.getBasicCalculatedThousands());
        entry.setBasicPrintingPrice(entity.getBasicPrintingPrice());
        entry.setPantoneFlipType(FlipType.fromValue(entity.getPantoneFlipType()));
        entry.setClientSuppliesPantoneInk(entity.getClientSuppliesPantoneInk());
        entry.setPantoneInkChargePrice(entity.getPantoneInkChargePrice());
        entry.setPantoneThousandRateId(entity.getPantoneThousandRateId());
        entry.setPantoneRateName(entity.getPantoneRateName());
        entry.setPantoneRatePrice(entity.getPantoneRatePrice());
        entry.setPantoneRateGripperFlipPrice(entity.getPantoneRateGripperFlipPrice());
        entry.setPantoneRateSquareFlipPrice(entity.getPantoneRateSquareFlipPrice());
        entry.setPantoneCalculatedThousands(entity.getPantoneCalculatedThousands());
        entry.setPantonePrintingPrice(entity.getPantonePrintingPrice());
        return entry;
    }

    public ProductionOrderPrintEntryEntity toEntity(PrintEntry entry, LocalDateTime now) {
        ProductionOrderPrintEntryEntity entity = new ProductionOrderPrintEntryEntity();
        entity.setProductionOrderPrintEntryId(entry.getPrintEntryId());
        entity.setCompanyId(entry.getCompanyId());
        entity.setPrintId(entry.getPrintId());
        entity.setShotsInkCount(entry.getShotsInkCount());
        entity.setShotsInks(new ArrayList<>(entry.getShotsInks()));
        entity.setReverseInkCount(entry.getReverseInkCount());
        entity.setReverseInks(new ArrayList<>(entry.getReverseInks()));
        entity.setBasicFlipType(FlipType.toValue(entry.getBasicFlipType()));
        entity.setBasicThousandRateId(entry.getBasicThousandRateId());
        entity.setBasicRateName(entry.getBasicRateName());
        entity.setBasicRatePrice(entry.getBasicRatePrice());
        entity.setBasicRateGripperFlipPrice(entry.getBasicRateGripperFlipPrice());
        entity.setBasicRateSquareFlipPrice(entry.getBasicRateSquareFlipPrice());
        entity.setBasicCalculatedThousands(entry.getBasicCalculatedThousands());
        entity.setBasicPrintingPrice(entry.getBasicPrintingPrice());
        entity.setPantoneFlipType(FlipType.toValue(entry.getPantoneFlipType()));
        entity.setClientSuppliesPantoneInk(entry.getClientSuppliesPantoneInk());
        entity.setPantoneInkChargePrice(entry.getPantoneInkChargePrice());
        entity.setPantoneThousandRateId(entry.getPantoneThousandRateId());
        entity.setPantoneRateName(entry.getPantoneRateName());
        entity.setPantoneRatePrice(entry.getPantoneRatePrice());
        entity.setPantoneRateGripperFlipPrice(entry.getPantoneRateGripperFlipPrice());
        entity.setPantoneRateSquareFlipPrice(entry.getPantoneRateSquareFlipPrice());
        entity.setPantoneCalculatedThousands(entry.getPantoneCalculatedThousands());
        entity.setPantonePrintingPrice(entry.getPantonePrintingPrice());
        entity.setCreatedAt(now);
        return entity;
    }

    public PostpressRecord toDomain(ProductionOrderPostpressRecordEntity entity) {
        PostpressRecord record = new PostpressRecord();
        record.setRecordId(entity.getProductionOrderPostpressRecordId());
        record.setCompanyId(entity.getCompanyId());
        record.setProductionOrderId(entity.getProductionOrderId());
        record.setPlateId(entity.getPlateId());
        record.setType(PostpressType.fromValue(entity.getType()));
        record.setCompleted(entity.isCompleted());
        record.setLines(new ArrayList<>());
        return record;
    }

    public ProductionOrderPostpressRecordEntity toEntity(PostpressRecord record, LocalDateTime now) {
        ProductionOrderPostpressRecordEntity entity = new ProductionOrderPostpressRecordEntity();
        entity.setProductionOrderPostpressRecordId(record.getRecordId());
        entity.setCompanyId(record.getCompanyId());
        entity.setProductionOrderId(record.getProductionOrderId());
        entity.setPlateId(record.getPlateId());
        entity.setType(record.getType() == null ? null : record.getType().name());
        entity.setCompleted(record.isCompleted());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }

    public PostpressLine toDomain(ProductionOrderPostpressLineEntity entity) {
        PostpressLine line = new PostpressLine();
        line.setLineId(entity.getProductionOrderPostpressLineId());
        line.setCompanyId(entity.getCompanyId());
        line.setRecordId(entity.getRecordId());
        line.setCatalogItemId(entity.getCatalogItemId());
        line.setItemName(entity.getItemName());
        line.setSource(entity.getSource());
        line.setValuePerCm2(entity.getValuePerCm2());
        line.setMinCost(entity.getMinCost());
        line.setAreaFactor(entity.getAreaFactor());
        line.setGoodSizes(entity.getGoodSizes());
        line.setCalculatedPrice(entity.getCalculatedPrice());
        line.setChargedPrice(entity.getChargedPrice());
        line.setAppliedMinCost(entity.isAppliedMinCost());
        line.setPositive(entity.getPositive());
        line.setCliche(entity.getCliche());
        return line;
    }

    public ProductionOrderPostpressLineEntity toEntity(PostpressLine line, LocalDateTime now) {
        ProductionOrderPostpressLineEntity entity = new ProductionOrderPostpressLineEntity();
        entity.setProductionOrderPostpressLineId(line.getLineId());
        entity.setCompanyId(line.getCompanyId());
        entity.setRecordId(line.getRecordId());
        entity.setCatalogItemId(line.getCatalogItemId());
        entity.setItemName(line.getItemName());
        entity.setSource(line.getSource());
        entity.setValuePerCm2(line.getValuePerCm2());
        entity.setMinCost(line.getMinCost());
        entity.setAreaFactor(line.getAreaFactor());
        entity.setGoodSizes(line.getGoodSizes());
        entity.setCalculatedPrice(line.getCalculatedPrice());
        entity.setChargedPrice(line.getChargedPrice());
        entity.setAppliedMinCost(line.isAppliedMinCost());
        entity.setPositive(line.getPositive());
        entity.setCliche(line.getCliche());
        entity.setCreatedAt(now);
        return entity;
    }

    public <E, D> List<D> mapAll(List<E> entities, Function<E, D> mapper) {
        List<D> result = new ArrayList<>(entities.size());
        for (E entity : entities) {
            result.add(mapper.apply(entity));
        }
        return result;
    }
}
