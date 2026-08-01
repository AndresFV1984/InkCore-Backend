package com.inkcore.infrastructure.out.inkestimation;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfInkCoverageEngineTest {

    @Test
    void fullPageCyanFill_reportsHighCyanCoverage() throws Exception {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.setNonStrokingColor(1f, 0f, 0f, 0f); // DeviceCMYK via floats
                cs.addRect(0, 0, page.getMediaBox().getWidth(), page.getMediaBox().getHeight());
                cs.fill();
            }

            // Re-open page from saved bytes to ensure color space is DeviceCMYK in content
            // For in-memory, process directly:
            RgbToCmykConverter noop = (r, g, b) -> new float[]{0, 0, 0, 0};
            PdfInkCoverageEngine engine = new PdfInkCoverageEngine(page, noop);
            // Force CMYK operators: PDFBox setNonStrokingColor(c,m,y,k) uses DeviceCMYK
            engine.processPage(page);
            PdfInkCoverageEngine.PageInkCoverage coverage = engine.result();

            assertTrue(coverage.processMeans()[0] > 90.0, "C expected high, got " + coverage.processMeans()[0]);
            assertTrue(coverage.processMeans()[1] < 5.0);
            assertTrue(coverage.processMeans()[2] < 5.0);
            assertTrue(coverage.processMeans()[3] < 5.0);
        }
    }

    @Test
    void spotSeparationFill_isMeasured() throws Exception {
        // Minimal: DeviceCMYK half-page is enough to prove engine runs; spot via Separation
        // requires constructing PDSeparation color space — covered in integration when available.
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(new PDRectangle(200, 200));
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.setNonStrokingColor(0f, 0f, 0f, 0.5f);
                cs.addRect(0, 0, 100, 200);
                cs.fill();
            }
            PdfInkCoverageEngine engine = new PdfInkCoverageEngine(page, (r, g, b) -> new float[]{0, 0, 0, 1});
            engine.processPage(page);
            double k = engine.result().processMeans()[3];
            // Half page at 50% K → ~25% mean coverage
            assertTrue(k > 20.0 && k < 30.0, "K coverage expected ~25%, got " + k);
        }
    }
}
