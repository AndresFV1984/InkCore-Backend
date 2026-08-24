package com.inkcore.application.inkestimateasset.usecase;

import com.inkcore.application.inkestimateasset.InkEstimateAssetFileValidator;
import com.inkcore.application.shared.AuthenticatedCompanyResolver;
import com.inkcore.domain.objectstorage.model.PresignedUpload;
import com.inkcore.domain.objectstorage.ports.out.ObjectStoragePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PresignStagingInkEstimateAssetUseCaseTest {

    @Mock ObjectStoragePort objectStorage;
    @Mock AuthenticatedCompanyResolver companyResolver;
    @Mock Authentication authentication;

    private PresignStagingInkEstimateAssetUseCase useCase;

    @BeforeEach
    void setUp() {
        InkEstimateAssetFileValidator validator = new InkEstimateAssetFileValidator(
                new InkEstimateAssetFileValidator.ObjectStoragePropertiesReader(26_214_400L, 2_097_152L)
        );
        InkEstimateAssetSupport support = new InkEstimateAssetSupport(
                companyResolver,
                objectStorage,
                new InkEstimateAssetSupport.InkEstimateAssetStorageSettings(Duration.ofSeconds(300))
        );
        useCase = new PresignStagingInkEstimateAssetUseCase(support, validator);
    }

    @Test
    void execute_presignsPutAndDoesNotUploadBytes() {
        when(companyResolver.resolveCompanyId(authentication)).thenReturn("c1");
        when(companyResolver.resolveUserId(authentication)).thenReturn("u1");
        when(objectStorage.createPresignedPutUrl(anyString(), eq("application/pdf"), any(), anyMap(), anyMap()))
                .thenReturn(new PresignedUpload(
                        "https://s3.example/original",
                        "PUT",
                        Map.of("Content-Type", "application/pdf"),
                        Duration.ofSeconds(300)
                ));

        InkEstimateAssetPresignResult result = useCase.execute(
                new PresignInkEstimateAssetCommand(
                        "plate-1",
                        "entry-1",
                        "flyer.pdf",
                        "application/pdf",
                        184320L,
                        null,
                        null,
                        null,
                        null
                ),
                authentication
        );

        assertEquals("tmp/company/c1/ink-estimates/u1/entry-1/original.pdf", result.objectKey());
        assertNotNull(result.upload());
        assertEquals("https://s3.example/original", result.upload().url());
        verify(objectStorage, never()).putObject(anyString(), any(), anyString());
    }

    @Test
    void execute_rejectsMissingOriginalAndPreview() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(
                new PresignInkEstimateAssetCommand("plate-1", "entry-1", null, null, null, null, null, null, null),
                authentication
        ));
        verify(objectStorage, never()).createPresignedPutUrl(anyString(), anyString(), any(), any(), any());
    }
}
