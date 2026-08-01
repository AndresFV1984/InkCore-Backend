package com.inkcore.domain.inkestimation.ports.out;

import com.inkcore.domain.inkestimation.model.InkCoverageAnalysis;
import com.inkcore.domain.inkestimation.model.InkEstimateRequest;

public interface InkCoverageAnalyzerPort {
    InkCoverageAnalysis analyze(InkEstimateRequest request, int dpi, String destinationIccProfile);
}
