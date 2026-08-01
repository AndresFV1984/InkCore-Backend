package com.inkcore.domain.colorconversion.ports.out;

import com.inkcore.domain.colorconversion.model.ConversionRequest;
import com.inkcore.domain.colorconversion.model.RasterImageInfo;

/**
 * Conversión ICC RGB→CMYK para TIFF, JPG y PNG (salida TIFF CMYK LZW).
 */
public interface ImageColorConverterPort {

    byte[] convertToCmykTiff(ConversionRequest request, String sourceIccProfileName, String destinationIccProfileName);

    RasterImageInfo readInfo(byte[] imageBytes, String hintFileName);

    RasterImageInfo readTiffInfo(byte[] tiffBytes);
}
