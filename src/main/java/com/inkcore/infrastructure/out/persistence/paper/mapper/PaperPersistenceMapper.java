package com.inkcore.infrastructure.out.persistence.paper.mapper;

import com.inkcore.domain.paper.model.Paper;
import com.inkcore.domain.paper.model.PaperCutLayout;
import com.inkcore.domain.paper.model.PaperPriceHistory;
import com.inkcore.domain.paper.model.PaperRemnant;
import com.inkcore.domain.paper.model.PaperStock;
import com.inkcore.domain.paper.model.PaperSupplierPrice;
import com.inkcore.infrastructure.out.persistence.paper.entity.PaperCutLayoutEntity;
import com.inkcore.infrastructure.out.persistence.paper.entity.PaperEntity;
import com.inkcore.infrastructure.out.persistence.paper.entity.PaperPriceHistoryEntity;
import com.inkcore.infrastructure.out.persistence.paper.entity.PaperRemnantEntity;
import com.inkcore.infrastructure.out.persistence.paper.entity.PaperStockEntity;
import com.inkcore.infrastructure.out.persistence.paper.entity.PaperSupplierPriceEntity;
import org.springframework.stereotype.Component;

@Component
public class PaperPersistenceMapper {

    public PaperEntity toNewPaperEntity(Paper paper) {
        PaperEntity entity = new PaperEntity();
        copyPaperScalars(paper, entity);
        return entity;
    }

    public void copyPaperScalars(Paper paper, PaperEntity entity) {
        entity.setPaperId(paper.getPaperId());
        entity.setCompanyId(paper.getCompanyId());
        entity.setName(paper.getName());
        entity.setGrammage(paper.getGrammage());
        entity.setWidth(paper.getWidth());
        entity.setHeight(paper.getHeight());
        entity.setUnit(paper.getUnit());
        entity.setCoated(paper.isCoated());
        entity.setAcceptsRemnants(paper.isAcceptsRemnants());
        entity.setMinRemnantWidth(paper.getMinRemnantWidth());
        entity.setMinRemnantHeight(paper.getMinRemnantHeight());
        entity.setMinRemnantUnit(paper.getMinRemnantUnit());
        entity.setState(paper.isState());
        entity.setCreationDate(paper.getCreationDate());
        entity.setUpdatedAt(paper.getUpdatedAt());
    }

    public Paper toPaperDomain(PaperEntity entity) {
        return Paper.reconstitute(
                entity.getPaperId(),
                entity.getCompanyId(),
                entity.getName(),
                entity.getGrammage(),
                entity.getWidth(),
                entity.getHeight(),
                entity.getUnit(),
                entity.isCoated(),
                entity.isAcceptsRemnants(),
                entity.getMinRemnantWidth(),
                entity.getMinRemnantHeight(),
                entity.getMinRemnantUnit(),
                entity.isState(),
                entity.getCreationDate(),
                entity.getUpdatedAt()
        );
    }

    public PaperSupplierPriceEntity toNewPriceEntity(PaperSupplierPrice price) {
        PaperSupplierPriceEntity entity = new PaperSupplierPriceEntity();
        copyPriceScalars(price, entity);
        return entity;
    }

    public void copyPriceScalars(PaperSupplierPrice price, PaperSupplierPriceEntity entity) {
        entity.setPaperSupplierPriceId(price.getPaperSupplierPriceId());
        entity.setCompanyId(price.getCompanyId());
        entity.setPaperId(price.getPaperId());
        entity.setSupplierId(price.getSupplierId());
        entity.setSheetValue(price.getSheetValue());
        entity.setPackageUnit(price.getPackageUnit());
        entity.setFreightPerSheet(price.getFreightPerSheet());
        entity.setMinPurchaseSheets(price.getMinPurchaseSheets());
        entity.setPaymentDays(price.getPaymentDays());
        entity.setDeliveryDays(price.getDeliveryDays());
        entity.setPriceDate(price.getPriceDate());
        entity.setPreferred(price.isPreferred());
        entity.setState(price.isState());
        entity.setCreatedAt(price.getCreatedAt());
        entity.setUpdatedAt(price.getUpdatedAt());
    }

