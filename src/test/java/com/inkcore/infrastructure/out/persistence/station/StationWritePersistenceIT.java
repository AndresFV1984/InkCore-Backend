package com.inkcore.infrastructure.out.persistence.station;

import com.inkcore.domain.station.model.StationIntervalKind;
import com.inkcore.domain.station.model.StationOperationInterval;
import com.inkcore.domain.station.model.StationProcessProgress;
import com.inkcore.domain.station.model.StationProcessStatus;
import com.inkcore.domain.station.ports.out.StationOperationIntervalRepositoryPort;
import com.inkcore.domain.station.ports.out.StationProcessProgressRepositoryPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class StationWritePersistenceIT {

    private static final String ORDER_ID = "ef658d09-bf30-43de-ba7a-edd0f14a61bd";
    private static final String COMPANY_ID = "company-seed-001";

    @Autowired
    StationOperationIntervalRepositoryPort intervalRepository;

    @Autowired
    StationProcessProgressRepositoryPort progressRepository;

    @Test
    void closingOpenIntervalUpdatesInsteadOfInserting() {
        StationOperationInterval open = intervalRepository.findOpenInterval(
                COMPANY_ID,
                "operator-seed-003",
                ORDER_ID,
                "preprensa",
                StationIntervalKind.LABOR
        ).orElseThrow();

        open.setEndedAt(LocalDateTime.now());
        open.setClosedByEventId(open.getOpenedByEventId());
        open.setOpen(false);
        open.setDurationMs(1_000L);

        StationOperationInterval saved = intervalRepository.save(open);
        assertFalse(saved.isOpen());
        assertEquals(open.getIntervalId(), saved.getIntervalId());
    }

    @Test
    void updatingExistingProgressDoesNotDuplicate() {
        StationProcessProgress progress = progressRepository
                .findByOrderAndProcessKey(ORDER_ID, "preprensa")
                .orElseThrow();

        progress.setCompletedUnits(10);
        progress.setStatus(StationProcessStatus.EN_PROCESO);
        progress.setUpdatedAt(LocalDateTime.now());

        StationProcessProgress saved = progressRepository.save(progress);
        assertEquals(10, saved.getCompletedUnits());
        assertTrue(progressRepository.findByOrderAndProcessKey(ORDER_ID, "preprensa").isPresent());
    }
}
