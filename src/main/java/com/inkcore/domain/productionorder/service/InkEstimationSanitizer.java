package com.inkcore.domain.productionorder.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Limpia {@code ink_estimation} antes de persistir: sin Base64 ni data-URL en JSONB.
 */
public final class InkEstimationSanitizer {

    private static final Set<String> FORBIDDEN_KEYS = Set.of(
            "previewimagedataurl",
            "previewimage",
            "previewrgbbase64",
            "filebase64",
            "dataurl",
            "imagedataurl",
            "base64",
            "previewdataurl"
    );

    private InkEstimationSanitizer() {
    }

    public static Map<String, Object> sanitize(Map<String, Object> inkEstimation) {
        if (inkEstimation == null || inkEstimation.isEmpty()) {
            return null;
        }
        return sanitizeMap(inkEstimation);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> sanitizeMap(Map<String, Object> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            if (entry.getKey() == null || isForbiddenKey(entry.getKey())) {
                continue;
            }
            Object cleaned = sanitizeValue(entry.getValue());
            if (cleaned != null) {
                result.put(entry.getKey(), cleaned);
            }
        }
        return result.isEmpty() ? null : result;
    }

    @SuppressWarnings("unchecked")
    private static Object sanitizeValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map<?, ?> map) {
            return sanitizeMap((Map<String, Object>) map);
        }
        if (value instanceof List<?> list) {
            List<Object> cleaned = new ArrayList<>();
            for (Object item : list) {
                Object next = sanitizeValue(item);
                if (next != null) {
                    cleaned.add(next);
                }
            }
            return cleaned.isEmpty() ? null : cleaned;
        }
        if (value instanceof String text && looksLikeEmbeddedPayload(text)) {
            return null;
        }
        return value;
    }

    private static boolean isForbiddenKey(String key) {
        return FORBIDDEN_KEYS.contains(key.trim().toLowerCase(Locale.ROOT));
    }

    private static boolean looksLikeEmbeddedPayload(String text) {
        String trimmed = text.trim();
        if (trimmed.length() < 256) {
            return false;
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);
        return lower.startsWith("data:")
                || (lower.length() > 512 && lower.chars().filter(ch -> ch == ',').count() <= 1
                && trimmed.matches("^[A-Za-z0-9+/=\\s]+$"));
    }
}
