package com.inkcore.infrastructure.out.colorconversion;

import com.inkcore.domain.colorconversion.exception.ColorConversionFailedException;
import com.inkcore.domain.colorconversion.exception.UnsupportedColorFileException;
import com.inkcore.domain.colorconversion.model.ConversionRequest;
import com.inkcore.domain.colorconversion.model.RasterImageInfo;
import com.inkcore.domain.colorconversion.model.RenderingIntent;
import com.inkcore.infrastructure.config.ColorConversionProperties;
import com.inkcore.infrastructure.out.cache.InMemoryIccProfileCacheAdapter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class PdfColorConverterIntegrityIT {

    private PdfColorConverterAdapter adapter;
    private ColorConversionProperties properties;

    @BeforeEach
    void setUp() {
        properties = new ColorConversionProperties();
        properties.setPdfEnabled(true);
        properties.setMinImageResolutionDpi(72);
        IccProfileLoader loader = new IccProfileLoader(new InMemoryIccProfileCacheAdapter(properties));
        ImageColorConverterAdapter imageConverter = new ImageColorConverterAdapter(loader, properties);
        adapter = new PdfColorConverterAdapter(properties, imageConverter);
    }

    @Test
    void readInfo_countsPages() throws Exception {
        byte[] pdf = samplePdf(2);
        RasterImageInfo info = adapter.readInfo(pdf);
        assertEquals(2, info.getPageCount());
    }

    @Test
    void convert_whenPdfDisabled_throwsDomainException() throws Exception {
        properties.setPdfEnabled(false);
        ConversionRequest request = new ConversionRequest(
                samplePdf(1), "a.pdf", "application/pdf", RenderingIntent.PERCEPTUAL, null, null
        );
        assertThrows(
                UnsupportedColorFileException.class,
                () -> adapter.convertToCmykPdf(request, "sRGB.icc", "FOGRA39.icc")
        );
    }

    @Test
    void convert_preservesPageCountAndDpiMetadata() throws Exception {
        assumeTrue(classpathHas("color-profiles/sRGB.icc"));
        assumeTrue(classpathHas("color-profiles/FOGRA39.icc"));

        byte[] input = samplePdf(1);
        ConversionRequest request = new ConversionRequest(
                input, "sample.pdf", "application/pdf", RenderingIntent.PERCEPTUAL, null, null
        );
        RasterImageInfo before = adapter.readInfo(input);
        byte[] output = adapter.convertToCmykPdf(request, "sRGB.icc", "FOGRA39.icc");
        RasterImageInfo after = adapter.readInfo(output);

        assertEquals(before.getPageCount(), after.getPageCount());
        assertEquals(before.getWidthPx(), after.getWidthPx());
        assertEquals(before.getHeightPx(), after.getHeightPx());
        assertEquals(before.getXResolutionDpi(), after.getXResolutionDpi());
        assertEquals(before.getYResolutionDpi(), after.getYResolutionDpi());
        assertTrue(output.length > 0);
        assertTrue(output.length > 4 && output[0] == '%' && output[1] == 'P');
    }

    @Test
    void createFlateCmykImage_rejectsNullDocumentViaConversionPath() {
        // Smoke: disabled path already covered; ensure failure on corrupt bytes is domain exception
        ConversionRequest request = new ConversionRequest(
                new byte[]{1, 2, 3}, "bad.pdf", "application/pdf", RenderingIntent.PERCEPTUAL, null, null
        );
        assertThrows(
                ColorConversionFailedException.class,
                () -> adapter.convertToCmykPdf(request, "sRGB.icc", "FOGRA39.icc")
        );
    }

    private static boolean classpathHas(String resource) {
        return PdfColorConverterIntegrityIT.class.getClassLoader().getResource(resource) != null;
    }

    static byte[] samplePdf(int pages) throws Exception {
        try (PDDocument doc = new PDDocument()) {
            for (int i = 0; i < pages; i++) {
                PDPage page = new PDPage(PDRectangle.A4);
                doc.addPage(page);
                BufferedImage img = new BufferedImage(40, 40, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = img.createGraphics();
                g.setColor(i % 2 == 0 ? Color.RED : Color.BLUE);
                g.fillRect(0, 0, 40, 40);
                g.dispose();
                var xImage = LosslessFactory.createFromImage(doc, img);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.drawImage(xImage, 50, 50, 100, 100);
                }
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }
}
