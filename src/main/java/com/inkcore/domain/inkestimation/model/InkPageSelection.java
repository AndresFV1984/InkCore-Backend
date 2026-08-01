package com.inkcore.domain.inkestimation.model;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Selección de páginas PDF para estimación (1-based, máximo 2).
 * Lista vacía = todas las páginas.
 */
public final class InkPageSelection {

    public static final int MAX_PAGES = 2;

    private InkPageSelection() {
    }

    /**
     * Normaliza una lista ya parseada (1-based). Vacía/null → todas.
     */
    public static List<Integer> normalize(List<Integer> pages) {
        if (pages == null || pages.isEmpty()) {
            return List.of();
        }
        Set<Integer> unique = new LinkedHashSet<>();
        for (Integer page : pages) {
            if (page == null) {
                throw new IllegalArgumentException("pages no puede contener valores nulos");
            }
            if (page < 1) {
                throw new IllegalArgumentException("pages usa índices 1-based; valor inválido: " + page);
            }
            unique.add(page);
        }
        if (unique.size() > MAX_PAGES) {
            throw new IllegalArgumentException(
                    "pages admite máximo " + MAX_PAGES + " páginas (recibidas: " + unique.size() + ")"
            );
        }
        return List.copyOf(unique);
    }

    /**
     * Parsea multipart {@code pages}: {@code "1"}, {@code "1,2"}, {@code "1 2"}.
     */
    public static List<Integer> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        String[] parts = raw.trim().split("[,;\\s]+");
        List<Integer> parsed = new ArrayList<>();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            try {
                parsed.add(Integer.parseInt(part.trim()));
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException(
                        "pages inválido: '" + raw + "'. Use p. ej. \"1\" o \"1,2\" (1-based, máx. "
                                + MAX_PAGES + ")"
                );
            }
        }
        return normalize(parsed);
    }

    /**
     * Valida que cada página exista en el PDF (totalPages ≥ 1).
     */
    public static void validateAgainstDocument(List<Integer> pagesOneBased, int totalPages) {
        if (totalPages < 1) {
            throw new IllegalArgumentException("El PDF no contiene páginas");
        }
        if (pagesOneBased == null || pagesOneBased.isEmpty()) {
            return;
        }
        for (int page : pagesOneBased) {
            if (page > totalPages) {
                throw new IllegalArgumentException(
                        "pages incluye la página " + page + " pero el PDF solo tiene " + totalPages
                                + " página(s)"
                );
            }
        }
    }

    public static boolean includes(List<Integer> pagesOneBased, int pageOneBased) {
        return pagesOneBased == null || pagesOneBased.isEmpty() || pagesOneBased.contains(pageOneBased);
    }

    public static String describe(List<Integer> pagesOneBased) {
        if (pagesOneBased == null || pagesOneBased.isEmpty()) {
            return "all";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pagesOneBased.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(pagesOneBased.get(i));
        }
        return sb.toString().toLowerCase(Locale.ROOT);
    }
}
