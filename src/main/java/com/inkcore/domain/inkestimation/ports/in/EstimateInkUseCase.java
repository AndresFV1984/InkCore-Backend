package com.inkcore.domain.inkestimation.ports.in;

import com.inkcore.domain.inkestimation.model.InkEstimateRequest;
import com.inkcore.domain.inkestimation.model.InkEstimateResult;

public interface EstimateInkUseCase {
    InkEstimateResult estimate(InkEstimateRequest request);
}
