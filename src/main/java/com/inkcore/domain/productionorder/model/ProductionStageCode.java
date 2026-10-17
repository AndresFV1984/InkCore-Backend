package com.inkcore.domain.productionorder.model;

import com.inkcore.domain.productionorder.exception.ProductionOrderBusinessRuleException;

import java.util.Arrays;

/** Etapas de planta que pueden tener un turno propio. */
public enum ProductionStageCode {
    PREPRENSA("preprensa"),
    CORTE_PAPEL("corte-papel"),
    IMPRESION("impresion"),
    TERMINADOS("terminados"),
    ACABADOS("acabados");

    private final String code;

    ProductionStageCode(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static ProductionStageCode fromCode(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ProductionOrderBusinessRuleException("La etapa es obligatoria");
        }
        return Arrays.stream(values())
                .filter(stage -> stage.code.equals(raw.trim()))
                .findFirst()
                .orElseThrow(() -> new ProductionOrderBusinessRuleException("La etapa no existe"));
    }
}
