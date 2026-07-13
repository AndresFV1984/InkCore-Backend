package com.indicore.application.order.usecase;

import com.indicore.domain.order.model.Order;
import com.indicore.domain.order.ports.out.OrderRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ListOrdersUseCase {

    private final OrderRepositoryPort orderRepository;

    public ListOrdersUseCase(OrderRepositoryPort orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public List<Order> execute() {
        return orderRepository.findAll();
    }
}
