package com.inkcore.application.inkestimateasset;

import com.inkcore.domain.inkestimation.exception.InkFileTooLargeException;
import com.inkcore.domain.inkestimation.exception.UnsupportedInkFileException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

@Component
public class InkEstimateAssetFileValidator {

    private static final Set<String> ORIGINAL_EXTENSIONS = Set.of("jpg", "jpeg", "png", "tif", "tiff", "webp", "gif", "pdf");
    private static final Set<String> ORIGINAL_MIMES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/tiff",
            "image/webp",
            "image/gif"
    );

    private final ObjectStoragePropertiesReader properties;

    public InkEstimateAssetFileValidator(ObjectStoragePropertiesReader properties) {
        this.properties = properties;
    }

    public void validateOriginalMetadata(String fileName, String declaredContentType, Long sizeBytes) {
        if (sizeBytes == null || sizeBytes <= 0) {
            throw new UnsupportedInkFileException("El archivo está vacío");
        }
        if (sizeBytes > properties.maxAssetFileBytes()) {
            throw new InkFileTooLargeException(
                    "El archivo supera el tamaño máximo permitido (" + properties.maxAssetFileBytes() + " bytes)"
            );
        }
        String extension = extensionOf(fileName);
        if (!ORIGINAL_EXTENSIONS.contains(extension)) {
            throw new UnsupportedInkFileException(
                    "Formato no soportado. Use JPG, PNG, TIFF, WEBP, GIF o PDF"
            );
        }
        String mime = normalizeMime(declaredContentType);
        if (mime != null && !ORIGINAL_MIMES.contains(mime)) {
            throw new UnsupportedInkFileException("Tipo MIME no permitido para el arte original");
        }
    }

    public void validateOriginal(String fileName, String declaredContentType, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new UnsupportedInkFileException("El archivo está vacío");
        }
        validateOriginalMetadata(fileName, declaredContentType, (long) bytes.length);
        assertMagicBytes(bytes, extensionOf(fileName));
    }

    public void validateOriginal(Path file, String fileName, String declaredContentType) {
        if (file == null || !Files.isRegularFile(file)) {
            throw new UnsupportedInkFileException("El archivo está vacío");
        }
        try {
            long size = Files.size(file);
            validateOriginalMetadata(fileName, declaredContentType, size);
            byte[] header = readHeader(file, 16);
            assertMagicBytes(header, extensionOf(fileName));
        } catch (IOException ex) {
            throw new UnsupportedInkFileException("No se pudo leer el archivo de estimación");
        }
    }

    public long maxOriginalBytes() {
        return properties.maxAssetFileBytes();
    }

    public void validatePreviewMetadata(String declaredContentType, Long sizeBytes) {
        if (sizeBytes == null || sizeBytes <= 0) {
            throw new UnsupportedInkFileException("La miniatura está vacía");
        }
        if (sizeBytes > properties.maxPreviewFileBytes()) {
            throw new InkFileTooLargeException("La miniatura supera el tamaño máximo permitido");
        }
        String mime = normalizeMime(declaredContentType);
        if (mime != null && !"image/jpeg".equals(mime)) {
            throw new UnsupportedInkFileException("La miniatura debe ser JPEG");
        }
    }

    public void validatePreview(byte[] bytes) {
        validatePreviewMetadata("image/jpeg", bytes == null ? null : (long) bytes.length);
        if (!startsWith(bytes, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF})) {
            throw new UnsupportedInkFileException("La miniatura debe ser JPEG");
        }
    }

    public String resolveOriginalContentType(String fileName, String declaredContentType) {
        String mime = normalizeMime(declaredContentType);
        if (mime != null && ORIGINAL_MIMES.contains(mime)) {
            return mime;
        }
        return switch (extensionOf(fileName)) {
            case "pdf" -> "application/pdf";
            case "png" -> "image/png";
            case "tif", "tiff" -> "image/tiff";
            case "webp" -> "image/webp";
            case "gif" -> "image/gif";
            default -> "image/jpeg";
        };
    }

    private static void assertMagicBytes(byte[] bytes, String extension) {
        boolean ok = switch (extension) {
            case "pdf" -> startsWith(bytes, "%PDF".getBytes());
            case "jpg", "jpeg" -> startsWith(bytes, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
            case "png" -> startsWith(bytes, new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47});
            case "gif" -> startsWith(bytes, "GIF8".getBytes());
            case "webp" -> bytes.length >= 12
                    && startsWith(bytes, "RIFF".getBytes())
                    && new String(bytes, 8, 4).equals("WEBP");
            case "tif", "tiff" -> bytes.length >= 4
                    && ((bytes[0] == 'I' && bytes[1] == 'I' && bytes[2] == '*' && bytes[3] == 0)
                    || (bytes[0] == 'M' && bytes[1] == 'M' && bytes[2] == 0 && bytes[3] == '*'));
            default -> false;
        };
        if (!ok) {
            throw new UnsupportedInkFileException("El contenido del archivo no coincide con su extensión");
        }
    }

    private static byte[] readHeader(Path file, int maxBytes) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            return in.readNBytes(maxBytes);
        }
    }

    private static boolean startsWith(byte[] bytes, byte[] prefix) {
        if (bytes.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (bytes[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private static String extensionOf(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private static String normalizeMime(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return null;
        }
        return contentType.split(";")[0].trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Facade sobre propiedades de infraestructura para mantener el validador en application.
     */
    public record ObjectStoragePropertiesReader(long maxAssetFileBytes, long maxPreviewFileBytes) {
    }
}
