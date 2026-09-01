package com.inkcore.infrastructure.out.persistence.productionorder.adapter;

import com.inkcore.domain.productionorder.model.PaperRow;
import com.inkcore.domain.productionorder.model.PostpressLine;
import com.inkcore.domain.productionorder.model.PostpressRecord;
import com.inkcore.domain.productionorder.model.PrintConfig;
import com.inkcore.domain.productionorder.model.PrintEntry;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.productionorder.ports.out.ProductionOrderFilter;
import com.inkcore.domain.productionorder.ports.out.ProductionOrderRepositoryPort;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;
import com.inkcore.infrastructure.out.persistence.productionorder.entity.ProductionOrderBillingDetailsEntity;
import com.inkcore.infrastructure.out.persistence.productionorder.entity.ProductionOrderEntity;
import com.inkcore.infrastructure.out.persistence.productionorder.entity.ProductionOrderPostpressLineEntity;
import com.inkcore.infrastructure.out.persistence.productionorder.entity.ProductionOrderPostpressRecordEntity;
import com.inkcore.infrastructure.out.persistence.productionorder.entity.ProductionOrderPrepressDetailsEntity;
import com.inkcore.infrastructure.out.persistence.productionorder.entity.ProductionOrderPrintEntity;
import com.inkcore.infrastructure.out.persistence.productionorder.entity.ProductionOrderPrintEntryEntity;
import com.inkcore.infrastructure.out.persistence.productionorder.mapper.ProductionOrderPersistenceMapper;
import com.inkcore.infrastructure.out.persistence.productionorder.repository.JpaProductionOrderBillingDetailsRepository;
import com.inkcore.infrastructure.out.persistence.productionorder.repository.JpaProductionOrderOperatorRepository;
import com.inkcore.infrastructure.out.persistence.productionorder.repository.JpaProductionOrderPaperRowRepository;
import com.inkcore.infrastructure.out.persistence.productionorder.repository.JpaProductionOrderPlateRepository;
import com.inkcore.infrastructure.out.persistence.productionorder.repository.JpaProductionOrderPostpressLineRepository;
import com.inkcore.infrastructure.out.persistence.productionorder.repository.JpaProductionOrderPostpressRecordRepository;
import com.inkcore.infrastructure.out.persistence.productionorder.repository.JpaProductionOrderPrepressDetailsRepository;
import com.inkcore.infrastructure.out.persistence.productionorder.repository.JpaProductionOrderPrintEntryRepository;
import com.inkcore.infrastructure.out.persistence.productionorder.repository.JpaProductionOrderPrintRepository;
import com.inkcore.infrastructure.out.persistence.productionorder.repository.JpaProductionOrderRepository;
import com.inkcore.infrastructure.out.persistence.productionorder.repository.JpaProductionOrderStageDiscountRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Persistencia del agregado Orden de Producción.
 * <p>
 * Las colecciones se guardan con estrategia de reemplazo (borrar + insertar en
 * lote), igual que {@code PaperTypePersistenceAdapter.replaceAssignments}, para
 * no depender de {@code orphanRemoval} en listas potencialmente grandes.
 */
@Component
public class ProductionOrderPersistenceAdapter implements ProductionOrderRepositoryPort {

    private final JpaProductionOrderRepository orderRepository;
    private final JpaProductionOrderPrepressDetailsRepository prepressRepository;
    private final JpaProductionOrderBillingDetailsRepository billingRepository;
    private final JpaProductionOrderOperatorRepository operatorRepository;
    private final JpaProductionOrderStageDiscountRepository stageDiscountRepository;
    private final JpaProductionOrderPlateRepository plateRepository;
    private final JpaProductionOrderPaperRowRepository paperRowRepository;
    private final JpaProductionOrderPrintRepository printRepository;
    private final JpaProductionOrderPrintEntryRepository printEntryRepository;
    private final JpaProductionOrderPostpressRecordRepository postpressRecordRepository;
    private final JpaProductionOrderPostpressLineRepository postpressLineRepository;
    private final ProductionOrderPersistenceMapper mapper;
    private final Clock clock;

    @PersistenceContext
    private EntityManager entityManager;

