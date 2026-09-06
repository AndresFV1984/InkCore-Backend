package com.inkcore.infrastructure.out.persistence.station.adapter;

import com.inkcore.domain.station.ports.out.StationOperatorQueryPort;
import com.inkcore.infrastructure.out.persistence.productionorder.entity.ProductionOrderOperatorEntity;
import com.inkcore.infrastructure.out.persistence.productionorder.repository.JpaProductionOrderOperatorRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StationOperatorQueryAdapter implements StationOperatorQueryPort {

    private final JpaProductionOrderOperatorRepository operatorRepository;

    public StationOperatorQueryAdapter(JpaProductionOrderOperatorRepository operatorRepository) {
        this.operatorRepository = operatorRepository;
    }

    @Override
    public List<String> findProductionOrderIdsByOperator(String companyId, String userId) {
        return operatorRepository.findAllByCompanyIdAndUserId(companyId, userId).stream()
                .map(ProductionOrderOperatorEntity::getProductionOrderId)
                .distinct()
                .toList();
    }
}
