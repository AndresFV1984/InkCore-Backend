package com.inkcore.domain.wastesettings.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Rangos sugeridos de merma por compañía y por proceso de la orden.
 * Corte 2%–5% (default 2%) e impresión 3%–8% (default 3%) son el valor
 * inicial. Preprensa, terminados y acabados nacen con el mismo rango de
 * impresión para no cambiar lo ya cotizado; la compañía puede separarlos.
 */
public final class CompanyWasteSettings {

    private final String companyId;
    private final BigDecimal cutWasteMinPercentage;
    private final BigDecimal cutWasteMaxPercentage;
    private final BigDecimal cutWasteDefaultPercentage;
    private final BigDecimal operationalWasteMinPercentage;
    private final BigDecimal operationalWasteMaxPercentage;
    private final BigDecimal operationalWasteDefaultPercentage;
    private final BigDecimal prepressWasteMinPercentage;
    private final BigDecimal prepressWasteMaxPercentage;
    private final BigDecimal prepressWasteDefaultPercentage;
    private final BigDecimal finishedWasteMinPercentage;
    private final BigDecimal finishedWasteMaxPercentage;
    private final BigDecimal finishedWasteDefaultPercentage;
    private final BigDecimal finishingWasteMinPercentage;
    private final BigDecimal finishingWasteMaxPercentage;
    private final BigDecimal finishingWasteDefaultPercentage;
    private final BigDecimal cutMakereadySheets;
    private final BigDecimal operationalMakereadySheets;
    private final LocalDateTime updatedAt;
    private final boolean persisted;

    private CompanyWasteSettings(
            String companyId,
            BigDecimal cutWasteMinPercentage,
            BigDecimal cutWasteMaxPercentage,
            BigDecimal cutWasteDefaultPercentage,
            BigDecimal operationalWasteMinPercentage,
            BigDecimal operationalWasteMaxPercentage,
            BigDecimal operationalWasteDefaultPercentage,
            BigDecimal prepressWasteMinPercentage,
            BigDecimal prepressWasteMaxPercentage,
            BigDecimal prepressWasteDefaultPercentage,
            BigDecimal finishedWasteMinPercentage,
            BigDecimal finishedWasteMaxPercentage,
            BigDecimal finishedWasteDefaultPercentage,
            BigDecimal finishingWasteMinPercentage,
            BigDecimal finishingWasteMaxPercentage,
            BigDecimal finishingWasteDefaultPercentage,
            BigDecimal cutMakereadySheets,
            BigDecimal operationalMakereadySheets,
            LocalDateTime updatedAt,
            boolean persisted
    ) {
        this.companyId = companyId;
        this.cutWasteMinPercentage = cutWasteMinPercentage;
        this.cutWasteMaxPercentage = cutWasteMaxPercentage;
        this.cutWasteDefaultPercentage = cutWasteDefaultPercentage;
        this.operationalWasteMinPercentage = operationalWasteMinPercentage;
        this.operationalWasteMaxPercentage = operationalWasteMaxPercentage;
        this.operationalWasteDefaultPercentage = operationalWasteDefaultPercentage;
        this.prepressWasteMinPercentage = prepressWasteMinPercentage;
        this.prepressWasteMaxPercentage = prepressWasteMaxPercentage;
        this.prepressWasteDefaultPercentage = prepressWasteDefaultPercentage;
        this.finishedWasteMinPercentage = finishedWasteMinPercentage;
        this.finishedWasteMaxPercentage = finishedWasteMaxPercentage;
        this.finishedWasteDefaultPercentage = finishedWasteDefaultPercentage;
        this.finishingWasteMinPercentage = finishingWasteMinPercentage;
        this.finishingWasteMaxPercentage = finishingWasteMaxPercentage;
        this.finishingWasteDefaultPercentage = finishingWasteDefaultPercentage;
        this.cutMakereadySheets = cutMakereadySheets;
        this.operationalMakereadySheets = operationalMakereadySheets;
        this.updatedAt = updatedAt;
        this.persisted = persisted;
    }

