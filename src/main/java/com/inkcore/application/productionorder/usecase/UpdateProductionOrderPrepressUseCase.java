package com.inkcore.application.productionorder.usecase;

import com.inkcore.domain.assemblyprice.model.AssemblyPrice;
import com.inkcore.domain.assemblyprice.ports.out.AssemblyPriceRepositoryPort;
import com.inkcore.domain.platetype.model.PlateType;
import com.inkcore.domain.platetype.ports.out.PlateTypeRepositoryPort;
import com.inkcore.domain.productionorder.exception.ProductionOrderBusinessRuleException;
import com.inkcore.domain.productionorder.model.ClientPlateType;
import com.inkcore.domain.productionorder.model.DiscountType;
import com.inkcore.domain.productionorder.model.Plate;
import com.inkcore.domain.productionorder.model.PrepressDetails;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.productionorder.model.ProductionOrderStage;
import com.inkcore.domain.productionorder.service.ProductionOrderCalculator;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class UpdateProductionOrderPrepressUseCase {

    private final ProductionOrderSupport support;
    private final PlateTypeRepositoryPort plateTypeRepository;
    private final AssemblyPriceRepositoryPort assemblyPriceRepository;

    public UpdateProductionOrderPrepressUseCase(
            ProductionOrderSupport support,
            PlateTypeRepositoryPort plateTypeRepository,
            AssemblyPriceRepositoryPort assemblyPriceRepository
    ) {
        this.support = support;
        this.plateTypeRepository = plateTypeRepository;
        this.assemblyPriceRepository = assemblyPriceRepository;
    }

    @Transactional
    public ProductionOrder execute(
            String productionOrderId,
            UpdateProductionOrderPrepressCommand command,
            Authentication authentication
    ) {
        String companyId = support.companyId(authentication);
        String userId = support.userId(authentication);
        ProductionOrder order = support.requireOrder(productionOrderId, companyId);
        support.assertVersion(order, command.version());

        validateRules(command, companyId);

        PrepressDetails prepress = order.getPrepress() == null ? new PrepressDetails() : order.getPrepress();
        prepress.setProductionOrderId(order.getProductionOrderId());
        prepress.setCompanyId(companyId);
        prepress.setNewDesign(command.isNewDesign());
        if (Boolean.TRUE.equals(command.isNewDesign())) {
            prepress.setDesignName(trimToNull(command.designName()));
            prepress.setExistingDesignOrderId(null);
            prepress.setClientPlateType(null);
        } else {
            prepress.setDesignName(null);
            prepress.setExistingDesignOrderId(trimToNull(command.existingDesignOrderId()));
            prepress.setClientPlateType(ClientPlateType.fromValue(command.clientPlateType()));
        }
        prepress.setHasDesignCost(Boolean.TRUE.equals(command.hasDesignCost()));
        prepress.setDesignCost(prepress.isHasDesignCost() ? command.designCost() : null);
        prepress.setClientSuppliesPlates(command.clientSuppliesPlates());
        prepress.setNewPlateCost(command.newPlateCost());
        prepress.setDieCutLine(Boolean.TRUE.equals(command.dieCutLine()));
        prepress.setUvReserve(Boolean.TRUE.equals(command.uvReserve()));
        prepress.setStamping(Boolean.TRUE.equals(command.stamping()));
        prepress.setEmbossing(Boolean.TRUE.equals(command.embossing()));
        prepress.setPrepressDiscountType(DiscountType.fromValue(command.prepressDiscountType()));
        prepress.setPrepressDiscountValue(command.prepressDiscountValue());

        if (command.assemblyPriceId() != null && !command.assemblyPriceId().isBlank()) {
            AssemblyPrice assembly = assemblyPriceRepository.findById(command.assemblyPriceId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "ASSEMBLY_PRICE_NOT_FOUND", "Precio de montaje no encontrado"));
            if (!companyId.equals(assembly.getCompanyId())) {
                throw new ResourceNotFoundException("ASSEMBLY_PRICE_NOT_FOUND", "Precio de montaje no encontrado");
            }
            prepress.setAssemblyPriceId(assembly.getAssemblyPriceId());
            prepress.setAssemblyPriceName(assembly.getName());
            prepress.setAssemblyPriceCost(assembly.getCost());
        } else {
            prepress.setAssemblyPriceId(null);
            prepress.setAssemblyPriceName(null);
            prepress.setAssemblyPriceCost(null);
        }

        List<Plate> plates = buildPlates(order, companyId, command.plates());
        prepress.setTotalPlatesValue(ProductionOrderCalculator.calculateTotalPlatesValue(plates));
        if (Boolean.TRUE.equals(command.completed())) {
            prepress.setPrepressCompletedAt(support.now());
        }

        order.setPrepress(prepress);
        order.setPlates(plates);
        // Al reemplazar planchas se invalidan hijos dependientes (corte/impresión/postprensa).
        order.setPaperRows(List.of());
        order.setPrints(List.of());
        order.setPostpressRecords(List.of());
        order.upsertOperator(ProductionOrderStage.PREPRESS, command.operatorUserId());
        order.setUpdatedAt(support.now());
        order.setUpdatedBy(userId);
        return support.repository().save(order);
    }

    private void validateRules(UpdateProductionOrderPrepressCommand command, String companyId) {
        List<String> errors = new ArrayList<>();
        if (command.isNewDesign() == null) {
            errors.add("isNewDesign: obligatorio");
        } else if (Boolean.FALSE.equals(command.isNewDesign())) {
            if (command.existingDesignOrderId() == null || command.existingDesignOrderId().isBlank()) {
                errors.add("existingDesignOrderId: obligatorio cuando isNewDesign=false");
            } else {
                ProductionOrder existing = support.repository().findSummaryById(command.existingDesignOrderId())
                        .orElse(null);
                if (existing == null || !companyId.equals(existing.getCompanyId())) {
                    errors.add("existingDesignOrderId: orden de diseño no encontrada");
                }
            }
            if (command.clientPlateType() == null || command.clientPlateType().isBlank()) {
                errors.add("clientPlateType: obligatorio cuando isNewDesign=false");
            } else {
                try {
                    ClientPlateType.fromValue(command.clientPlateType());
                } catch (IllegalArgumentException ex) {
                    errors.add("clientPlateType: valor inválido");
                }
            }
        }
        if (Boolean.TRUE.equals(command.hasDesignCost()) && command.designCost() == null) {
            errors.add("designCost: obligatorio cuando hasDesignCost=true");
        }
        if (command.plates() != null) {
            for (int i = 0; i < command.plates().size(); i++) {
                var plate = command.plates().get(i);
                if (plate.colors() == null || plate.colors().isBlank()) {
                    errors.add("plates[" + i + "].colors: obligatorio");
                }
                if (plate.quantity() == null || plate.quantity() <= 0) {
                    errors.add("plates[" + i + "].quantity: debe ser mayor que 0");
                }
                if (Boolean.TRUE.equals(plate.plateReplacement())
                        && (plate.replacementQuantity() == null || plate.replacementQuantity() <= 0)) {
                    errors.add("plates[" + i + "].replacementQuantity: obligatorio cuando plateReplacement=true");
                }
            }
        }
        if (!errors.isEmpty()) {
            throw new ProductionOrderBusinessRuleException("Regla de negocio incumplida", errors);
        }
    }

    private List<Plate> buildPlates(
            ProductionOrder order,
            String companyId,
            List<UpdateProductionOrderPrepressCommand.PlateInput> inputs
    ) {
        if (inputs == null || inputs.isEmpty()) {
            return List.of();
        }
        List<Plate> plates = new ArrayList<>();
        for (UpdateProductionOrderPrepressCommand.PlateInput input : inputs) {
            Plate plate = new Plate();
            if (input.plateId() != null && !input.plateId().isBlank()) {
                plate.setPlateId(input.plateId().trim());
            }
            plate.setCompanyId(companyId);
            plate.setProductionOrderId(order.getProductionOrderId());
            plate.setColors(input.colors().trim());
            plate.setQuantity(input.quantity());
            plate.setCavities(input.cavities());
            plate.setSurplus(input.surplus());
            plate.setPlatesCount(input.platesCount());
            plate.setDetail(trimToNull(input.detail()));
            plate.setObservation(trimToNull(input.observation()));
            plate.setManualEntry(Boolean.TRUE.equals(input.manualEntry()));
            plate.setPlateSupply(trimToNull(input.plateSupply()));
            plate.setPlateReplacement(Boolean.TRUE.equals(input.plateReplacement()));
            plate.setReplacementQuantity(plate.isPlateReplacement() ? input.replacementQuantity() : null);

            if (input.plateTypeId() != null && !input.plateTypeId().isBlank()) {
                PlateType type = plateTypeRepository.findById(input.plateTypeId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "PLATE_TYPE_NOT_FOUND", "Tipo de plancha no encontrado"));
                if (!companyId.equals(type.getCompanyId())) {
                    throw new ResourceNotFoundException("PLATE_TYPE_NOT_FOUND", "Tipo de plancha no encontrado");
                }
                plate.setPlateTypeId(type.getPlateTypeId());
                plate.setPlateName(type.getName());
                plate.setPlateSize(formatSize(type.getWidth(), type.getHeight(), type.getUnit()));
                plate.setPlatePrice(type.getValue());
            } else {
                plate.setPlateTypeId(null);
                plate.setPlateName(trimToNull(input.plateName()));
                plate.setPlateSize(trimToNull(input.plateSize()));
                plate.setPlatePrice(input.platePrice());
            }

            plate.setGoodSizes(ProductionOrderCalculator.calculateGoodSizes(plate.getQuantity(), plate.getCavities()));
            plate.setTotalValue(ProductionOrderCalculator.calculatePlateTotalValue(
                    plate.getPlatePrice(),
                    Objects.requireNonNullElse(plate.getPlatesCount(), 1)
            ));
            plates.add(plate);
        }
        return plates;
    }

    private static String formatSize(BigDecimal width, BigDecimal height, String unit) {
        if (width == null || height == null) {
            return null;
        }
        String u = unit == null ? "" : unit;
        return width.stripTrailingZeros().toPlainString() + "x"
                + height.stripTrailingZeros().toPlainString() + u;
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
