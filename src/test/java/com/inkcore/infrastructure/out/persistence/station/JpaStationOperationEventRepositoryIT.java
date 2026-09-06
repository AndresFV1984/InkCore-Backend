package com.inkcore.infrastructure.out.persistence.station;

import com.inkcore.infrastructure.out.persistence.station.repository.JpaStationOperationEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
class JpaStationOperationEventRepositoryIT {

    @Autowired
    JpaStationOperationEventRepository repository;

    @Test
    void searchWithOptionalFilters() {
        repository.findAll(
                (root, query, builder) -> builder.equal(root.get("companyId"), "company-seed-001"),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "occurredAt"))
        );
    }
}
