package com.inkcore.domain.productionorder.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Reglas sobre {@code inkEstimation.entries[]} al persistir impresión en OP.
 */
public final class InkEstimationEntriesSupport {

    private InkEstimationEntriesSupport() {
    }

    /**
     * Si el autosave llega sin {@code objectKey} pero ya existía en Postgres, conserva las claves
     * de MinIO para evitar perder el vínculo por carreras con el presign del front.
     */
    @SuppressWarnings("unchecked")
    public static void mergePersistedAssetKeys(
            Map<String, Object> inkEstimation,
            Map<String, Object> persistedInkEstimation
    ) {
        if (inkEstimation == null || persistedInkEstimation == null) {
            return;
        }
        Object entriesObj = inkEstimation.get("entries");
        if (!(entriesObj instanceof List<?> entries)) {
            return;
        }
        for (Object entryObj : entries) {
            if (entryObj instanceof Map<?, ?> rawEntry) {
                mergePersistedAssetKeysForEntry((Map<String, Object>) rawEntry, persistedInkEstimation);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static void mergePersistedAssetKeysForEntry(
            Map<String, Object> entry,
            Map<String, Object> persistedInkEstimation
    ) {
        if (entry == null || persistedInkEstimation == null || hasNonBlankKey(entry, "objectKey")) {
            return;
        }
        String entryId = entryId(entry);
        if (entryId == null) {
            return;
        }
        Map<String, Object> persistedEntry = findEntryById(persistedInkEstimation, entryId);
        if (persistedEntry == null) {
            return;
        }
        copyIfAbsent(entry, persistedEntry, "objectKey");
        copyIfAbsent(entry, persistedEntry, "previewObjectKey");
        copyIfAbsent(entry, persistedEntry, "contentType");
        copyIfAbsent(entry, persistedEntry, "sizeBytes");
    }

    /**
     * Valida entradas con arte declarado. Solo aplica cuando la impresión de la plancha está marcada completa.
     */
    @SuppressWarnings("unchecked")
    public static List<String> validateAssetReferences(
            Map<String, Object> inkEstimation,
            boolean printingCompleted
    ) {
        if (!printingCompleted || inkEstimation == null || inkEstimation.isEmpty()) {
            return List.of();
        }
        Object entriesObj = inkEstimation.get("entries");
        if (!(entriesObj instanceof List<?> entries) || entries.isEmpty()) {
            return List.of();
        }
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            if (!(entries.get(i) instanceof Map<?, ?> rawEntry)) {
                continue;
            }
            Map<String, Object> entry = (Map<String, Object>) rawEntry;
            if (!requiresAssetKey(entry)) {
                continue;
            }
            if (!hasNonBlankKey(entry, "objectKey")) {
                errors.add("inkEstimation.entries[" + i + "]: fileName declarado sin objectKey en MinIO");
            }
        }
        return errors;
    }

    public static boolean requiresAssetKey(Map<String, Object> entry) {
        return hasNonBlankKey(entry, "fileName");
    }

    public static String entryId(Map<String, Object> entry) {
        if (entry == null) {
            return null;
        }
        Object entradaId = entry.get("entradaId");
        if (entradaId instanceof String text && !text.isBlank()) {
            return text.trim();
        }
        Object id = entry.get("id");
        if (id instanceof String text && !text.isBlank()) {
            return text.trim();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> findEntryById(Map<String, Object> inkEstimation, String entryId) {
        Object entriesObj = inkEstimation.get("entries");
        if (!(entriesObj instanceof List<?> entries)) {
            return null;
        }
        for (Object entryObj : entries) {
            if (entryObj instanceof Map<?, ?> rawEntry) {
                Map<String, Object> entry = (Map<String, Object>) rawEntry;
                String candidate = entryId(entry);
                if (entryId.equals(candidate)) {
                    return entry;
                }
            }
        }
        return null;
    }

    private static void copyIfAbsent(Map<String, Object> target, Map<String, Object> source, String field) {
        if (hasNonBlankKey(target, field)) {
            return;
        }
        Object value = source.get(field);
        if (value != null) {
            target.put(field, value);
        }
    }

    private static boolean hasNonBlankKey(Map<String, Object> entry, String field) {
        Object value = entry.get(field);
        if (value instanceof String text) {
            return !text.isBlank();
        }
        return false;
    }
}
