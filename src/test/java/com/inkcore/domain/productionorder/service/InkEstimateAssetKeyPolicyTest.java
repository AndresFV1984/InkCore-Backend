package com.inkcore.domain.productionorder.service;

import com.inkcore.domain.objectstorage.exception.InvalidObjectKeyException;
import com.inkcore.domain.objectstorage.exception.ObjectStorageAccessDeniedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InkEstimateAssetKeyPolicyTest {

    @Test
    void buildsStagingAndDefinitiveKeys() {
        String staging = InkEstimateAssetKeyPolicy.stagingOriginalKey(
                "company-1", "user-1", "entry-1", "pdf");
        assertEquals(
                "tmp/company/company-1/ink-estimates/user-1/entry-1/original.pdf",
                staging
        );

        String definitive = InkEstimateAssetKeyPolicy.definitiveOriginalKey(
                "company-1", "po-1", "plate-1", "entry-1", "pdf");
        assertEquals(
                "company/company-1/production-orders/po-1/prints/plate-1/ink-estimates/entry-1/original.pdf",
                definitive
        );
    }

    @Test
    void assertStagingKeyForUser_acceptsCanonicalAndLegacyPrefix() {
        InkEstimateAssetKeyPolicy.assertStagingKeyForUser(
                "tmp/company/c1/ink-estimates/u1/entry-1/original.pdf",
                "c1",
                "u1"
        );
        InkEstimateAssetKeyPolicy.assertStagingKeyForUser(
                "company/c1/tmp/ink-estimates/u1/entry-1/original.pdf",
                "c1",
                "u1"
        );
    }

    @Test
    void assertStagingKeyForUser_rejectsForeignCompanyEvenIfIdAppearsLater() {
        assertThrows(ObjectStorageAccessDeniedException.class, () ->
                InkEstimateAssetKeyPolicy.assertStagingKeyForUser(
                        "tmp/company/other/ink-estimates/u1/entry-1/original.pdf",
                        "c1",
                        "u1"
                ));
        assertThrows(ObjectStorageAccessDeniedException.class, () ->
                InkEstimateAssetKeyPolicy.assertEstimateSourceKey(
                        "tmp/company/c1/secret/company/other/ink-estimates/u1/entry-1/original.pdf",
                        "c1",
                        "u1"
                ));
    }

    @Test
    void assertStagingKeyForUser_rejectsOtherUserInSameCompany() {
        assertThrows(ObjectStorageAccessDeniedException.class, () ->
                InkEstimateAssetKeyPolicy.assertStagingKeyForUser(
                        "tmp/company/c1/ink-estimates/other-user/entry-1/original.pdf",
                        "c1",
                        "u1"
                ));
    }

    @Test
    void assertSafeObjectKey_rejectsDoubleSlash() {
        assertThrows(InvalidObjectKeyException.class, () ->
                InkEstimateAssetKeyPolicy.assertSafeObjectKey(
                        "tmp/company/c1//ink-estimates/u1/entry-1/original.pdf"));
    }

    @Test
    void assertSafeObjectKey_rejectsTraversalAndEncodedSlash() {
        assertThrows(InvalidObjectKeyException.class, () ->
                InkEstimateAssetKeyPolicy.assertSafeObjectKey(
                        "tmp/company/c1/ink-estimates/u1/../other/original.pdf"));
        assertThrows(InvalidObjectKeyException.class, () ->
                InkEstimateAssetKeyPolicy.assertSafeObjectKey(
                        "tmp/company/c1/ink-estimates/u1/entry-1/original%2fpdf"));
    }

    @Test
    void isStagingObjectKey_requiresCanonicalTmpPrefixOrLegacyTmpSegment() {
        assertTrue(InkEstimateAssetKeyPolicy.isStagingObjectKey(
                "tmp/company/c1/ink-estimates/u1/entry-1/original.pdf"));
        assertTrue(InkEstimateAssetKeyPolicy.isStagingObjectKey(
                "company/c1/tmp/ink-estimates/u1/entry-1/original.pdf"));
        assertFalse(InkEstimateAssetKeyPolicy.isStagingObjectKey(
                "company/c1/production-orders/po-1/tmp/ink-estimates/u1/entry-1/original.pdf"));
        assertFalse(InkEstimateAssetKeyPolicy.isStagingObjectKey(
                "tmp/company/c1/ink-estimates/u1/entry-1/extra/original.pdf"));
        assertFalse(InkEstimateAssetKeyPolicy.isLegacyStagingObjectKey(
                "tmp/company/c1/ink-estimates/u1/entry-1/original.pdf"));
        assertTrue(InkEstimateAssetKeyPolicy.isLegacyStagingObjectKey(
                "company/c1/tmp/ink-estimates/u1/entry-1/original.pdf"));
    }

    @Test
    void assertStagingKeyForUser_rejectsCompanyIdPrefixConfusionOnBothFormats() {
        assertThrows(ObjectStorageAccessDeniedException.class, () ->
                InkEstimateAssetKeyPolicy.assertStagingKeyForUser(
                        "tmp/company/c1evil/ink-estimates/u1/entry-1/original.pdf",
                        "c1",
                        "u1"
                ));
        assertThrows(ObjectStorageAccessDeniedException.class, () ->
                InkEstimateAssetKeyPolicy.assertStagingKeyForUser(
                        "company/c1evil/tmp/ink-estimates/u1/entry-1/original.pdf",
                        "c1",
                        "u1"
                ));
    }

    @Test
    void assertStagingKeyForUser_rejectsHybridAndLegacyForeignUser() {
        assertThrows(ObjectStorageAccessDeniedException.class, () ->
                InkEstimateAssetKeyPolicy.assertStagingKeyForUser(
                        "tmp/company/c1/tmp/ink-estimates/u1/entry-1/original.pdf",
                        "c1",
                        "u1"
                ));
        assertThrows(ObjectStorageAccessDeniedException.class, () ->
                InkEstimateAssetKeyPolicy.assertStagingKeyForUser(
                        "company/c1/tmp/ink-estimates/other-user/entry-1/original.pdf",
                        "c1",
                        "u1"
                ));
        assertThrows(ObjectStorageAccessDeniedException.class, () ->
                InkEstimateAssetKeyPolicy.assertEstimateSourceKey(
                        "company/other/tmp/ink-estimates/u1/entry-1/original.pdf",
                        "c1",
                        "u1"
                ));
    }

    @Test
    void extractsEntradaIdFromStagingKey() {
        assertEquals(
                "entry-1",
                InkEstimateAssetKeyPolicy.extractEntradaIdFromKey(
                        "tmp/company/c1/ink-estimates/u1/entry-1/original.pdf")
        );
        assertEquals(
                "entry-1",
                InkEstimateAssetKeyPolicy.extractEntradaIdFromKey(
                        "company/c1/tmp/ink-estimates/u1/entry-1/original.pdf")
        );
    }
}
