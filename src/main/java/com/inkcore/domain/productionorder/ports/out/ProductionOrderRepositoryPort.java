package com.inkcore.domain.productionorder.ports.out;

import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;

import java.util.Optional;

public interface ProductionOrderRepositoryPort {

    /**
     * Guarda la raíz y reemplaza por completo las colecciones del agregado.
     */
    ProductionOrder save(ProductionOrder productionOrder);

    /**
     * Actualiza solo la raíz ({@code status}, {@code state}, auditoría, etc.)
     * sin tocar planchas/corte/impresión/postprensa. Usado por archivar y
     * cambios de estado en planta.
     */
    ProductionOrder saveRoot(ProductionOrder productionOrder);

    /**
     * Carga el agregado completo (preprensa, cobro, operadores, descuentos,
     * planchas, corte, impresión y postprensa).
     */
    Optional<ProductionOrder> findById(String productionOrderId);

    /**
     * Carga únicamente la raíz, para validaciones y listados.
     */
    Optional<ProductionOrder> findSummaryById(String productionOrderId);

    PageResult<ProductionOrder> findPage(ProductionOrderFilter filter, PageQuery pageQuery);

    boolean existsByCompanyIdAndOrderNumber(String companyId, String orderNumber);

    /**
     * Reserva de forma atómica el siguiente consecutivo de {@code order_number}
     * para la empresa (sin colisiones bajo concurrencia).
     */
    long allocateNextOrderSequence(String companyId);

    boolean hasOperators(String productionOrderId);

    boolean isBillingCompleted(String productionOrderId);

    void deleteById(String productionOrderId);
}
