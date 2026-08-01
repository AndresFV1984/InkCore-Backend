package com.inkcore.application.colorconversion;

import com.inkcore.domain.colorconversion.exception.ConversionIntegrityException;
import com.inkcore.domain.colorconversion.exception.UnsupportedColorFileException;
import com.inkcore.domain.colorconversion.model.ConversionHistory;
import com.inkcore.domain.colorconversion.model.ConversionRequest;
import com.inkcore.domain.colorconversion.model.ConversionResult;
import com.inkcore.domain.colorconversion.model.OutputFormat;
import com.inkcore.domain.colorconversion.model.RasterImageInfo;
import com.inkcore.domain.colorconversion.model.RenderingIntent;
import com.inkcore.domain.colorconversion.ports.out.ConversionHistoryRepositoryPort;
import com.inkcore.domain.colorconversion.ports.out.ImageColorConverterPort;
import com.inkcore.domain.colorconversion.ports.out.PdfColorConverterPort;
import com.inkcore.infrastructure.config.ColorConversionProperties;
import com.inkcore.infrastructure.out.colorconversion.IccProfileLoader;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConvertColorSpaceServiceTest {

    @Mock
    private ImageColorConverterPort imageColorConverter;
    @Mock
    private PdfColorConverterPort pdfColorConverter;
    @Mock
    private ConversionHistoryRepositoryPort conversionHistoryRepository;
    @Mock
    private IccProfileLoader iccProfileLoader;

    private ConvertColorSpaceService service;

    @BeforeEach
    void setUp() {
        ColorConversionProperties properties = new ColorConversionProperties();
        properties.setSourceIccProfile("sRGB.icc");
        properties.setDestinationIccProfile("FOGRA39.icc");
        lenient().when(iccProfileLoader.resolveExistingFileName(anyString())).thenAnswer(inv -> {
            String name = inv.getArgument(0);
            if (name == null || name.isBlank()) {
                return "FOGRA39.icc";
            }
            String lower = name.toLowerCase();
            if (lower.contains("iso") || lower.contains("fogra39") || lower.equals("fogra39.icc")) {
                return "FOGRA39.icc";
            }
            return name.endsWith(".icc") || name.endsWith(".icm") ? name : name + ".icc";
        });
        service = new ConvertColorSpaceService(
                imageColorConverter,
                pdfColorConverter,
                conversionHistoryRepository,
                properties,
                iccProfileLoader,
                Clock.fixed(Instant.parse("2026-07-30T12:00:00Z"), ZoneOffset.UTC),
                new SimpleMeterRegistry()
        );
    }

    @Test
    void convertImage_persistsHistoryAndReturnsResult() {
        byte[] input = new byte[]{1, 2, 3};
        byte[] output = new byte[]{4, 5, 6, 7};
        RasterImageInfo info = new RasterImageInfo(10, 20, 300.0, 300.0, 8, "LZW", 1);
        when(imageColorConverter.readInfo(eq(input), anyString())).thenReturn(info);
        when(imageColorConverter.convertToCmykTiff(any(), anyString(), anyString())).thenReturn(output);
        when(imageColorConverter.readTiffInfo(output)).thenReturn(info);
        when(conversionHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ConversionResult result = service.convert(new ConversionRequest(
                input, "sample.png", "image/png", RenderingIntent.PERCEPTUAL, null, "user-1"
        ));

        assertEquals("sample_CMYK.tif", result.getOutputFileName());
        assertEquals(4, result.getFinalSizeBytes());
        assertEquals(10, result.getWidthPx());
        assertEquals(0f, result.getBrightnessLift(), 0.0001f);
        assertEquals(0f, result.getVibranceBoost(), 0.0001f);
        assertEquals(false, result.isSoftProofBrightnessMatch());
        ArgumentCaptor<ConversionHistory> captor = ArgumentCaptor.forClass(ConversionHistory.class);
        verify(conversionHistoryRepository).save(captor.capture());
        assertEquals("user-1", captor.getValue().getUserId());
    }

    @Test
    void convertImage_appliesQualityOverridesFromRequest() {
        byte[] input = new byte[]{1, 2, 3};
        byte[] output = new byte[]{4, 5, 6, 7};
        RasterImageInfo info = new RasterImageInfo(10, 20, 300.0, 300.0, 8, "LZW", 1);
        when(imageColorConverter.readInfo(eq(input), anyString())).thenReturn(info);
        when(imageColorConverter.convertToCmykTiff(any(), anyString(), anyString())).thenReturn(output);
        when(imageColorConverter.readTiffInfo(output)).thenReturn(info);
        when(conversionHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ConversionResult result = service.convert(new ConversionRequest(
                input, "sample.png", "image/png", RenderingIntent.PERCEPTUAL, null, "user-1",
                OutputFormat.TIFF, 0.05f, 0.2f, true
        ));

        assertEquals(0.05f, result.getBrightnessLift(), 0.0001f);
        assertEquals(0.2f, result.getVibranceBoost(), 0.0001f);
        assertTrue(result.isSoftProofBrightnessMatch());
    }

    @Test
    void convertImage_toPdf_whenOutputFormatPdf() {
        byte[] input = new byte[]{1, 2, 3};
        byte[] output = new byte[]{9, 9, 9};
        RasterImageInfo info = new RasterImageInfo(10, 20, 300.0, 300.0, 8, "LZW", 1);
        RasterImageInfo pdfInfo = new RasterImageInfo(10, 20, 300.0, 300.0, 8, "Flate", 1);
        when(imageColorConverter.readInfo(eq(input), anyString())).thenReturn(info);
        when(pdfColorConverter.convertRasterImageToCmykPdf(any(), anyString(), anyString())).thenReturn(output);
        when(pdfColorConverter.readInfo(output)).thenReturn(pdfInfo);
        when(conversionHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ConversionResult result = service.convert(new ConversionRequest(
                input, "sample.png", "image/png", RenderingIntent.PERCEPTUAL, null, "user-1", OutputFormat.PDF
        ));

        assertEquals("sample_CMYK.pdf", result.getOutputFileName());
        assertEquals("application/pdf", result.getOutputMimeType());
        verify(pdfColorConverter).convertRasterImageToCmykPdf(any(), anyString(), anyString());
    }

    @Test
    void convertPdf_toTiff_whenOutputFormatTiff() {
        byte[] input = new byte[]{1, 2, 3};
        byte[] output = new byte[]{7, 7, 7, 7};
        RasterImageInfo pdfInfo = new RasterImageInfo(100, 200, 300.0, 300.0, 8, "Flate", 2);
        RasterImageInfo tiffInfo = new RasterImageInfo(100, 200, 300.0, 300.0, 8, "LZW", 2);
        when(pdfColorConverter.readInfo(input)).thenReturn(pdfInfo);
        when(pdfColorConverter.convertPdfToCmykTiff(any(), anyString(), anyString())).thenReturn(output);
        when(imageColorConverter.readTiffInfo(output)).thenReturn(tiffInfo);
        when(conversionHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ConversionResult result = service.convert(new ConversionRequest(
                input, "doc.pdf", "application/pdf", RenderingIntent.PERCEPTUAL, null, "user-1", OutputFormat.TIFF
        ));

        assertEquals("doc_CMYK.tif", result.getOutputFileName());
        assertEquals("image/tiff", result.getOutputMimeType());
        verify(pdfColorConverter).convertPdfToCmykTiff(any(), anyString(), anyString());
    }

    @Test
    void convert_rejectsUnsupportedExtension() {
        ConversionRequest request = new ConversionRequest(
                new byte[]{1}, "doc.docx", "application/octet-stream", RenderingIntent.PERCEPTUAL, null, null
        );
        assertThrows(UnsupportedColorFileException.class, () -> service.convert(request));
    }

    @Test
    void assertImageIntegrity_failsOnDimensionMismatch() {
        RasterImageInfo in = new RasterImageInfo(10, 10, 300.0, 300.0, 8, null, 1);
        RasterImageInfo out = new RasterImageInfo(10, 11, 300.0, 300.0, 8, "LZW", 1);
        ConversionIntegrityException ex = assertThrows(
                ConversionIntegrityException.class,
                () -> service.assertImageIntegrity(in, out)
        );
        assertTrue(ex.getMessage().contains("Dimensiones"));
    }

    @Test
    void assertImageIntegrity_failsOnJpegCompression() {
        RasterImageInfo in = new RasterImageInfo(10, 10, 300.0, 300.0, 8, null, 1);
        RasterImageInfo out = new RasterImageInfo(10, 10, 300.0, 300.0, 8, "JPEG", 1);
        assertThrows(ConversionIntegrityException.class, () -> service.assertImageIntegrity(in, out));
    }

    @Test
    void assertImageIntegrity_failsOnDpiLoss() {
        RasterImageInfo in = new RasterImageInfo(10, 10, 300.0, 300.0, 8, null, 1);
        RasterImageInfo out = new RasterImageInfo(10, 10, 72.0, 72.0, 8, "LZW", 1);
        assertThrows(ConversionIntegrityException.class, () -> service.assertImageIntegrity(in, out));
    }

    @Test
    void assertImageIntegrity_failsOnBitDepthReduction() {
        RasterImageInfo in = new RasterImageInfo(10, 10, null, null, 16, null, 1);
        RasterImageInfo out = new RasterImageInfo(10, 10, null, null, 8, "LZW", 1);
        assertThrows(ConversionIntegrityException.class, () -> service.assertImageIntegrity(in, out));
    }

    @Test
    void assertPdfIntegrity_failsOnPageCountMismatch() {
        RasterImageInfo in = new RasterImageInfo(0, 0, null, null, 8, null, 3);
        RasterImageInfo out = new RasterImageInfo(0, 0, null, null, 8, null, 2);
        assertThrows(ConversionIntegrityException.class, () -> service.assertPdfIntegrity(in, out));
    }

    @Test
    void assertImageIntegrity_acceptsLosslessMatch() {
        RasterImageInfo in = new RasterImageInfo(100, 80, 300.0, 300.0, 8, null, 1);
        RasterImageInfo out = new RasterImageInfo(100, 80, 300.0, 300.0, 8, "LZW", 1);
        service.assertImageIntegrity(in, out);
    }
}
