package com.inkcore.domain.productionorder.exception;

import com.inkcore.domain.shared.exception.DomainException;

/**
 * La versión enviada por el cliente no coincide con la persistida (bloqueo optimista).
 */
public class ProductionOrderVersionConflictException extends DomainException {

    private static final String CODE = "PRODUCTION_ORDER_VERSION_CONFLICT";
    private static final String MESSAGE = "La orden fue modificada por otro usuario";

    private final Long expectedVersion;
    private final Long actualVersion;

    public ProductionOrderVersionConflictException(Long expectedVersion, Long actualVersion) {
        super(CODE, MESSAGE);
        this.expectedVersion = expectedVersion;
        this.actualVersion = actualVersion;
    }

    public Long getExpectedVersion() {
        return expectedVersion;
    }

    public Long getActualVersion() {
        return actualVersion;
    }
}
