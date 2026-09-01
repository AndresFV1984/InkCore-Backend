package com.inkcore.domain.productionorder.service;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InkEstimationEntriesSupportTest {

    @Test
    void mergePersistedAssetKeys_restoresObjectKeyFromPersistedEntry() {
        Map<String, Object> persisted = Map.of(
                "entries",
                List.of(Map.of(
                        "id", "entry-1",
                        "objectKey", "company/c1/production-orders/po-1/prints/plate-1/ink-estimates/entry-1/original.pdf",
                        "previewObjectKey", "company/c1/production-orders/po-1/prints/plate-1/ink-estimates/entry-1/preview.jpg"
                ))
        );
        Map<String, Object> incoming = new LinkedHashMap<>();
        incoming.put("entries", List.of(new LinkedHashMap<>(Map.of(
                "id", "entry-1",
                "fileName", "arte.pdf"
        ))));

        InkEstimationEntriesSupport.mergePersistedAssetKeys(incoming, persisted);

        @SuppressWarnings("unchecked")
        Map<String, Object> entry = ((List<Map<String, Object>>) incoming.get("entries")).get(0);
        assertEquals(
                "company/c1/production-orders/po-1/prints/plate-1/ink-estimates/entry-1/original.pdf",
                entry.get("objectKey")
        );
        assertEquals(
                "company/c1/production-orders/po-1/prints/plate-1/ink-estimates/entry-1/preview.jpg",
                entry.get("previewObjectKey")
        );
    }

    @Test
    void validateAssetReferences_requiresObjectKeyWhenPrintingCompleted() {
        Map<String, Object> inkEstimation = Map.of(
                "entries",
                List.of(Map.of("id", "entry-1", "fileName", "arte.pdf"))
        );
        List<String> errors = InkEstimationEntriesSupport.validateAssetReferences(inkEstimation, true);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("objectKey"));
    }
}
