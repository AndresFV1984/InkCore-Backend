package com.inkcore.domain.productionorder.model;

import com.inkcore.domain.productionorder.exception.ProductionOrderBusinessRuleException;

/**
 * Motivo estructurado de merma al cerrar una fase. Se persiste como texto
 * legible en {@code production_order_waste_records.note} y se devuelve
 * con este código en la respuesta del evento.
 */
public enum WasteReason {

    PAPEL_MALA_CALIDAD("papel_mala_calidad", "Papel de mala calidad"),
    MAL_CORTADO("mal_cortado", "Mal cortado"),
    AJUSTE_REGISTRO_COLOR("ajuste_registro_color", "Ajuste de registro/color"),
    CAMBIO_MEDIO_TIRO("cambio_medio_tiro", "Cambio a medio tiro"),
    DEFECTO_IMPRESION("defecto_impresion", "Defecto de impresión"),
    OTRO("otro", "Otro");

    private final String apiValue;
    private final String label;

    WasteReason(String apiValue, String label) {
        this.apiValue = apiValue;
        this.label = label;
    }

    public String getApiValue() {
        return apiValue;
    }

    public String getLabel() {
        return label;
    }

    public static WasteReason fromApiValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        for (WasteReason reason : values()) {
            if (reason.apiValue.equals(normalized)) {
                return reason;
            }
        }
        throw new ProductionOrderBusinessRuleException(
                "wasteReason: papel_mala_calidad | mal_cortado | ajuste_registro_color | "
                        + "cambio_medio_tiro | defecto_impresion | otro"
        );
    }
}