    public PaperSupplierPrice toPriceDomain(PaperSupplierPriceEntity entity) {
        return PaperSupplierPrice.reconstitute(
                entity.getPaperSupplierPriceId(),
                entity.getCompanyId(),
                entity.getPaperId(),
                entity.getSupplierId(),
                entity.getSheetValue(),
                entity.getPackageUnit(),
                entity.getFreightPerSheet(),
                entity.getMinPurchaseSheets(),
                entity.getPaymentDays(),
                entity.getDeliveryDays(),
                entity.getPriceDate(),
                entity.isPreferred(),
                entity.isState(),
                entity.getLandedCostPerSheet(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public PaperPriceHistory toPriceHistoryDomain(PaperPriceHistoryEntity entity) {
        return new PaperPriceHistory(
                entity.getPaperPriceHistoryId(),
                entity.getCompanyId(),
                entity.getPaperId(),
                entity.getSupplierId(),
                entity.getSheetValue(),
                entity.getPackageUnit(),
                entity.getFreightPerSheet(),
                entity.getMinPurchaseSheets(),
                entity.getPaymentDays(),
                entity.getDeliveryDays(),
                entity.getPriceDate(),
                entity.isPreferred(),
                entity.isState(),
                entity.getEffectiveFrom(),
                entity.getChangedBy()
        );
    }

    public PaperCutLayoutEntity toNewCutLayoutEntity(PaperCutLayout layout) {
        PaperCutLayoutEntity entity = new PaperCutLayoutEntity();
        copyCutLayoutScalars(layout, entity);
        return entity;
    }

    public void copyCutLayoutScalars(PaperCutLayout layout, PaperCutLayoutEntity entity) {
        entity.setPaperCutLayoutId(layout.getPaperCutLayoutId());
        entity.setCompanyId(layout.getCompanyId());
        entity.setPaperId(layout.getPaperId());
        entity.setCutLayoutId(layout.getCutLayoutId());
        entity.setOrientation(layout.getOrientation());
        entity.setWastePercentage(layout.getWastePercentage());
        entity.setNote(layout.getNote());
        entity.setState(layout.isState());
        entity.setCreatedAt(layout.getCreatedAt());
        entity.setUpdatedAt(layout.getUpdatedAt());
    }

    public PaperCutLayout toCutLayoutDomain(PaperCutLayoutEntity entity) {
        return PaperCutLayout.reconstitute(
                entity.getPaperCutLayoutId(),
                entity.getCompanyId(),
                entity.getPaperId(),
                entity.getCutLayoutId(),
                entity.getOrientation(),
                entity.getWastePercentage(),
                entity.getNote(),
                entity.isState(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public PaperStockEntity toNewStockEntity(PaperStock stock) {
        PaperStockEntity entity = new PaperStockEntity();
        copyStockScalars(stock, entity);
        return entity;
    }

    public void copyStockScalars(PaperStock stock, PaperStockEntity entity) {
        entity.setPaperStockId(stock.getPaperStockId());
        entity.setCompanyId(stock.getCompanyId());
        entity.setPaperId(stock.getPaperId());
        entity.setQuantityInitial(stock.getQuantityInitial());
        entity.setQuantityAvailable(stock.getQuantityAvailable());
        entity.setUnitCost(stock.getUnitCost());
        entity.setEntryDate(stock.getEntryDate());
        entity.setState(stock.isState());
        entity.setCreatedAt(stock.getCreatedAt());
        entity.setUpdatedAt(stock.getUpdatedAt());
    }

    public PaperStock toStockDomain(PaperStockEntity entity) {
        return PaperStock.reconstitute(
                entity.getPaperStockId(),
                entity.getCompanyId(),
                entity.getPaperId(),
                entity.getQuantityInitial(),
                entity.getQuantityAvailable(),
                entity.getUnitCost(),
                entity.getEntryDate(),
                entity.isState(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public PaperRemnantEntity toNewRemnantEntity(PaperRemnant remnant) {
        PaperRemnantEntity entity = new PaperRemnantEntity();
        copyRemnantScalars(remnant, entity);
        return entity;
    }

    public void copyRemnantScalars(PaperRemnant remnant, PaperRemnantEntity entity) {
        entity.setPaperRemnantId(remnant.getPaperRemnantId());
        entity.setCompanyId(remnant.getCompanyId());
        entity.setPaperId(remnant.getPaperId());
        entity.setWidth(remnant.getWidth());
        entity.setHeight(remnant.getHeight());
        entity.setUnit(remnant.getUnit());
        entity.setQuantityInitial(remnant.getQuantityInitial());
        entity.setQuantityAvailable(remnant.getQuantityAvailable());
        entity.setUnitCost(remnant.getUnitCost());
        entity.setSourceProductionOrderId(remnant.getSourceProductionOrderId());
        entity.setSourcePaperRowId(remnant.getSourcePaperRowId());
        entity.setEntryDate(remnant.getEntryDate());
        entity.setNote(remnant.getNote());
        entity.setState(remnant.isState());
        entity.setCreatedAt(remnant.getCreatedAt());
        entity.setUpdatedAt(remnant.getUpdatedAt());
    }

    public PaperRemnant toRemnantDomain(PaperRemnantEntity entity) {
        return PaperRemnant.reconstitute(
                entity.getPaperRemnantId(),
                entity.getCompanyId(),
                entity.getPaperId(),
                entity.getWidth(),
                entity.getHeight(),
                entity.getUnit(),
                entity.getQuantityInitial(),
                entity.getQuantityAvailable(),
                entity.getUnitCost(),
                entity.getSourceProductionOrderId(),
                entity.getSourcePaperRowId(),
                entity.getEntryDate(),
                entity.getNote(),
                entity.isState(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
