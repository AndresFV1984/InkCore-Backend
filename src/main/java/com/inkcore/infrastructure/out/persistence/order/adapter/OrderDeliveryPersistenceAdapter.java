package com.inkcore.infrastructure.out.persistence.order.adapter;

import com.inkcore.domain.order.model.OrderDelivery;
import com.inkcore.domain.order.ports.out.OrderDeliveryRepositoryPort;
import com.inkcore.infrastructure.out.persistence.order.mapper.OrderPersistenceMapper;
import com.inkcore.infrastructure.out.persistence.order.repository.JpaOrderDeliveryRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class OrderDeliveryPersistenceAdapter implements OrderDeliveryRepositoryPort {

    private final JpaOrderDeliveryRepository repository;

    public OrderDeliveryPersistenceAdapter(JpaOrderDeliveryRepository repository) {
        this.repository = repository;
    }

    @Override
    public OrderDelivery save(OrderDelivery delivery) {
        return OrderPersistenceMapper.toDomain(
                repository.saveAndFlush(OrderPersistenceMapper.toEntity(delivery))
        );
    }

    @Override
    public Optional<OrderDelivery> findById(String companyId, String orderDeliveryId) {
        return repository.findByCompanyIdAndOrderDeliveryId(companyId, orderDeliveryId)
                .map(OrderPersistenceMapper::toDomain);
    }

    @Override
    public List<OrderDelivery> findByProductionOrderId(String companyId, String productionOrderId) {
        return repository.findAllByCompanyIdAndProductionOrderIdOrderByDeliveredAtDesc(companyId, productionOrderId)
                .stream()
                .map(OrderPersistenceMapper::toDomain)
                .toList();
    }
}
