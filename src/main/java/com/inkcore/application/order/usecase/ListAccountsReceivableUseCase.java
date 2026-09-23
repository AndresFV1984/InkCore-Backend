package com.inkcore.application.order.usecase;

import com.inkcore.application.order.AbonosBalance;
import com.inkcore.application.order.OrderSupport;
import com.inkcore.domain.client.model.Client;
import com.inkcore.domain.client.ports.out.ClientRepositoryPort;
import com.inkcore.domain.order.model.AccountsReceivable;
import com.inkcore.domain.order.model.AccountsReceivableAging;
import com.inkcore.domain.order.model.CustomerOrder;
import com.inkcore.domain.order.model.DeliveryMovementType;
import com.inkcore.domain.order.model.OrderDelivery;
import com.inkcore.domain.order.ports.out.AccountsReceivableRepositoryPort;
import com.inkcore.domain.order.ports.out.OrderDeliveryRepositoryPort;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ListAccountsReceivableUseCase {

    private final OrderSupport support;
    private final AccountsReceivableRepositoryPort accountsReceivableRepository;
    private final OrderDeliveryRepositoryPort deliveryRepository;
    private final ClientRepositoryPort clientRepository;

    public ListAccountsReceivableUseCase(
            OrderSupport support,
            AccountsReceivableRepositoryPort accountsReceivableRepository,
            OrderDeliveryRepositoryPort deliveryRepository,
            ClientRepositoryPort clientRepository
    ) {
        this.support = support;
        this.accountsReceivableRepository = accountsReceivableRepository;
        this.deliveryRepository = deliveryRepository;
        this.clientRepository = clientRepository;
    }

    @Transactional(readOnly = true)
    public PageResult<AccountsReceivableRow> execute(
            String status,
            String clientId,
            String search,
            Boolean overdueOnly,
            Boolean dueSoonOnly,
            Boolean withBalance,
            PageQuery pageQuery,
            Authentication authentication
    ) {
        String companyId = support.companyId(authentication);
        PageQuery query = pageQuery == null ? PageQuery.of(0, PageQuery.DEFAULT_SIZE) : pageQuery;
        LocalDate today = LocalDate.now();

        boolean hasSearch = search != null && !search.isBlank();
        boolean filterAging = Boolean.TRUE.equals(overdueOnly) || Boolean.TRUE.equals(dueSoonOnly);
        boolean filterBalance = Boolean.TRUE.equals(withBalance);
        PageQuery fetchQuery = (hasSearch || filterAging || filterBalance)
                ? PageQuery.of(0, PageQuery.MAX_SIZE)
                : query;

        PageResult<AccountsReceivable> page = accountsReceivableRepository.findPage(companyId, status, clientId, fetchQuery);
        List<String> productionOrderIds = page.content().stream()
                .map(AccountsReceivable::getProductionOrderId)
                .toList();
        Map<String, String> odpByProductionOrderId = support.customerOrderRepository()
                .findByProductionOrderIds(companyId, productionOrderIds)
                .stream()
                .collect(Collectors.toMap(
                        CustomerOrder::getProductionOrderId,
                        CustomerOrder::getOdpNumber,
                        (a, b) -> a
                ));

        List<AccountsReceivableRow> rows = new ArrayList<>();
        for (AccountsReceivable summary : page.content()) {
            rows.add(toRow(
                    companyId,
                    summary,
                    today,
                    odpByProductionOrderId.get(summary.getProductionOrderId())
            ));
        }

        if (Boolean.TRUE.equals(overdueOnly)) {
            rows = rows.stream().filter(AccountsReceivableRow::overdue).toList();
        }
        if (Boolean.TRUE.equals(dueSoonOnly)) {
            rows = rows.stream().filter(AccountsReceivableRow::dueSoon).toList();
        }
        if (filterBalance) {
            rows = rows.stream()
                    .filter(row -> row.totalRemaining() != null && row.totalRemaining().compareTo(BigDecimal.ZERO) > 0)
                    .toList();
        }

        if (hasSearch) {
            String needle = search.trim().toLowerCase(Locale.ROOT);
            rows = rows.stream()
                    .filter(row -> contains(row.orderNumber(), needle)
                            || contains(row.odpNumber(), needle)
                            || contains(row.clientName(), needle)
                            || contains(row.cxcNumber(), needle)
                            || contains(row.abonosNumber(), needle)
                            || contains(row.lastDeliveryNumber(), needle)
                            || contains(row.lastPaymentNumber(), needle))
                    .toList();
        }

        if (hasSearch || filterAging || filterBalance) {
            int from = Math.min(query.offset(), rows.size());
            int to = Math.min(from + query.size(), rows.size());
            return new PageResult<>(rows.subList(from, to), query.page(), query.size(), rows.size());
        }

        return new PageResult<>(rows, page.page(), page.size(), page.totalElements());
    }

    private AccountsReceivableRow toRow(
            String companyId,
            AccountsReceivable summary,
            LocalDate today,
            String odpNumber
    ) {
        ProductionOrder order = support.productionOrderRepository()
                .findById(summary.getProductionOrderId())
                .orElse(null);
        String orderNumber = order == null ? null : order.getOrderNumber();
        String clientName = clientRepository.findById(summary.getClientId())
                .map(Client::getName)
                .orElse(null);
        AbonosBalance.applyTo(summary, order);
        AccountsReceivableAging.AgingSnapshot aging = AccountsReceivableAging.of(summary, today);
        return new AccountsReceivableRow(
                summary.getAccountsReceivableId(),
                summary.getCxcNumber(),
                summary.getAbonosNumber(),
                summary.getProductionOrderId(),
                orderNumber,
                odpNumber,
                summary.getClientId(),
                clientName,
                summary.getTotalUnits(),
                summary.getDeliveredUnits(),
                summary.getPendingUnits(),
                summary.getTotalOwed(),
                nullToZero(summary.getTotalPaid()),
                summary.getTotalRemaining(),
                nullToZero(summary.getTotalCashPaid()),
                nullToZero(summary.getTotalWithheld()),
                nullToZero(summary.getTotalAdvancePaid()),
                summary.getStatus() == null ? null : summary.getStatus().getDbValue(),
                summary.getOpenedAt(),
                summary.getDueDate(),
                summary.getPaymentTermDays(),
                aging.collectionStatus(),
                aging.daysOverdue(),
                aging.agingBucket(),
                aging.overdue(),
                aging.dueSoon(),
                resolveLastDeliveryNumber(companyId, summary.getProductionOrderId()),
                summary.getLastDeliveryAt(),
                summary.getLastPaymentNumber(),
                summary.getLastPaymentAt()
        );
    }

    /** Última entrega (no reversión); el repo ordena por deliveredAt desc. */
    private String resolveLastDeliveryNumber(String companyId, String productionOrderId) {
        return deliveryRepository.findByProductionOrderId(companyId, productionOrderId).stream()
                .filter(d -> d.getMovementType() == DeliveryMovementType.ENTREGA)
                .map(OrderDelivery::getDeliveryNumber)
                .filter(n -> n != null && !n.isBlank())
                .findFirst()
                .orElse(null);
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    private static BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public record AccountsReceivableRow(
            String accountsReceivableId,
            String cxcNumber,
            String abonosNumber,
            String productionOrderId,
            String orderNumber,
            String odpNumber,
            String clientId,
            String clientName,
            int totalUnits,
            int deliveredUnits,
            int pendingUnits,
            BigDecimal totalOwed,
            BigDecimal totalPaid,
            BigDecimal totalRemaining,
            BigDecimal totalCashPaid,
            BigDecimal totalWithheld,
            BigDecimal totalAdvancePaid,
            String status,
            LocalDateTime openedAt,
            LocalDate dueDate,
            int paymentTermDays,
            String collectionStatus,
            int daysOverdue,
            String agingBucket,
            boolean overdue,
            boolean dueSoon,
            String lastDeliveryNumber,
            LocalDateTime lastDeliveryAt,
            String lastPaymentNumber,
            LocalDateTime lastPaymentAt
    ) {
    }
}
