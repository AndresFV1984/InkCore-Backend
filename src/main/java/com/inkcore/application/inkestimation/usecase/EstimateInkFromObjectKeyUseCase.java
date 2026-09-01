package com.inkcore.application.inkestimation.usecase;

import com.inkcore.application.inkestimateasset.InkEstimateAssetFileValidator;
import com.inkcore.application.inkestimateasset.usecase.InkEstimateAssetSupport;
import com.inkcore.domain.inkestimation.exception.InkEstimationBusyException;
import com.inkcore.domain.inkestimation.model.InkEstimateRequest;
import com.inkcore.domain.inkestimation.model.InkEstimateResult;
import com.inkcore.domain.inkestimation.model.InkPageSelection;
import com.inkcore.domain.inkestimation.ports.in.EstimateInkUseCase;
import com.inkcore.infrastructure.config.InkEstimationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Semaphore;

@Service
public class EstimateInkFromObjectKeyUseCase {

    private static final Logger log = LoggerFactory.getLogger(EstimateInkFromObjectKeyUseCase.class);

    private final InkEstimateAssetSupport assetSupport;
    private final InkEstimateAssetFileValidator fileValidator;
    private final EstimateInkUseCase estimateInkUseCase;
    private final InkEstimationProperties properties;
    private final Semaphore downloadSlots;

    public EstimateInkFromObjectKeyUseCase(
            InkEstimateAssetSupport assetSupport,
            InkEstimateAssetFileValidator fileValidator,
            EstimateInkUseCase estimateInkUseCase,
            InkEstimationProperties properties
    ) {
        this.assetSupport = assetSupport;
        this.fileValidator = fileValidator;
        this.estimateInkUseCase = estimateInkUseCase;
        this.properties = properties;
        int max = properties.getMaxConcurrentObjectDownloads();
        this.downloadSlots = max > 0 ? new Semaphore(max) : null;
    }

    public InkEstimateResult execute(EstimateInkFromObjectKeyCommand command, Authentication authentication) {
        if (command.objectKey() == null || command.objectKey().isBlank()) {
            throw new IllegalArgumentException("objectKey es obligatorio");
        }
        if (command.sheetCount() == null || command.sheetCount() < 1) {
            throw new IllegalArgumentException("sheetCount es obligatorio y debe ser > 0");
        }
        if (command.widthCm() != null && command.widthCm() < 0.01) {
            throw new IllegalArgumentException("widthCm debe ser ≥ 0.01");
        }
        if (command.heightCm() != null && command.heightCm() < 0.01) {
            throw new IllegalArgumentException("heightCm debe ser ≥ 0.01");
        }
        if (command.dpi() != null && command.dpi() < 72) {
            throw new IllegalArgumentException("dpi debe ser ≥ 72");
        }
        if (command.gramsPerCm2() != null && command.gramsPerCm2() <= 0) {
            throw new IllegalArgumentException("gramsPerCm2 debe ser > 0");
        }

        String objectKey = command.objectKey().trim();
        String companyId = assetSupport.companyId(authentication);
        String userId = assetSupport.userId(authentication);
        String fileName = InkEstimateAssetSupport.fileNameFromKey(objectKey);
        String contentType = fileValidator.resolveOriginalContentType(fileName, null);

        acquireDownloadSlot();
        Path tempFile = null;
        try {
            tempFile = createTempFile(fileName);
            long downloadStarted = System.nanoTime();
            assetSupport.downloadAuthorizedObject(
                    objectKey,
                    companyId,
                    userId,
                    null,
                    tempFile,
                    fileValidator.maxOriginalBytes()
            );
            long downloadMs = (System.nanoTime() - downloadStarted) / 1_000_000L;
            fileValidator.validateOriginal(tempFile, fileName, contentType);

            double widthCm = command.widthCm() != null ? command.widthCm() : properties.getDefaultWidthCm();
            double heightCm = command.heightCm() != null ? command.heightCm() : properties.getDefaultHeightCm();
            List<Integer> pages = InkPageSelection.normalize(command.pages());

            InkEstimateResult result = estimateInkUseCase.estimate(new InkEstimateRequest(
                    tempFile,
                    fileName,
                    contentType,
                    widthCm,
                    heightCm,
                    command.sheetCount(),
                    command.dpi(),
                    command.gramsPerCm2(),
                    userId,
                    command.iccProfile(),
                    pages
            ));
            log.info(
                    "Estimación tinta objectKey={} downloadMs={} processingTimeMs={} sizeBytes={}",
                    objectKey,
                    downloadMs,
                    result.getProcessingTimeMs(),
                    result.getOriginalSizeBytes()
            );
            return result;
        } finally {
            deleteQuietly(tempFile);
            releaseDownloadSlot();
        }
    }

    private void acquireDownloadSlot() {
        if (downloadSlots == null) {
            return;
        }
        if (!downloadSlots.tryAcquire()) {
            throw new InkEstimationBusyException(
                    "Hay demasiadas estimaciones en curso. Reintente en unos segundos."
            );
        }
    }

    private void releaseDownloadSlot() {
        if (downloadSlots != null) {
            downloadSlots.release();
        }
    }

    private static Path createTempFile(String fileName) {
        try {
            String suffix = fileName != null && fileName.contains(".")
                    ? fileName.substring(fileName.lastIndexOf('.'))
                    : ".bin";
            return Files.createTempFile("inkcore-estimate-", suffix);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo crear el archivo temporal de estimación", ex);
        }
    }

    private static void deleteQuietly(Path tempFile) {
        if (tempFile == null) {
            return;
        }
        try {
            Files.deleteIfExists(tempFile);
        } catch (IOException ex) {
            tempFile.toFile().deleteOnExit();
        }
    }

    public record EstimateInkFromObjectKeyCommand(
            String objectKey,
            Integer sheetCount,
            Double widthCm,
            Double heightCm,
            Integer dpi,
            Double gramsPerCm2,
            String iccProfile,
            List<Integer> pages
    ) {
    }
}
