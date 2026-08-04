package com.inkcore.domain.colorconversion.ports.out;

import com.inkcore.domain.colorconversion.model.ConversionRequest;
import com.inkcore.domain.colorconversion.model.CmykTiffConversion;
import com.inkcore.domain.colorconversion.model.RasterImageInfo;

/**
 * Conversión ICC RGB→CMYK para TIFF, JPG y PNG (salida TIFF CMYK LZW).
 */
public interface ImageColorConverterPort {

    /**
     * Convierte a TIFF CMYK y genera preview JPEG desde el CMYK en memoria (ICC),
     * más fiable que re-leer el TIFF escrito.
     */
    CmykTiffConversion convertToCmykTiff(
            ConversionRequest request,
            String sourceIccProfileName,
            String destinationIccProfileName
    );

    RasterImageInfo readInfo(byte[] imageBytes, String hintFileName);

    RasterImageInfo readTiffInfo(byte[] tiffBytes);

    /**
     * Soft-proof RGB (JPEG) a resolución nativa del TIFF CMYK, usando el ICC embebido.
     */
    byte[] softProofRgbJpeg(byte[] cmykTiffBytes, float jpegQuality);
}