    public static CompanyWasteSettings initialSuggestion(String companyId) {
        BigDecimal cutMin = scale(new BigDecimal("2"));
        BigDecimal cutMax = scale(new BigDecimal("5"));
        BigDecimal cutDefault = scale(new BigDecimal("2"));
        BigDecimal operationalMin = scale(new BigDecimal("3"));
        BigDecimal operationalMax = scale(new BigDecimal("8"));
        BigDecimal operationalDefault = scale(new BigDecimal("3"));
        return new CompanyWasteSettings(
                companyId,
                cutMin,
                cutMax,
                cutDefault,
                operationalMin,
                operationalMax,
                operationalDefault,
                operationalMin,
                operationalMax,
                operationalDefault,
                operationalMin,
                operationalMax,
                operationalDefault,
                operationalMin,
                operationalMax,
                operationalDefault,
                scale(BigDecimal.ZERO),
                scale(BigDecimal.ZERO),
                null,
                false
        );
    }

    public static CompanyWasteSettings reconstitute(
            String companyId,
            BigDecimal cutWasteMinPercentage,
            BigDecimal cutWasteMaxPercentage,
            BigDecimal cutWasteDefaultPercentage,
            BigDecimal operationalWasteMinPercentage,
            BigDecimal operationalWasteMaxPercentage,
            BigDecimal operationalWasteDefaultPercentage,
            BigDecimal prepressWasteMinPercentage,
            BigDecimal prepressWasteMaxPercentage,
            BigDecimal prepressWasteDefaultPercentage,
            BigDecimal finishedWasteMinPercentage,
            BigDecimal finishedWasteMaxPercentage,
            BigDecimal finishedWasteDefaultPercentage,
            BigDecimal finishingWasteMinPercentage,
            BigDecimal finishingWasteMaxPercentage,
            BigDecimal finishingWasteDefaultPercentage,
            BigDecimal cutMakereadySheets,
            BigDecimal operationalMakereadySheets,
            LocalDateTime updatedAt
    ) {
        return new CompanyWasteSettings(
                companyId,
                cutWasteMinPercentage,
                cutWasteMaxPercentage,
                cutWasteDefaultPercentage,
                operationalWasteMinPercentage,
                operationalWasteMaxPercentage,
                operationalWasteDefaultPercentage,
                prepressWasteMinPercentage,
                prepressWasteMaxPercentage,
                prepressWasteDefaultPercentage,
                finishedWasteMinPercentage,
                finishedWasteMaxPercentage,
                finishedWasteDefaultPercentage,
                finishingWasteMinPercentage,
                finishingWasteMaxPercentage,
                finishingWasteDefaultPercentage,
                cutMakereadySheets,
                operationalMakereadySheets,
                updatedAt,
                true
        );
    }

    public CompanyWasteSettings update(
            BigDecimal cutMin,
            BigDecimal cutMax,
            BigDecimal cutDefault,
            BigDecimal operationalMin,
            BigDecimal operationalMax,
            BigDecimal operationalDefault,
            BigDecimal prepressMin,
            BigDecimal prepressMax,
            BigDecimal prepressDefault,
            BigDecimal finishedMin,
            BigDecimal finishedMax,
            BigDecimal finishedDefault,
            BigDecimal finishingMin,
            BigDecimal finishingMax,
            BigDecimal finishingDefault,
            BigDecimal cutMakeready,
            BigDecimal operationalMakeready,
            LocalDateTime updatedAt
    ) {
        BigDecimal[] cut = band(cutMin, cutDefault, cutMax, "merma de corte");
        BigDecimal[] operational = band(operationalMin, operationalDefault, operationalMax, "merma de impresión");
        BigDecimal[] prepress = band(prepressMin, prepressDefault, prepressMax, "merma de preprensa");
        BigDecimal[] finished = band(finishedMin, finishedDefault, finishedMax, "merma de terminados");
        BigDecimal[] finishing = band(finishingMin, finishingDefault, finishingMax, "merma de acabados");
        return new CompanyWasteSettings(
                this.companyId,
                cut[0], cut[1], cut[2],
                operational[0], operational[1], operational[2],
                prepress[0], prepress[1], prepress[2],
                finished[0], finished[1], finished[2],
                finishing[0], finishing[1], finishing[2],
                sheets(cutMakeready, "Los pliegos fijos de arranque de corte"),
                sheets(operationalMakeready, "Los pliegos fijos de arranque de impresión"),
                updatedAt,
                true
        );
    }

