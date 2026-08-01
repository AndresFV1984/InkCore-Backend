package com.inkcore.domain.colorconversion.ports.in;

import com.inkcore.domain.colorconversion.model.ConversionRequest;
import com.inkcore.domain.colorconversion.model.ConversionResult;

public interface ConvertColorSpaceUseCase {

    ConversionResult convert(ConversionRequest request);
}
