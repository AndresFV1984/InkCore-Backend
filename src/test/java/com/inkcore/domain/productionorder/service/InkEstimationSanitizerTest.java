package com.inkcore.domain.productionorder.service;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InkEstimationSanitizerTest {

    @Test
    void sanitize_removesForbiddenKeysAndDataUrls() {
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("totalGramsOrder", 100.5);
        raw.put("previewImageDataUrl", "data:image/jpeg;base64," + "A".repeat(512));
        raw.put("entries", List.of(Map.of(
                "objectKey", "company/c1/tmp/ink-estimates/u1/e1/original.pdf",
                "previewObjectKey", "company/c1/tmp/ink-estimates/u1/e1/preview.jpg",
                "fileBase64", "QUJD".repeat(200),
                "coveragePercent", 12.5
        )));

        Map<String, Object> sanitized = InkEstimationSanitizer.sanitize(raw);

        assertEquals(100.5, sanitized.get("totalGramsOrder"));
        assertFalse(sanitized.containsKey("previewImageDataUrl"));
        @SuppressWarnings("unchecked")
        Map<String, Object> entry = ((List<Map<String, Object>>) sanitized.get("entries")).get(0);
        assertEquals("company/c1/tmp/ink-estimates/u1/e1/original.pdf", entry.get("objectKey"));
        assertFalse(entry.containsKey("fileBase64"));
        assertEquals(12.5, entry.get("coveragePercent"));
    }

    @Test
    void sanitize_returnsNullForEmptyResult() {
        assertNull(InkEstimationSanitizer.sanitize(Map.of("previewImageDataUrl", "data:image/png;base64,abc")));
    }
}
