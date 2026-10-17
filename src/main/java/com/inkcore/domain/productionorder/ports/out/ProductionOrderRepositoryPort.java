package com.inkcore.domain.productionorder.ports.out;

import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;

import java.time.LocalDateTime;
import java.util.List;
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

    /**
     * Siguiente posición al final de la cola de la empresa (MAX + 1).
     */
    int allocateNextPriorityRank(String companyId);

    /**
     * Órdenes abiertas (activas, no finalizadas ni anuladas), de la primera a la última.
     */
    List<String> findOpenWorkQueueIds(String companyId);

    /**
     * Órdenes activas en proceso (cualquier etapa de planta), sin orden definido.
     */
    List<String> findInProgressIds(String companyId);

    /**
     * Reemplaza la posición de trabajo sin tocar {@code version}.
     * Reordenar no debe invalidar el guardado del wizard.
     */
    void replaceWorkQueuePriority(
            String companyId,
            List<String> orderedProductionOrderIds,
            String updatedBy,
            LocalDateTime updatedAt
    );

    boolean hasOperators(String productionOrderId);

    boolean isBillingCompleted(String productionOrderId);

    void deleteById(String productionOrderId);
}