    /**
     * Porcentaje que se aplica al cotizar el proceso si el paso no envía uno.
     */
    public BigDecimal defaultPercentage(String phase) {
        if ("preprensa".equals(phase)) {
            return prepressWasteDefaultPercentage;
        }
        if ("corte-papel".equals(phase)) {
            return cutWasteDefaultPercentage;
        }
        if ("impresion".equals(phase)) {
            return operationalWasteDefaultPercentage;
        }
        if ("terminados".equals(phase)) {
            return finishedWasteDefaultPercentage;
        }
        if ("acabados".equals(phase)) {
            return finishingWasteDefaultPercentage;
        }
        throw new IllegalArgumentException("Fase de merma no reconocida");
    }

    private static BigDecimal[] band(BigDecimal min, BigDecimal value, BigDecimal max, String label) {
        BigDecimal normalizedMin = percentage(min, "El mínimo de " + label);
        BigDecimal normalizedMax = percentage(max, "El máximo de " + label);
        BigDecimal normalizedValue = percentage(value, "El porcentaje por defecto de " + label);
        assertRange(normalizedMin, normalizedValue, normalizedMax, label);
        return new BigDecimal[] {normalizedMin, normalizedMax, normalizedValue};
    }

    private static BigDecimal sheets(BigDecimal value, String label) {
        if (value == null) {
            throw new IllegalArgumentException(label + " son obligatorios");
        }
        if (value.signum() < 0) {
            throw new IllegalArgumentException(label + " no pueden ser negativos");
        }
        return scale(value);
    }

    private static void assertRange(BigDecimal min, BigDecimal value, BigDecimal max, String label) {
        if (min.compareTo(max) > 0 || value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw new IllegalArgumentException(
                    "El rango de " + label + " debe cumplir mínimo <= valor por defecto <= máximo");
        }
    }

    private static BigDecimal percentage(BigDecimal value, String label) {
        if (value == null) {
            throw new IllegalArgumentException(label + " es obligatorio");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException(label + " debe estar entre 0 y 100");
        }
        return scale(value);
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public String getCompanyId() {
        return companyId;
    }

    public BigDecimal getCutWasteMinPercentage() {
        return cutWasteMinPercentage;
    }

    public BigDecimal getCutWasteMaxPercentage() {
        return cutWasteMaxPercentage;
    }

    public BigDecimal getCutWasteDefaultPercentage() {
        return cutWasteDefaultPercentage;
    }

    public BigDecimal getOperationalWasteMinPercentage() {
        return operationalWasteMinPercentage;
    }

    public BigDecimal getOperationalWasteMaxPercentage() {
        return operationalWasteMaxPercentage;
    }

    public BigDecimal getOperationalWasteDefaultPercentage() {
        return operationalWasteDefaultPercentage;
    }

    public BigDecimal getPrepressWasteMinPercentage() {
        return prepressWasteMinPercentage;
    }

    public BigDecimal getPrepressWasteMaxPercentage() {
        return prepressWasteMaxPercentage;
    }

    public BigDecimal getPrepressWasteDefaultPercentage() {
        return prepressWasteDefaultPercentage;
    }

    public BigDecimal getFinishedWasteMinPercentage() {
        return finishedWasteMinPercentage;
    }

    public BigDecimal getFinishedWasteMaxPercentage() {
        return finishedWasteMaxPercentage;
    }

    public BigDecimal getFinishedWasteDefaultPercentage() {
        return finishedWasteDefaultPercentage;
    }

    public BigDecimal getFinishingWasteMinPercentage() {
        return finishingWasteMinPercentage;
    }

    public BigDecimal getFinishingWasteMaxPercentage() {
        return finishingWasteMaxPercentage;
    }

    public BigDecimal getFinishingWasteDefaultPercentage() {
        return finishingWasteDefaultPercentage;
    }

    public BigDecimal getCutMakereadySheets() {
        return cutMakereadySheets;
    }

    public BigDecimal getOperationalMakereadySheets() {
        return operationalMakereadySheets;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public boolean isPersisted() {
        return persisted;
    }
}
