package com.inkcore.application.inkestimation.usecase;

import com.inkcore.application.inkestimateasset.InkEstimateAssetFileValidator;
import com.inkcore.application.inkestimateasset.usecase.InkEstimateAssetSupport;
import com.inkcore.application.shared.AuthenticatedCompanyResolver;
import com.inkcore.domain.inkestimation.exception.InkEstimationBusyException;
import com.inkcore.domain.inkestimation.model.InkEstimateRequest;
import com.inkcore.domain.inkestimation.model.InkEstimateResult;
import com.inkcore.domain.inkestimation.ports.in.EstimateInkUseCase;
import com.inkcore.domain.objectstorage.exception.ObjectStorageAccessDeniedException;
import com.inkcore.domain.objectstorage.exception.ObjectStorageUnavailableException;
import com.inkcore.domain.objectstorage.ports.out.ObjectStoragePort;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import com.inkcore.infrastructure.config.InkEstimationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EstimateInkFromObjectKeyUseCaseTest {

    private static final String KEY = "tmp/company/c1/ink-estimates/u1/entry-1/original.pdf";

    @Mock ObjectStoragePort objectStorage;
    @Mock AuthenticatedCompanyResolver companyResolver;
    @Mock EstimateInkUseCase estimateInkUseCase;
    @Mock Authentication authentication;

    private EstimateInkFromObjectKeyUseCase useCase;

    @BeforeEach
    void setUp() {
        InkEstimateAssetSupport support = new InkEstimateAssetSupport(
                companyResolver,
                objectStorage,
                new InkEstimateAssetSupport.InkEstimateAssetStorageSettings(Duration.ofSeconds(300))
        );
        useCase = new EstimateInkFromObjectKeyUseCase(
                support,
                new InkEstimateAssetFileValidator(
                        new InkEstimateAssetFileValidator.ObjectStoragePropertiesReader(26_214_400L, 2_097_152L)
                ),
                estimateInkUseCase,
                new InkEstimationProperties()
        );
    }

    @Test
    void execute_streamsObjectToTempFileAndEstimates() throws Exception {
        when(companyResolver.resolveCompanyId(authentication)).thenReturn("c1");
        when(companyResolver.resolveUserId(authentication)).thenReturn("u1");
        byte[] pdf = "%PDF-1.4 fake".getBytes();
        doAnswer(invocation -> {
            Path destination = invocation.getArgument(1);
            Files.write(destination, pdf);
            return null;
        }).when(objectStorage).downloadObject(eq(KEY), any(Path.class), anyLong());
        when(estimateInkUseCase.estimate(any(InkEstimateRequest.class))).thenReturn(sampleResult());

        InkEstimateResult result = useCase.execute(
                new EstimateInkFromObjectKeyUseCase.EstimateInkFromObjectKeyCommand(
                        KEY, 1000, 70.0, 100.0, 300, null, "FOGRA39.icc", List.of(1, 2)
                ),
                authentication
        );

        assertEquals("original.pdf", result.getOriginalFileName());
        verify(estimateInkUseCase).estimate(any(InkEstimateRequest.class));
        verify(objectStorage, never()).getObject(KEY);
        verify(objectStorage).downloadObject(eq(KEY), any(Path.class), anyLong());
    }

    @Test
    void execute_rejectsForeignCompanyKey() {
        when(companyResolver.resolveCompanyId(authentication)).thenReturn("c1");
        when(companyResolver.resolveUserId(authentication)).thenReturn("u1");

        assertThrows(ObjectStorageAccessDeniedException.class, () -> useCase.execute(
                new EstimateInkFromObjectKeyUseCase.EstimateInkFromObjectKeyCommand(
                        "tmp/company/other/ink-estimates/u1/entry-1/original.pdf",
                        1000, null, null, null, null, null, null
                ),
                authentication
        ));
    }

    @Test
    void execute_rejectsKeyThatOnlyContainsCompanyId() {
        when(companyResolver.resolveCompanyId(authentication)).thenReturn("c1");
        when(companyResolver.resolveUserId(authentication)).thenReturn("u1");

        assertThrows(ObjectStorageAccessDeniedException.class, () -> useCase.execute(
                new EstimateInkFromObjectKeyUseCase.EstimateInkFromObjectKeyCommand(
                        "tmp/company/c1/secret/company/other/ink-estimates/u1/entry-1/original.pdf",
                        1000, null, null, null, null, null, null
                ),
                authentication
        ));
    }

    @Test
    void execute_missingObject_notFound() {
        when(companyResolver.resolveCompanyId(authentication)).thenReturn("c1");
        when(companyResolver.resolveUserId(authentication)).thenReturn("u1");
        doThrow(new IllegalArgumentException("Objeto no encontrado: " + KEY))
                .when(objectStorage).downloadObject(eq(KEY), any(Path.class), anyLong());

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(
                new EstimateInkFromObjectKeyUseCase.EstimateInkFromObjectKeyCommand(
                        KEY, 1000, null, null, null, null, null, null
                ),
                authentication
        ));
    }

    @Test
    void execute_propagatesObjectStorageUnavailable() {
        when(companyResolver.resolveCompanyId(authentication)).thenReturn("c1");
        when(companyResolver.resolveUserId(authentication)).thenReturn("u1");
        doThrow(new ObjectStorageUnavailableException("Object storage no disponible durante download. Reintente."))
                .when(objectStorage).downloadObject(eq(KEY), any(Path.class), anyLong());

        assertThrows(ObjectStorageUnavailableException.class, () -> useCase.execute(
                new EstimateInkFromObjectKeyUseCase.EstimateInkFromObjectKeyCommand(
                        KEY, 1000, null, null, null, null, null, null
                ),
                authentication
        ));
        verify(estimateInkUseCase, never()).estimate(any(InkEstimateRequest.class));
    }

    @Test
    void execute_rejectsWhenConcurrentDownloadLimitReached() {
        InkEstimationProperties limited = new InkEstimationProperties();
        limited.setMaxConcurrentObjectDownloads(1);
        EstimateInkFromObjectKeyUseCase limitedUseCase = new EstimateInkFromObjectKeyUseCase(
                new InkEstimateAssetSupport(
                        companyResolver,
                        objectStorage,
                        new InkEstimateAssetSupport.InkEstimateAssetStorageSettings(Duration.ofSeconds(300))
                ),
                new InkEstimateAssetFileValidator(
                        new InkEstimateAssetFileValidator.ObjectStoragePropertiesReader(26_214_400L, 2_097_152L)
                ),
                estimateInkUseCase,
                limited
        );
        when(companyResolver.resolveCompanyId(authentication)).thenReturn("c1");
        when(companyResolver.resolveUserId(authentication)).thenReturn("u1");
        doAnswer(invocation -> {
            assertThrows(InkEstimationBusyException.class, () -> limitedUseCase.execute(
                    new EstimateInkFromObjectKeyUseCase.EstimateInkFromObjectKeyCommand(
                            KEY, 1000, null, null, null, null, null, null
                    ),
                    authentication
            ));
            Path destination = invocation.getArgument(1);
            Files.write(destination, "%PDF-1.4 fake".getBytes());
            return null;
        }).when(objectStorage).downloadObject(eq(KEY), any(Path.class), anyLong());
        when(estimateInkUseCase.estimate(any(InkEstimateRequest.class))).thenReturn(sampleResult());

        InkEstimateResult result = limitedUseCase.execute(
                new EstimateInkFromObjectKeyUseCase.EstimateInkFromObjectKeyCommand(
                        KEY, 1000, 70.0, 100.0, 300, null, "FOGRA39.icc", List.of(1)
                ),
                authentication
        );

        assertEquals("original.pdf", result.getOriginalFileName());
    }

    private static InkEstimateResult sampleResult() {
        return new InkEstimateResult(
                "original.pdf",
                "application/pdf",
                70, 100, 7000, 1000, 300, 0.00021, 1, 1, "FOGRA39.icc",
                List.of(), List.of(),
                0, 0, 0, 0, 0, 0, 1, 12,
                List.of(1, 2), false, false, List.of(), "littlecms"
        );
    }
}
