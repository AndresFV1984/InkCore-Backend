package com.inkcore.domain.colorconversion.ports.out;

import com.inkcore.domain.colorconversion.model.ConversionHistory;

public interface ConversionHistoryRepositoryPort {

    ConversionHistory save(ConversionHistory history);
}
