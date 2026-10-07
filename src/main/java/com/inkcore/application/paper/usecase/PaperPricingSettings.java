package com.inkcore.application.paper.usecase;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PaperPricingSettings {

    private final int priceStaleDays;
    private final BigDecimal ivaRate;
    private final boolean ivaDeductible;

    public PaperPricingSettings(
            @Value("${inkcore.paper.price-stale-days:30}") int priceStaleDays,
            @Value("${inkcore.paper.iva-rate:19}") BigDecimal ivaRate,
            @Value("${inkcore.paper.iva-deductible:true}") boolean ivaDeductible
    ) {
        this.priceStaleDays = priceStaleDays;
        this.ivaRate = ivaRate;
        this.ivaDeductible = ivaDeductible;
    }

    public int priceStaleDays() {
        return priceStaleDays;
    }

    public BigDecimal ivaRate() {
        return ivaRate;
    }

    public boolean ivaDeductible() {
        return ivaDeductible;
    }
}
