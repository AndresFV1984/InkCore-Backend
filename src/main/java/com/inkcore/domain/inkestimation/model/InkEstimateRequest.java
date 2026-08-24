package com.inkcore.domain.inkestimation.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Solicitud de estimación de consumo de tinta (proceso + spot).
 */
public final class InkEstimateRequest {

    private final byte[] fileBytes;
    private final Path sourceFile;
    private final String originalFileName;
    private final String mimeType;
    private final double widthCm;
    private final double heightCm;
    private final int sheetCount;
    private final Integer dpi;
    private final Double gramsPerCm2AtFullCoverage;
    private final String userId;
    private final String analysisIccProfile;
    /** Páginas PDF 1-based a estimar (máx. 2). Vacía = todas. */
    private final List<Integer> pages;

    public InkEstimateRequest(
            byte[] fileBytes,
            String originalFileName,
            String mimeType,
            double widthCm,
            double heightCm,
            int sheetCount,
            Integer dpi,
            Double gramsPerCm2AtFullCoverage,
            String userId,
            String analysisIccProfile
    ) {
        this(
                fileBytes,
                null,
                originalFileName,
                mimeType,
                widthCm,
                heightCm,
                sheetCount,
                dpi,
                gramsPerCm2AtFullCoverage,
                userId,
                analysisIccProfile,
                List.of()
        );
    }

    public InkEstimateRequest(
            byte[] fileBytes,
            String originalFileName,
            String mimeType,
            double widthCm,
            double heightCm,
            int sheetCount,
            Integer dpi,
            Double gramsPerCm2AtFullCoverage,
            String userId,
            String analysisIccProfile,
            List<Integer> pages
    ) {
        this(
                fileBytes,
                null,
                originalFileName,
                mimeType,
                widthCm,
                heightCm,
                sheetCount,
                dpi,
                gramsPerCm2AtFullCoverage,
                userId,
                analysisIccProfile,
                pages
        );
    }

    public InkEstimateRequest(
            Path sourceFile,
            String originalFileName,
            String mimeType,
            double widthCm,
            double heightCm,
            int sheetCount,
            Integer dpi,
            Double gramsPerCm2AtFullCoverage,
            String userId,
            String analysisIccProfile,
            List<Integer> pages
    ) {
        this(
                null,
                sourceFile,
                originalFileName,
                mimeType,
                widthCm,
                heightCm,
                sheetCount,
                dpi,
                gramsPerCm2AtFullCoverage,
                userId,
                analysisIccProfile,
                pages
        );
    }

    private InkEstimateRequest(
            byte[] fileBytes,
            Path sourceFile,
            String originalFileName,
            String mimeType,
            double widthCm,
            double heightCm,
            int sheetCount,
            Integer dpi,
            Double gramsPerCm2AtFullCoverage,
            String userId,
            String analysisIccProfile,
            List<Integer> pages
    ) {
        if (sourceFile != null) {
            if (!Files.isRegularFile(sourceFile)) {
                throw new IllegalArgumentException("El archivo es obligatorio");
            }
            this.fileBytes = null;
            this.sourceFile = sourceFile;
        } else {
            if (fileBytes == null || fileBytes.length == 0) {
                throw new IllegalArgumentException("El archivo es obligatorio");
            }
            this.fileBytes = fileBytes;
            this.sourceFile = null;
        }
        if (widthCm <= 0 || heightCm <= 0) {
            throw new IllegalArgumentException("El área de impresión (ancho × alto cm) debe ser mayor que 0");
        }
        if (sheetCount <= 0) {
            throw new IllegalArgumentException("La cantidad de pliegos debe ser mayor que 0");
        }
        if (dpi != null && dpi <= 0) {
            throw new IllegalArgumentException("El DPI debe ser mayor que 0");
        }
        if (gramsPerCm2AtFullCoverage != null && gramsPerCm2AtFullCoverage <= 0) {
            throw new IllegalArgumentException("El factor g/cm² debe ser mayor que 0");
        }
        this.originalFileName = Objects.requireNonNullElse(originalFileName, "upload");
        this.mimeType = mimeType;
        this.widthCm = widthCm;
        this.heightCm = heightCm;
        this.sheetCount = sheetCount;
        this.dpi = dpi;
        this.gramsPerCm2AtFullCoverage = gramsPerCm2AtFullCoverage;
        this.userId = userId;
        this.analysisIccProfile = analysisIccProfile;
        this.pages = InkPageSelection.normalize(pages);
    }

    public byte[] getFileBytes() {
        return fileBytes;
    }

    public Path getSourceFile() {
        return sourceFile;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public double getWidthCm() {
        return widthCm;
    }

    public double getHeightCm() {
        return heightCm;
    }

    public int getSheetCount() {
        return sheetCount;
    }

    public Integer getDpi() {
        return dpi;
    }

    public Double getGramsPerCm2AtFullCoverage() {
        return gramsPerCm2AtFullCoverage;
    }

    public String getUserId() {
        return userId;
    }

    public String getAnalysisIccProfile() {
        return analysisIccProfile;
    }

    /**
     * Páginas PDF a analizar (1-based). Vacía = todas las páginas del documento.
     */
    public List<Integer> getPages() {
        return pages;
    }

    public long getOriginalSizeBytes() {
        if (sourceFile != null) {
            try {
                return Files.size(sourceFile);
            } catch (IOException ex) {
                throw new IllegalStateException("No se pudo leer el tamaño del archivo", ex);
            }
        }
        return fileBytes.length;
    }

    public double areaCm2() {
        return widthCm * heightCm;
    }
}
