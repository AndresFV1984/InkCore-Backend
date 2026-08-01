package com.inkcore.domain.inkestimation.ports.out;

import com.inkcore.domain.inkestimation.model.InkEstimateHistory;

public interface InkEstimateHistoryRepositoryPort {
    InkEstimateHistory save(InkEstimateHistory history);
}
