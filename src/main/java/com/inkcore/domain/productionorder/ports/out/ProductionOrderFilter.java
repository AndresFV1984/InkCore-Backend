package com.inkcore.domain.productionorder.ports.out;

import java.time.LocalDate;

/**
 * Filtros del listado de órdenes. {@code companyId} siempre viene del usuario
 * autenticado, nunca del cliente.
 */
public record ProductionOrderFilter(
        String companyId,
        String status,
        String clientId,
        String orderNumber,
        LocalDate fromDate,
        LocalDate toDate,
        Boolean state
) {
}