    public ProductionOrderPersistenceAdapter(
            JpaProductionOrderRepository orderRepository,
            JpaProductionOrderPrepressDetailsRepository prepressRepository,
            JpaProductionOrderBillingDetailsRepository billingRepository,
            JpaProductionOrderOperatorRepository operatorRepository,
            JpaProductionOrderStageDiscountRepository stageDiscountRepository,
            JpaProductionOrderPlateRepository plateRepository,
            JpaProductionOrderPaperRowRepository paperRowRepository,
            JpaProductionOrderPrintRepository printRepository,
            JpaProductionOrderPrintEntryRepository printEntryRepository,
            JpaProductionOrderPostpressRecordRepository postpressRecordRepository,
            JpaProductionOrderPostpressLineRepository postpressLineRepository,
            ProductionOrderPersistenceMapper mapper,
            Clock clock
    ) {
        this.orderRepository = orderRepository;
        this.prepressRepository = prepressRepository;
        this.billingRepository = billingRepository;
        this.operatorRepository = operatorRepository;
        this.stageDiscountRepository = stageDiscountRepository;
        this.plateRepository = plateRepository;
        this.paperRowRepository = paperRowRepository;
        this.printRepository = printRepository;
        this.printEntryRepository = printEntryRepository;
        this.postpressRecordRepository = postpressRecordRepository;
        this.postpressLineRepository = postpressLineRepository;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ProductionOrder save(ProductionOrder order) {
        LocalDateTime now = LocalDateTime.now(clock);
        ProductionOrderEntity existing = orderRepository.findById(order.getProductionOrderId()).orElse(null);
        ProductionOrderEntity rootEntity;
        if (existing == null) {
            // version debe quedar null para que Spring Data/Hibernate traten el insert
            // como entidad nueva (ver comentario en ProductionOrderEntity).
            rootEntity = orderRepository.save(mapper.toNewEntity(order));
        } else {
            mapper.copyScalars(order, existing);
            rootEntity = orderRepository.save(existing);
        }

        String orderId = rootEntity.getProductionOrderId();
        replacePrepress(order, now);
        replaceBilling(order, now);

        // Colecciones con reemplazo total: borrar en lote, vaciar el PC y volver a insertar.
        // Sin clear(), las entidades cargadas en requireOrder/loadAggregate siguen managed
        // con el mismo @Id y el re-persist falla o deja el agregado inconsistente.
        clearOperators(orderId);
        clearStageDiscounts(orderId);
        clearPostpress(orderId);
        clearPrints(orderId);
        clearPaperRows(orderId);
        clearPlates(orderId);
        flushAndClearPersistenceContext();

        insertOperators(order, now);
        insertStageDiscounts(order, now);
        insertPlates(order, now);
        insertPaperRows(order, now);
        insertPrints(order, now);
        insertPostpress(order, now);

        orderRepository.flush();
        flushAndClearPersistenceContext();
        return orderRepository.findById(orderId)
                .map(this::loadAggregate)
                .orElseThrow(() -> new IllegalStateException(
                        "Orden de producción no encontrada tras guardar: " + orderId));
    }

    @Override
    @Transactional
    public ProductionOrder saveRoot(ProductionOrder order) {
        ProductionOrderEntity existing = orderRepository.findById(order.getProductionOrderId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Orden de producción no encontrada: " + order.getProductionOrderId()));
        mapper.copyScalars(order, existing);
        ProductionOrderEntity rootEntity = orderRepository.save(existing);
        orderRepository.flush();
        return loadAggregate(rootEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductionOrder> findById(String productionOrderId) {
        return orderRepository.findById(productionOrderId).map(this::loadAggregate);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductionOrder> findSummaryById(String productionOrderId) {
        return orderRepository.findById(productionOrderId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ProductionOrder> findPage(ProductionOrderFilter filter, PageQuery pageQuery) {
        Page<ProductionOrderEntity> page = orderRepository.findAll(toSpecification(filter), pageable(pageQuery));
        List<ProductionOrder> orders = page.getContent().stream().map(mapper::toDomain).toList();
        attachOperators(orders);
        return new PageResult<>(
                orders,
                pageQuery.page(),
                pageQuery.size(),
                page.getTotalElements()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCompanyIdAndOrderNumber(String companyId, String orderNumber) {
        return companyId != null
                && orderNumber != null
                && orderRepository.existsByCompanyIdAndOrderNumber(companyId.trim(), orderNumber.trim());
    }

    @Override
    @Transactional
    public long allocateNextOrderSequence(String companyId) {
        if (companyId == null || companyId.isBlank()) {
            throw new IllegalArgumentException("companyId es obligatorio para asignar order_number");
        }
        Object result = entityManager.createNativeQuery("""
                INSERT INTO indicolors.production_order_number_sequences (company_id, last_value)
                SELECT :companyId,
                       COALESCE(
                           (SELECT MAX(CAST(substring(po.order_number FROM 4) AS BIGINT))
                            FROM indicolors.production_orders po
                            WHERE po.company_id = :companyId
                              AND po.order_number ~ '^OP-[0-9]+$'),
                           0
                       ) + 1
                ON CONFLICT (company_id) DO UPDATE
                SET last_value = production_order_number_sequences.last_value + 1
                RETURNING last_value
                """)
                .setParameter("companyId", companyId.trim())
                .getSingleResult();
        return ((Number) result).longValue();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasOperators(String productionOrderId) {
        return operatorRepository.existsByProductionOrderId(productionOrderId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBillingCompleted(String productionOrderId) {
        return billingRepository.findById(productionOrderId)
                .map(entity -> entity.getBillingCompletedAt() != null)
                .orElse(false);
    }

    @Override
    @Transactional
    public void deleteById(String productionOrderId) {
        orderRepository.deleteById(productionOrderId);
    }

    /**
     * El listado paginado no carga el agregado completo; sí necesita operadores
     * para que el front filtre órdenes asignadas (Estación operario).
     */
    private void attachOperators(List<ProductionOrder> orders) {
        if (orders.isEmpty()) {
            return;
        }
        List<String> orderIds = orders.stream()
                .map(ProductionOrder::getProductionOrderId)
                .filter(id -> id != null && !id.isBlank())
                .toList();
        if (orderIds.isEmpty()) {
            return;
        }
        Map<String, List<com.inkcore.domain.productionorder.model.OperatorAssignment>> byOrder =
                new LinkedHashMap<>();
        for (var entity : operatorRepository.findAllByProductionOrderIdIn(orderIds)) {
            byOrder.computeIfAbsent(entity.getProductionOrderId(), key -> new ArrayList<>())
                    .add(mapper.toDomain(entity));
        }
        for (ProductionOrder order : orders) {
            order.setOperators(byOrder.getOrDefault(order.getProductionOrderId(), List.of()));
        }
    }

    private ProductionOrder loadAggregate(ProductionOrderEntity rootEntity) {
        String orderId = rootEntity.getProductionOrderId();
        ProductionOrder order = mapper.toDomain(rootEntity);

        prepressRepository.findById(orderId)
                .map(mapper::toDomain)
                .ifPresent(order::setPrepress);
        billingRepository.findById(orderId)
                .map(mapper::toDomain)
                .ifPresent(order::setBilling);

        order.setOperators(mapper.mapAll(
                operatorRepository.findAllByProductionOrderId(orderId), mapper::toDomain));
        order.setStageDiscounts(mapper.mapAll(
                stageDiscountRepository.findAllByProductionOrderId(orderId), mapper::toDomain));
        order.setPlates(mapper.mapAll(
                plateRepository.findAllByProductionOrderIdOrderByCreatedAtAsc(orderId), mapper::toDomain));
        order.setPaperRows(mapper.mapAll(
                paperRowRepository.findAllByProductionOrderIdOrderByCreatedAtAsc(orderId), mapper::toDomain));

        order.setPrints(loadPrints(orderId));
        order.setPostpressRecords(loadPostpressRecords(orderId));
        return order;
    }

    /**
     * Dos consultas para toda la impresión (cabeceras + entradas) en vez de una
     * por plancha.
     */
    private List<PrintConfig> loadPrints(String orderId) {
        List<ProductionOrderPrintEntity> printEntities =
                printRepository.findAllByProductionOrderIdOrderByCreatedAtAsc(orderId);
        if (printEntities.isEmpty()) {
            return List.of();
        }
        List<PrintConfig> prints = mapper.mapAll(printEntities, mapper::toDomain);
        List<String> printIds = prints.stream().map(PrintConfig::getPrintId).toList();

        Map<String, List<PrintEntry>> entriesByPrint = new LinkedHashMap<>();
        for (ProductionOrderPrintEntryEntity entryEntity
                : printEntryRepository.findAllByPrintIdInOrderByCreatedAtAsc(printIds)) {
            entriesByPrint
                    .computeIfAbsent(entryEntity.getPrintId(), key -> new ArrayList<>())
                    .add(mapper.toDomain(entryEntity));
        }
        for (PrintConfig print : prints) {
            print.setEntries(entriesByPrint.getOrDefault(print.getPrintId(), List.of()));
        }
        return prints;
    }

    private List<PostpressRecord> loadPostpressRecords(String orderId) {
        List<ProductionOrderPostpressRecordEntity> recordEntities =
                postpressRecordRepository.findAllByProductionOrderIdOrderByCreatedAtAsc(orderId);
        if (recordEntities.isEmpty()) {
            return List.of();
        }
        List<PostpressRecord> records = mapper.mapAll(recordEntities, mapper::toDomain);
        List<String> recordIds = records.stream().map(PostpressRecord::getRecordId).toList();

        Map<String, List<PostpressLine>> linesByRecord = new LinkedHashMap<>();
        for (ProductionOrderPostpressLineEntity lineEntity
                : postpressLineRepository.findAllByRecordIdInOrderByCreatedAtAsc(recordIds)) {
            linesByRecord
                    .computeIfAbsent(lineEntity.getRecordId(), key -> new ArrayList<>())
                    .add(mapper.toDomain(lineEntity));
        }
        for (PostpressRecord record : records) {
            record.setLines(linesByRecord.getOrDefault(record.getRecordId(), List.of()));
        }
        return records;
    }

    private void replacePrepress(ProductionOrder order, LocalDateTime now) {
        if (order.getPrepress() == null) {
            return;
        }
        ProductionOrderPrepressDetailsEntity existing =
                prepressRepository.findById(order.getProductionOrderId()).orElse(null);
        prepressRepository.save(mapper.toEntity(order.getPrepress(), existing, now));
    }

    private void replaceBilling(ProductionOrder order, LocalDateTime now) {
        if (order.getBilling() == null) {
            return;
        }
        ProductionOrderBillingDetailsEntity existing =
                billingRepository.findById(order.getProductionOrderId()).orElse(null);
        billingRepository.save(mapper.toEntity(order.getBilling(), existing, now));
    }

    private void clearOperators(String orderId) {
        operatorRepository.deleteAllByProductionOrderId(orderId);
        operatorRepository.flush();
    }

    private void insertOperators(ProductionOrder order, LocalDateTime now) {
        if (order.getOperators().isEmpty()) {
            return;
        }
        operatorRepository.saveAll(order.getOperators().stream()
                .map(operator -> mapper.toEntity(operator, now))
                .toList());
    }

    private void clearStageDiscounts(String orderId) {
        stageDiscountRepository.deleteAllByProductionOrderId(orderId);
        stageDiscountRepository.flush();
    }

    private void insertStageDiscounts(ProductionOrder order, LocalDateTime now) {
        if (order.getStageDiscounts().isEmpty()) {
            return;
        }
        stageDiscountRepository.saveAll(order.getStageDiscounts().stream()
                .map(discount -> mapper.toEntity(discount, now))
                .toList());
    }

    private void clearPlates(String orderId) {
        plateRepository.deleteAllByProductionOrderId(orderId);
        plateRepository.flush();
    }

    private void insertPlates(ProductionOrder order, LocalDateTime now) {
        if (order.getPlates().isEmpty()) {
            return;
        }
        plateRepository.saveAll(order.getPlates().stream()
                .map(plate -> mapper.toEntity(plate, now))
                .toList());
        plateRepository.flush();
    }

    /**
     * Self-FK {@code parent_row_id}: primero se anulan referencias, luego se borra.
     * Un único {@code DELETE WHERE order_id} puede fallar si PostgreSQL elimina el
     * padre antes que la fila de faltante.
     */
    private void clearPaperRows(String orderId) {
        entityManager.createQuery("""
                update ProductionOrderPaperRowEntity r
                set r.parentRowId = null
                where r.productionOrderId = :orderId
                  and r.parentRowId is not null
                """)
                .setParameter("orderId", orderId)
                .executeUpdate();
        paperRowRepository.deleteAllByProductionOrderId(orderId);
        paperRowRepository.flush();
    }

    private void insertPaperRows(ProductionOrder order, LocalDateTime now) {
        if (order.getPaperRows().isEmpty()) {
            return;
        }
        // Padres (sin parentRowId) primero para respetar el self-FK al insertar.
        List<PaperRow> ordered = order.getPaperRows().stream()
                .sorted((a, b) -> {
                    boolean aChild = a.getParentRowId() != null && !a.getParentRowId().isBlank();
                    boolean bChild = b.getParentRowId() != null && !b.getParentRowId().isBlank();
                    return Boolean.compare(aChild, bChild);
                })
                .toList();
        paperRowRepository.saveAll(ordered.stream()
                .map(row -> mapper.toEntity(row, now))
                .toList());
        paperRowRepository.flush();
    }

    private void flushAndClearPersistenceContext() {
        entityManager.flush();
        entityManager.clear();
    }

    private void clearPrints(String orderId) {
        List<ProductionOrderPrintEntity> currentPrints =
                printRepository.findAllByProductionOrderIdOrderByCreatedAtAsc(orderId);
        if (!currentPrints.isEmpty()) {
            printEntryRepository.deleteAllByPrintIdIn(currentPrints.stream()
                    .map(ProductionOrderPrintEntity::getProductionOrderPrintId)
                    .toList());
            printEntryRepository.flush();
        }
        printRepository.deleteAllByProductionOrderId(orderId);
        printRepository.flush();
    }

    private void insertPrints(ProductionOrder order, LocalDateTime now) {
        if (order.getPrints().isEmpty()) {
            return;
        }
        printRepository.saveAll(order.getPrints().stream()
                .map(print -> mapper.toEntity(print, now))
                .toList());
        printRepository.flush();

        List<ProductionOrderPrintEntryEntity> entryEntities = new ArrayList<>();
        for (PrintConfig print : order.getPrints()) {
            for (PrintEntry entry : print.getEntries()) {
                entry.setPrintId(print.getPrintId());
                entry.setCompanyId(print.getCompanyId());
                entryEntities.add(mapper.toEntity(entry, now));
            }
        }
        if (!entryEntities.isEmpty()) {
            printEntryRepository.saveAll(entryEntities);
        }
    }

    private void clearPostpress(String orderId) {
        List<ProductionOrderPostpressRecordEntity> currentRecords =
                postpressRecordRepository.findAllByProductionOrderIdOrderByCreatedAtAsc(orderId);
        if (!currentRecords.isEmpty()) {
            postpressLineRepository.deleteAllByRecordIdIn(currentRecords.stream()
                    .map(ProductionOrderPostpressRecordEntity::getProductionOrderPostpressRecordId)
                    .toList());
            postpressLineRepository.flush();
        }
        postpressRecordRepository.deleteAllByProductionOrderId(orderId);
        postpressRecordRepository.flush();
    }

    private void insertPostpress(ProductionOrder order, LocalDateTime now) {
        if (order.getPostpressRecords().isEmpty()) {
            return;
        }
        postpressRecordRepository.saveAll(order.getPostpressRecords().stream()
                .map(record -> mapper.toEntity(record, now))
                .toList());
        postpressRecordRepository.flush();

        List<ProductionOrderPostpressLineEntity> lineEntities = new ArrayList<>();
        for (PostpressRecord record : order.getPostpressRecords()) {
            for (PostpressLine line : record.getLines()) {
                line.setRecordId(record.getRecordId());
                line.setCompanyId(record.getCompanyId());
                lineEntities.add(mapper.toEntity(line, now));
            }
        }
        if (!lineEntities.isEmpty()) {
            postpressLineRepository.saveAll(lineEntities);
        }
    }

    private static Specification<ProductionOrderEntity> toSpecification(ProductionOrderFilter filter) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("companyId"), filter.companyId()));
            if (filter.status() != null && !filter.status().isBlank()) {
                predicates.add(builder.equal(root.get("status"), filter.status().trim()));
            }
            if (filter.clientId() != null && !filter.clientId().isBlank()) {
                predicates.add(builder.equal(root.get("clientId"), filter.clientId().trim()));
            }
            if (filter.orderNumber() != null && !filter.orderNumber().isBlank()) {
                String pattern = "%" + escapeLike(filter.orderNumber().trim().toLowerCase(Locale.ROOT)) + "%";
                predicates.add(builder.like(builder.lower(root.get("orderNumber")), pattern, '\\'));
            }
            if (filter.fromDate() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("orderDate"), filter.fromDate()));
            }
            if (filter.toDate() != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("orderDate"), filter.toDate()));
            }
            if (filter.state() != null) {
                predicates.add(builder.equal(root.get("state"), filter.state()));
            }
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static PageRequest pageable(PageQuery pageQuery) {
        return PageRequest.of(
                pageQuery.page(),
                pageQuery.size(),
                Sort.by(Sort.Order.desc("orderDate"), Sort.Order.desc("createdAt"))
        );
    }
}
