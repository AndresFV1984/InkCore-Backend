package com.inkcore.application.order.usecase;

import com.inkcore.domain.order.model.DeliveryType;
import com.inkcore.domain.order.model.PaymentMethod;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderEnumsTest {

    @Test
    void deliveryType_parsesSpanishValues() {
        assertEquals(DeliveryType.PARCIAL, DeliveryType.fromValue("parcial"));
        assertEquals(DeliveryType.TOTAL, DeliveryType.fromValue("TOTAL"));
    }

    @Test
    void paymentMethod_rejectsUnknown() {
        assertThrows(IllegalArgumentException.class, () -> PaymentMethod.fromValue("bitcoin"));
    }
}
