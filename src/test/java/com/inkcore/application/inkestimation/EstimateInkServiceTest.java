package com.inkcore.application.inkestimation;

import com.inkcore.domain.inkestimation.exception.InkFileTooLargeException;
import com.inkcore.domain.inkestimation.model.InkCoverageAnalysis;
import com.inkcore.domain.inkestimation.model.InkEstimateHistory;
import com.inkcore.domain.inkestimation.model.InkEstimateRequest;
import com.inkcore.domain.inkestimation.model.InkEstimateResult;
import com.inkcore.domain.inkestimation.ports.out.InkCoverageAnalyzerPort;
import com.inkcore.domain.inkestimation.ports.out.InkEstimateHistoryRepositoryPort;
import com.inkcore.infrastructure.config.InkEstimationProperties;
import com.inkcore.infrastructure.config.InkMediaUploadLimits;
import com.inkcore.infrastructure.config.ObjectStorageProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EstimateInkServiceTest {

    @Mock
    private InkCoverageAnalyzerPort analyzer;
    @Mock
    private InkEstimateHistoryRepositoryPort historyRepository;

    private EstimateInkService service;

    @BeforeEach
    void setUp() {
        InkEstimationProperties properties = new InkEstimationProperties();
        properties.setAbsoluteMaxFileBytes(1024);
        properties.setSizeToleranceMaxRatio(2.5);
        ObjectStorageProperties objectStorage = new ObjectStorageProperties();
        objectStorage.setMaxAssetFileBytes(1024);
        InkMediaUploadLimits uploadLimits = new InkMediaUploadLimits(properties, objectStorage);
        service = new EstimateInkService(
                analyzer,
                historyRepository,
                properties,
                uploadLimits,
                Clock.fixed(Instant.parse("2026-07-31T12:00:00Z"), ZoneOffset.UTC),
                new SimpleMeterRegistry()
        );
    }

    @Test
    void estimate_persistsHistory() {
        byte[] png = new byte[100];
        when(analyzer.analyze(any(), anyInt(), anyString())).thenReturn(new InkCoverageAnalysis(
                10, 10, 300, "FOGRA39.icc",
                List.of(
                        new InkCoverageAnalysis.RawInkCoverage("Cian", "C", 10, null, true),
                        new InkCoverageAnalysis.RawInkCoverage("Magenta", "M", 0, null, true),
                        new InkCoverageAnalysis.RawInkCoverage("Amarillo", "Y", 0, null, true),
                        new InkCoverageAnalysis.RawInkCoverage("Negro", "K", 0, null, true)
                ),
                List.of()
        ));
        when(historyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InkEstimateResult result = service.estimate(new InkEstimateRequest(
                png, "a.png", "image/png", 70, 100, 10, 300, 0.00021, "u1", null
        ));

        assertEquals(4, result.getProcessInks().size());
        ArgumentCaptor<InkEstimateHistory> captor = ArgumentCaptor.forClass(InkEstimateHistory.class);
        verify(historyRepository).save(captor.capture());
        assertEquals("u1", captor.getValue().getUserId());
    }

    @Test
    void estimate_rejectsAbsoluteCeiling() {
        byte[] huge = new byte[2048];
        assertThrows(InkFileTooLargeException.class, () -> service.estimate(new InkEstimateRequest(
                huge, "a.png", "image/png", 70, 100, 1, 300, null, "u", null
        )));
    }
}
