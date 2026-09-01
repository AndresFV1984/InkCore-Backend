package com.inkcore.application.inkestimation.usecase;

import com.inkcore.application.inkestimateasset.InkEstimateAssetFileValidator;
import com.inkcore.application.shared.AuthenticatedCompanyResolver;
import com.inkcore.domain.inkestimation.model.InkEstimateRequest;
import com.inkcore.domain.inkestimation.model.InkEstimateResult;
import com.inkcore.domain.inkestimation.model.InkPageSelection;
import com.inkcore.domain.inkestimation.ports.in.EstimateInkUseCase;
import com.inkcore.infrastructure.config.InkEstimationProperties;
import com.inkcore.infrastructure.config.InkMediaUploadLimits;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Estimación directa vía multipart (formulario "Estimar tintas" sin presign S3).
 */
@Service
public class EstimateInkFromMultipartUseCase {

    private final InkEstimateAssetFileValidator fileValidator;
    private final EstimateInkUseCase estimateInkUseCase;
    private final InkMediaUploadLimits uploadLimits;
    private final InkEstimationProperties properties;
    private final AuthenticatedCompanyResolver companyResolver;

    public EstimateInkFromMultipartUseCase(
            InkEstimateAssetFileValidator fileValidator,
            EstimateInkUseCase estimateInkUseCase,
            InkMediaUploadLimits uploadLimits,
            InkEstimationProperties properties,
            AuthenticatedCompanyResolver companyResolver
    ) {
        this.fileValidator = fileValidator;
        this.estimateInkUseCase = estimateInkUseCase;
        this.uploadLimits = uploadLimits;
        this.properties = properties;
        this.companyResolver = companyResolver;
    }

    public InkEstimateResult execute(EstimateInkFromMultipartCommand command, Authentication authentication) {
        MultipartFile file = command.file();
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo es obligatorio");
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

        String fileName = file.getOriginalFilename() != null && !file.getOriginalFilename().isBlank()
                ? file.getOriginalFilename().trim()
                : "upload.bin";
        String contentType = fileValidator.resolveOriginalContentType(fileName, file.getContentType());
        long size = file.getSize();
        long maxBytes = uploadLimits.effectiveMaxOriginalBytes();
        if (size > maxBytes) {
            throw new com.inkcore.domain.inkestimation.exception.InkFileTooLargeException(
                    "El archivo supera el tamaño máximo permitido (" + maxBytes + " bytes)"
            );
        }
        fileValidator.validateOriginalMetadata(fileName, contentType, size);

        double widthCm = command.widthCm() != null ? command.widthCm() : properties.getDefaultWidthCm();
        double heightCm = command.heightCm() != null ? command.heightCm() : properties.getDefaultHeightCm();
        List<Integer> pages = InkPageSelection.parse(command.pagesRaw());
        String userId = companyResolver.resolveUserId(authentication);

        Path tempFile = null;
        try {
            tempFile = createTempFile(fileName);
            file.transferTo(tempFile);
            fileValidator.validateOriginal(tempFile, fileName, contentType);

            InkEstimateRequest request = new InkEstimateRequest(
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
            );
            return estimateInkUseCase.estimate(request);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo leer el archivo de estimación", ex);
        } finally {
            deleteQuietly(tempFile);
        }
    }

    private static Path createTempFile(String fileName) throws IOException {
        String suffix = fileName.contains(".")
                ? fileName.substring(fileName.lastIndexOf('.'))
                : ".bin";
        return Files.createTempFile("inkcore-estimate-multipart-", suffix);
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

    public record EstimateInkFromMultipartCommand(
            MultipartFile file,
            Integer sheetCount,
            Double widthCm,
            Double heightCm,
            Integer dpi,
            Double gramsPerCm2,
            String iccProfile,
            String pagesRaw
    ) {
    }
}
