package com.inkcore.infrastructure.out.inkestimation;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSFloat;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.function.PDFunctionType2;
import org.apache.pdfbox.pdmodel.graphics.color.PDColorSpace;
import org.apache.pdfbox.pdmodel.graphics.color.PDSeparation;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfSpotColorScannerTest {

    @Test
    void scanPage_findsPantoneSeparationInResources() throws Exception {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            PDSeparation pantone = createSeparation("PANTONE 185 C", new float[]{0f, 1f, 0.8f, 0f});
            PDResources resources = new PDResources();
            resources.add(pantone);
            page.setResources(resources);

            Map<String, String> found = PdfSpotColorScanner.scanPage(page);
            assertTrue(found.containsKey("PANTONE 185 C"), "Inventario debe hallar Pantone: " + found);
        }
    }

    @Test
    void scanPage_ignoresAllSeparation() throws Exception {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            PDSeparation all = createSeparation("All", new float[]{1f, 1f, 1f, 1f});
            PDResources resources = new PDResources();
            resources.add(all);
            page.setResources(resources);

            Map<String, String> found = PdfSpotColorScanner.scanPage(page);
            assertTrue(found.isEmpty(), "All no debe reportarse: " + found);
            assertFalse(SpotColorantNames.isReportableSpotColorant("All"));
        }
    }

    private static PDSeparation createSeparation(String name, float[] cmykAtFull) throws Exception {
        COSDictionary fnDict = new COSDictionary();
        fnDict.setInt(COSName.FUNCTION_TYPE, 2);
        COSArray domainArr = new COSArray();
        domainArr.add(COSInteger.get(0));
        domainArr.add(COSInteger.get(1));
        fnDict.setItem(COSName.DOMAIN, domainArr);
        COSArray c0 = new COSArray();
        for (int i = 0; i < 4; i++) {
            c0.add(new COSFloat(0f));
        }
        COSArray c1 = new COSArray();
        for (float v : cmykAtFull) {
            c1.add(new COSFloat(v));
        }
        fnDict.setItem(COSName.C0, c0);
        fnDict.setItem(COSName.C1, c1);
        fnDict.setInt(COSName.N, 1);
        PDFunctionType2 tint = new PDFunctionType2(fnDict);

        COSArray sepArray = new COSArray();
        sepArray.add(COSName.SEPARATION);
        sepArray.add(COSName.getPDFName(name));
        sepArray.add(COSName.DEVICECMYK);
        sepArray.add(tint);
        return (PDSeparation) PDColorSpace.create(sepArray);
    }
}
