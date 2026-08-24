package com.inkcore.domain.productionorder.service;

import com.inkcore.domain.objectstorage.exception.InvalidObjectKeyException;
import com.inkcore.domain.objectstorage.exception.ObjectStorageAccessDeniedException;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Convención de claves S3/MinIO para artes de estimación de tintas en OP.
 * <p>
 * Staging canónico: {@code tmp/company/{companyId}/ink-estimates/{userId}/{entradaId}/...}
 * (el prefijo {@code tmp/} permite ILM por prefix; S3 no soporta glob en el medio de la key).
 * Staging legado (solo lectura/relocate): {@code company/{companyId}/tmp/ink-estimates/...}
 * Definitivo: {@code company/{companyId}/production-orders/{poId}/prints/{plateId}/ink-estimates/{entradaId}/...}
 * <p>
 * El aislamiento entre empresas es JWT + prefijo exacto (con {@code /} final), no IAM.
 * Si más adelante se agregan políticas por prefijo, hay que cubrir ambos:
 * {@code company/{id}/*} y {@code tmp/company/{id}/*}.
 */
public final class InkEstimateAssetKeyPolicy {

    /** Prefijo de bucket para ILM de respaldo (sin tag). */
    public static final String STAGING_BUCKET_PREFIX = "tmp/";

    /** Tag ILM de objetos temporales (MinIO/S3 lifecycle, 2 días). */
    public static final String STAGING_LIFECYCLE_TAG_KEY = "inkcore-staging";
    public static final String STAGING_LIFECYCLE_TAG_VALUE = "true";

    private static final Pattern SAFE_SEGMENT = Pattern.compile("^[a-zA-Z0-9._-]+$");
    private static final Pattern SAFE_EXTENSION = Pattern.compile("^[a-z0-9]{1,10}$");
    private static final Pattern SAFE_FILE_NAME = Pattern.compile("^[a-zA-Z0-9._-]+$");

    private InkEstimateAssetKeyPolicy() {
    }

    public static String stagingOriginalKey(
            String companyId,
            String userId,
            String entradaId,
            String extension
    ) {
        return stagingBase(companyId, userId, entradaId) + "/original." + normalizeExtension(extension);
    }

    public static String stagingPreviewKey(String companyId, String userId, String entradaId) {
        return stagingBase(companyId, userId, entradaId) + "/preview.jpg";
    }

    public static String definitiveOriginalKey(
            String companyId,
            String productionOrderId,
            String plateId,
            String entradaId,
            String extension
    ) {
        return definitiveBase(companyId, productionOrderId, plateId, entradaId)
                + "/original." + normalizeExtension(extension);
    }

    public static String definitivePreviewKey(
            String companyId,
            String productionOrderId,
            String plateId,
            String entradaId
    ) {
        return definitiveBase(companyId, productionOrderId, plateId, entradaId) + "/preview.jpg";
    }

    /**
     * Staging canónico ({@code tmp/company/{id}/ink-estimates/{user}/{entrada}/file})
     * o legado ({@code company/{id}/tmp/ink-estimates/{user}/{entrada}/file}).
     * Exactamente 7 segmentos; no basta con que {@code tmp} aparezca en la key.
     */
    public static boolean isStagingObjectKey(String objectKey) {
        String[] parts = splitKey(objectKey);
        return isCanonicalStaging(parts) || isLegacyStaging(parts);
    }

    /** Path viejo {@code company/{id}/tmp/ink-estimates/...} (huérfanos pre-fix). */
    public static boolean isLegacyStagingObjectKey(String objectKey) {
        return isLegacyStaging(splitKey(objectKey));
    }

    public static boolean isProductionOrderObjectKey(String objectKey) {
        String[] parts = splitKey(objectKey);
        return parts.length >= 3
                && "company".equals(parts[0])
                && "production-orders".equals(parts[2]);
    }

    public static void assertSafeObjectKey(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new InvalidObjectKeyException("objectKey es obligatorio");
        }
        String key = objectKey.trim();
        if (!key.equals(objectKey)) {
            throw new InvalidObjectKeyException("objectKey inválido");
        }
        if (key.startsWith("/") || key.contains("\\") || key.contains("..")) {
            throw new InvalidObjectKeyException("objectKey inválido");
        }
        if (key.indexOf('%') >= 0 || key.contains("//") || key.indexOf('\0') >= 0) {
            throw new InvalidObjectKeyException("objectKey inválido");
        }
        if (!key.startsWith("company/") && !key.startsWith(STAGING_BUCKET_PREFIX)) {
            throw new InvalidObjectKeyException("objectKey debe iniciar con company/ o tmp/");
        }
        String[] parts = splitKey(key);
        if (parts.length < 3) {
            throw new InvalidObjectKeyException("objectKey inválido");
        }
        for (String part : parts) {
            if (part.isEmpty()) {
                throw new InvalidObjectKeyException("objectKey inválido");
            }
        }
    }

    public static void assertReadableByCompany(String objectKey, String companyId) {
        assertSafeObjectKey(objectKey);
        String expectedPrefix = objectKey.startsWith(STAGING_BUCKET_PREFIX)
                ? stagingCompanyPrefix(companyId)
                : companyPrefix(companyId);
        if (!objectKey.startsWith(expectedPrefix)) {
            throw new ObjectStorageAccessDeniedException("Sin permiso para el objeto solicitado");
        }
    }

    public static void assertStagingKeyForUser(String objectKey, String companyId, String userId) {
        assertReadableByCompany(objectKey, companyId);
        String[] parts = splitKey(objectKey);
        boolean canonical = isCanonicalStaging(parts);
        boolean legacy = isLegacyStaging(parts);
        if (!canonical && !legacy) {
            throw new ObjectStorageAccessDeniedException("Sin permiso para el objeto solicitado");
        }
        String expectedPrefix = canonical
                ? canonicalStagingUserPrefix(companyId, userId)
                : legacyStagingUserPrefix(companyId, userId);
        if (!objectKey.startsWith(expectedPrefix)) {
            throw new ObjectStorageAccessDeniedException("Sin permiso para el objeto solicitado");
        }
        String rest = objectKey.substring(expectedPrefix.length());
        int slash = rest.indexOf('/');
        if (slash <= 0 || slash == rest.length() - 1 || rest.indexOf('/', slash + 1) >= 0) {
            throw new InvalidObjectKeyException("objectKey de staging inválido");
        }
        requireSegment(rest.substring(0, slash), "entradaId");
        String fileName = rest.substring(slash + 1);
        if (!SAFE_FILE_NAME.matcher(fileName).matches()) {
            throw new InvalidObjectKeyException("objectKey de staging inválido");
        }
    }

    public static void assertProductionOrderKey(
            String objectKey,
            String companyId,
            String productionOrderId
    ) {
        assertReadableByCompany(objectKey, companyId);
        String expectedPrefix = "company/" + requireSegment(companyId, "companyId")
                + "/production-orders/" + requireSegment(productionOrderId, "productionOrderId") + "/";
        if (!objectKey.startsWith(expectedPrefix)) {
            throw new ObjectStorageAccessDeniedException("Sin permiso para el objeto solicitado");
        }
    }

    /**
     * Estimación / GET: solo staging del usuario JWT o artes definitivos de la misma empresa.
     */
    public static void assertEstimateSourceKey(String objectKey, String companyId, String userId) {
        assertReadableByCompany(objectKey, companyId);
        if (isStagingObjectKey(objectKey)) {
            assertStagingKeyForUser(objectKey, companyId, userId);
            return;
        }
        if (isProductionOrderObjectKey(objectKey)) {
            String expectedPrefix = companyPrefix(companyId) + "production-orders/";
            if (!objectKey.startsWith(expectedPrefix)) {
                throw new ObjectStorageAccessDeniedException("Sin permiso para el objeto solicitado");
            }
            return;
        }
        throw new ObjectStorageAccessDeniedException("Sin permiso para el objeto solicitado");
    }

    public static String extractExtensionFromKey(String objectKey) {
        int slash = objectKey.lastIndexOf('/');
        int dot = objectKey.lastIndexOf('.');
        if (dot <= slash || dot == objectKey.length() - 1) {
            return "bin";
        }
        return objectKey.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    public static String extractEntradaIdFromKey(String objectKey) {
        String[] parts = splitKey(objectKey);
        if (isStagingObjectKey(objectKey)) {
            // tmp/company/{c}/ink-estimates/{user}/{entradaId}/file
            // company/{c}/tmp/ink-estimates/{user}/{entradaId}/file
            if (parts.length < 6) {
                throw new InvalidObjectKeyException("objectKey de staging inválido");
            }
            return parts[5];
        }
        for (int i = 0; i < parts.length - 1; i++) {
            if ("ink-estimates".equals(parts[i]) && i + 1 < parts.length) {
                return parts[i + 1];
            }
        }
        throw new InvalidObjectKeyException("objectKey sin entradaId");
    }

    private static boolean isCanonicalStaging(String[] parts) {
        return parts.length == 7
                && "tmp".equals(parts[0])
                && "company".equals(parts[1])
                && "ink-estimates".equals(parts[3]);
    }

    private static boolean isLegacyStaging(String[] parts) {
        return parts.length == 7
                && "company".equals(parts[0])
                && "tmp".equals(parts[2])
                && "ink-estimates".equals(parts[3]);
    }

    private static String stagingBase(String companyId, String userId, String entradaId) {
        return canonicalStagingUserPrefix(companyId, userId) + requireSegment(entradaId, "entradaId");
    }

    private static String canonicalStagingUserPrefix(String companyId, String userId) {
        return stagingCompanyPrefix(companyId)
                + "ink-estimates/" + requireSegment(userId, "userId") + "/";
    }

    private static String legacyStagingUserPrefix(String companyId, String userId) {
        return companyPrefix(companyId)
                + "tmp/ink-estimates/" + requireSegment(userId, "userId") + "/";
    }

    private static String stagingCompanyPrefix(String companyId) {
        return STAGING_BUCKET_PREFIX + "company/" + requireSegment(companyId, "companyId") + "/";
    }

    private static String companyPrefix(String companyId) {
        return "company/" + requireSegment(companyId, "companyId") + "/";
    }

    private static String definitiveBase(
            String companyId,
            String productionOrderId,
            String plateId,
            String entradaId
    ) {
        return "company/" + requireSegment(companyId, "companyId")
                + "/production-orders/" + requireSegment(productionOrderId, "productionOrderId")
                + "/prints/" + requireSegment(plateId, "plateId")
                + "/ink-estimates/" + requireSegment(entradaId, "entradaId");
    }

    private static String[] splitKey(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return new String[0];
        }
        return objectKey.split("/", -1);
    }

    private static String requireSegment(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new InvalidObjectKeyException(fieldName + " es obligatorio para construir objectKey");
        }
        String segment = value.trim();
        if (!SAFE_SEGMENT.matcher(segment).matches()) {
            throw new InvalidObjectKeyException(fieldName + " contiene caracteres no permitidos");
        }
        return segment;
    }

    private static String normalizeExtension(String extension) {
        if (extension == null || extension.isBlank()) {
            return "bin";
        }
        String ext = extension.trim().toLowerCase(Locale.ROOT);
        if (ext.startsWith(".")) {
            ext = ext.substring(1);
        }
        if (!SAFE_EXTENSION.matcher(ext).matches()) {
            throw new InvalidObjectKeyException("Extensión de archivo no permitida");
        }
        return ext;
    }
}
