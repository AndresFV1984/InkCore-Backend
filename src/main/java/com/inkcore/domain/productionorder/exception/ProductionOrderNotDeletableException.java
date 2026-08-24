package com.inkcore.domain.productionorder.exception;

import com.inkcore.domain.shared.exception.DomainException;

/**
 * El borrado físico solo aplica a órdenes que ningún operador tomó y que no se
 * han cobrado; en el resto de casos se usa la baja lógica (state=false).
 */
public class ProductionOrderNotDeletableException extends DomainException {

    private static final String CODE = "PRODUCTION_ORDER_NOT_DELETABLE";
    private static final String MESSAGE =
            "No se puede eliminar: la orden ya fue iniciada por un operador o ya fue cobrada";

    public ProductionOrderNotDeletableException() {
        super(CODE, MESSAGE);
    }
}
