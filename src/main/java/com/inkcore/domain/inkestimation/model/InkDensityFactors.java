package com.inkcore.domain.inkestimation.model;

/**
 * Factores g/cm² al 100 % de cobertura por canal.
 * Permite densidades distintas C/M/Y/K/spot (práctica offset) sin licencias de RIP.
 */
public record InkDensityFactors(
        double cyan,
        double magenta,
        double yellow,
        double black,
        double spot
) {
    public InkDensityFactors {
        if (cyan <= 0 || magenta <= 0 || yellow <= 0 || black <= 0 || spot <= 0) {
            throw new IllegalArgumentException("Los factores g/cm² deben ser mayores que 0");
        }
    }

    public static InkDensityFactors uniform(double gramsPerCm2) {
        return new InkDensityFactors(gramsPerCm2, gramsPerCm2, gramsPerCm2, gramsPerCm2, gramsPerCm2);
    }

    public double forChannel(String channel) {
        if (channel == null) {
            return spot;
        }
        return switch (channel.trim().toUpperCase()) {
            case "C" -> cyan;
            case "M" -> magenta;
            case "Y" -> yellow;
            case "K" -> black;
            default -> spot;
        };
    }

    /** Factor de referencia (cian) para eco en API/historial. */
    public double reference() {
        return cyan;
    }
}
