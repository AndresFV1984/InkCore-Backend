package com.inkcore.domain.colorconversion.model;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Catálogo de perfiles ICC de destino CMYK según condición de impresión / CTP.
 * El archivo real debe existir en {@code classpath:/color-profiles/} (o un alias conocido).
 */
public enum DestinationIccProfile {

    FOGRA39(
            "FOGRA39.icc",
            "Offset estucado Europa (ISO Coated v2 / FOGRA39)",
            "Papel couché/estucado offset. Estándar CTP europeo y LATAM más usado.",
            "COATED_OFFSET",
            "FOGRA39.icc",
            "ISOcoated_v2_eci.icc",
            "ISOCoatedV2.icc",
            "CoatedFOGRA39.icc"
    ),
    FOGRA51(
            "FOGRA51.icc",
            "Offset estucado Europa v3 (PSO Coated v3 / FOGRA51)",
            "Estucado moderno (incluido). Sustituye FOGRA39 en muchas imprentas.",
            "COATED_OFFSET",
            "FOGRA51.icc",
            "PSOcoated_v3.icc",
            "PSO_Coated_v3.icc"
    ),
    FOGRA47(
            "FOGRA47.icc",
            "Offset no estucado Europa (FOGRA47)",
            "Papel bond / no estucado clásico. No se redistribuye en el registry ICC; instalar PSO_Uncoated_ISO12647_eci.icc o usar FOGRA52.",
            "UNCOATED_OFFSET",
            "FOGRA47.icc",
            "PSO_Uncoated_ISO12647_eci.icc",
            "UncoatedFOGRA47.icc"
    ),
    FOGRA52(
            "FOGRA52.icc",
            "Offset no estucado Europa v3 (PSO Uncoated v3 / FOGRA52)",
            "No estucado moderno (incluido). Preferible frente a FOGRA47 si la imprenta lo acepta.",
            "UNCOATED_OFFSET",
            "FOGRA52.icc",
            "PSOuncoated_v3.icc",
            "PSO_Uncoated_v3.icc",
            "PSOuncoated_v3_FOGRA52.icc"
    ),
    GRACOL2013(
            "GRACoL2013.icc",
            "Offset estucado EE.UU. (GRACoL 2013)",
            "Prensa sheetfed coated norteamericana. Alternativa a FOGRA en mercados US.",
            "COATED_OFFSET_US",
            "GRACoL2013.icc",
            "GRACoL2013_CRPC6.icc",
            "GRACoL2006_Coated1v2.icc",
            "CoatedGRACoL2006.icc"
    ),
    SWOP2006(
            "SWOP2006_Coated3v2.icc",
            "Web offset EE.UU. (SWOP 2006)",
            "Impresión web coated / revistas. Gamut típico SWOP.",
            "WEB_OFFSET_US",
            "SWOP2006_Coated3v2.icc",
            "USWebCoatedSWOP.icc",
            "WebCoatedSWOP2006.icc"
    ),
    JAPAN_COLOR_2011(
            "JapanColor2011Coated.icc",
            "Offset estucado Japón (Japan Color 2011)",
            "Estándar de impresión coated japonés.",
            "COATED_OFFSET_JP",
            "JapanColor2011Coated.icc",
            "JapanColor2001Coated.icc"
    );

    private final String fileName;
    private final String title;
    private final String useCase;
    private final String paperClass;
    private final String[] aliases;

    DestinationIccProfile(
            String fileName,
            String title,
            String useCase,
            String paperClass,
            String... aliases
    ) {
        this.fileName = fileName;
        this.title = title;
        this.useCase = useCase;
        this.paperClass = paperClass;
        this.aliases = aliases == null ? new String[0] : aliases;
    }

    public String getFileName() {
        return fileName;
    }

    public String getTitle() {
        return title;
    }

    public String getUseCase() {
        return useCase;
    }

    public String getPaperClass() {
        return paperClass;
    }

    public String[] getAliases() {
        return aliases.clone();
    }

    /**
     * Resuelve el nombre de archivo canónico a cargar desde classpath.
     * Acepta el fileName del catálogo o cualquiera de sus alias.
     */
    public static String resolveClasspathFileName(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = normalizeName(raw);
        Optional<DestinationIccProfile> match = findByNameOrAlias(normalized);
        return match.map(DestinationIccProfile::getFileName).orElse(normalized);
    }

    public static Optional<DestinationIccProfile> findByNameOrAlias(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String normalized = normalizeName(raw);
        return Arrays.stream(values())
                .filter(p -> p.matches(normalized))
                .findFirst();
    }

    /**
     * Valida que el valor pertenezca al catálogo (fileName o alias).
     * Perfiles custom fuera del catálogo se permiten solo si el caller confirma existencia en classpath.
     */
    public static boolean isCatalogEntry(String raw) {
        return findByNameOrAlias(raw).isPresent();
    }

    public static String[] openApiAllowableValues() {
        return Arrays.stream(values())
                .map(DestinationIccProfile::getFileName)
                .toArray(String[]::new);
    }

    public boolean matches(String normalizedFileName) {
        if (fileName.equalsIgnoreCase(normalizedFileName)) {
            return true;
        }
        for (String alias : aliases) {
            if (alias.equalsIgnoreCase(normalizedFileName)) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeName(String raw) {
        String name = raw.trim().replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        if (!name.toLowerCase(Locale.ROOT).endsWith(".icc")
                && !name.toLowerCase(Locale.ROOT).endsWith(".icm")) {
            name = name + ".icc";
        }
        return name;
    }
}
