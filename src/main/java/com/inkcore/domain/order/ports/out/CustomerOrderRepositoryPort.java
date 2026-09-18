package com.inkcore.domain.order.ports.out;

import com.inkcore.domain.order.model.CustomerOrder;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CustomerOrderRepositoryPort {

    CustomerOrder save(CustomerOrder order);

    Optional<CustomerOrder> findByProductionOrderId(String companyId, String productionOrderId);

    List<CustomerOrder> findByProductionOrderIds(String companyId, Collection<String> productionOrderIds);

    long allocateNextOdpSequence(String companyId);
}
