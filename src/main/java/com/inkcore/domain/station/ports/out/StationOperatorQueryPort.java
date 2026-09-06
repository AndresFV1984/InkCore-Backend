package com.inkcore.domain.station.ports.out;

import java.util.List;

public interface StationOperatorQueryPort {

    List<String> findProductionOrderIdsByOperator(String companyId, String userId);
}
