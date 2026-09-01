package com.inkcore.application.inkestimation.usecase;

import com.inkcore.application.inkestimateasset.InkEstimateAssetFileValidator;
import com.inkcore.application.shared.AuthenticatedCompanyResolver;
import com.inkcore.domain.inkestimation.model.InkEstimateRequest;
import com.inkcore.domain.inkestimation.model.InkEstimateResult;
import com.inkcore.domain.inkestimation.ports.in.EstimateInkUseCase;
import com.inkcore.infrastructure.config.InkEstimationProperties;
import com.inkcore.infrastructure.config.InkMediaUploadLimits;
import com.inkcore.infrastructure.config.ObjectStorageProperties;
import com.inkcore.application.inkestimation.usecase.EstimateInkFromMultipartUseCase.EstimateInkFromMultipartCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EstimateInkFromMultipartUseCaseTest {

    @Mock EstimateInkUseCase estimateInkUseCase;
    @Mock AuthenticatedCompanyResolver companyResolver;
    @Mock Authentication authentication;

    private EstimateInkFromMultipartUseCase useCase;

    @BeforeEach
    void setUp() {
        InkEstimationProperties properties = new InkEstimationProperties();
        properties.setDefaultWidthCm(70);
        properties.setDefaultHeightCm(100);
        properties.setAbsoluteMaxFileBytes(512L * 1024L * 1024L);
        ObjectStorageProperties objectStorage = new ObjectStorageProperties();
        objectStorage.setMaxAssetFileBytes(512L * 1024L * 1024L);
        InkMediaUploadLimits uploadLimits = new InkMediaUploadLimits(properties, objectStorage);

        InkEstimateAssetFileValidator validator = new InkEstimateAssetFileValidator(
                new InkEstimateAssetFileValidator.ObjectStoragePropertiesReader(
                        uploadLimits.effectiveMaxOriginalBytes(),
                        2_097_152L
                )
        );
        useCase = new EstimateInkFromMultipartUseCase(
                validator,
                estimateInkUseCase,
                uploadLimits,
                properties,
                companyResolver
        );
    }

    @Test
    void execute_acceptsImageAndEstimates() {
        when(companyResolver.resolveUserId(authentication)).thenReturn("user-1");
        when(estimateInkUseCase.estimate(any(InkEstimateRequest.class))).thenReturn(
                new InkEstimateResult(
                        "arte.jpg",
                        "image/jpeg",
                        70,
                        100,
                        7000,
                        100,
                        300,
                        0.00021,
                        1000,
                        1000,
                        "FOGRA39.icc",
                        List.of(),
                        List.of(),
                        0.1,
                        0,
                        0.1,
                        10,
                        0,
                        10,
                        50L,
                        1024L,
                        List.of(),
                        false,
                        false,
                        List.of(),
                        "littlecms"
                )
        );

        byte[] jpegHeader = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0, 0, 0, 0, 0};
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "arte.jpg",
                "image/jpeg",
                jpegHeader
        );

        InkEstimateResult result = useCase.execute(
                new EstimateInkFromMultipartCommand(file, 100, null, null, null, null, null, null),
                authentication
        );

        assertEquals(10, result.getTotalGramsOrder());
        verify(estimateInkUseCase).estimate(any(InkEstimateRequest.class));
    }

    @Test
    void execute_emptyFile_rejects() {
        MockMultipartFile file = new MockMultipartFile("file", "arte.jpg", "image/jpeg", new byte[0]);
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(
                new EstimateInkFromMultipartCommand(file, 100, null, null, null, null, null, null),
                authentication
        ));
        verify(estimateInkUseCase, never()).estimate(any());
    }
}
