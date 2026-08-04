package com.inkcore.domain.colorconversion.ports.out;

import com.inkcore.domain.colorconversion.model.ConversionRequest;
import com.inkcore.domain.colorconversion.model.CmykPdfConversion;
import com.inkcore.domain.colorconversion.model.RasterImageInfo;

/**
 * Conversión ICC RGB→CMYK para PDF vía Apache PDFBox (Java puro, sin Ghostscript).
 */
public interface PdfColorConverterPort {

    byte[] convertToCmykPdf(ConversionRequest request, String sourceIccProfilePath, String destinationIccProfilePath);

    /**
     * Convierte una imagen raster (TIFF/JPG/PNG) a un PDF CMYK de una página
     * (ICCBased + OutputIntent con el perfil de destino) + preview soft-proof.
     */
    CmykPdfConversion convertRasterImageToCmykPdf(
            ConversionRequest request,
            String sourceIccProfileName,
            String destinationIccProfileName
    );

    /**
     * Rasteriza un PDF a TIFF CMYK (multipágina). Implica pérdida de vectores (esperado).
     */
    byte[] convertPdfToCmykTiff(
            ConversionRequest request,
            String sourceIccProfileName,
            String destinationIccProfileName
    );

    RasterImageInfo readInfo(byte[] pdfBytes);

    boolean isAvailable();
}
