package com.inkcore.domain.station.exception;

import com.inkcore.domain.shared.exception.DomainException;

import java.util.List;

public class StationBusinessRuleException extends DomainException {

    private static final String CODE = "STATION_BUSINESS_RULE";

    private final List<String> errors;

    public StationBusinessRuleException(String message) {
        super(CODE, message);
        this.errors = List.of();
    }

    public StationBusinessRuleException(String message, List<String> errors) {
        super(CODE, message);
        this.errors = errors == null ? List.of() : List.copyOf(errors);
    }

    public List<String> getErrors() {
        return errors;
    }
}
