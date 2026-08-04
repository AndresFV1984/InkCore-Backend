package com.inkcore.infrastructure.out.colorconversion;

import com.inkcore.domain.colorconversion.model.ConversionRequest;
import com.inkcore.domain.colorconversion.model.OutputFormat;
import com.inkcore.domain.colorconversion.model.RenderingIntent;
import com.inkcore.infrastructure.config.ColorConversionProperties;
import com.inkcore.infrastructure.out.cache.InMemoryIccProfileCacheAdapter;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Calidad RGB→CMYK: sin cuadro negro en PDF, color coherente, mismas dimensiones.
 */
class ColorConversionQualityIT {

    private ImageColorConverterAdapter imageConverter;
    private PdfColorConverterAdapter pdfConverter;
    private IccProfileLoader iccLoader;
    private com.inkcore.infrastructure.out.colorconversion.lcms.LittleCmsColorConverter littleCms;
    private ColorConversionProperties properties;

    @BeforeEach
    void setUp() {
        assumeTrue(classpathHas("color-profiles/sRGB.icc"));
        assumeTrue(classpathHas("color-profiles/FOGRA39.icc"));
        properties = new ColorConversionProperties();
        properties.setPdfEnabled(true);
        properties.setMinImageResolutionDpi(72);
        iccLoader = new IccProfileLoader(new InMemoryIccProfileCacheAdapter(properties));
        littleCms = ColorConversionTestSupport.littleCms(properties);
        assumeTrue(littleCms.isNativeAvailable(), "Requiere lcms2 nativo instalado");
        imageConverter = ColorConversionTestSupport.imageAdapter(iccLoader, properties);
        pdfConverter = new PdfColorConverterAdapter(properties, imageConverter);
    }

    @Test
    void cmykWhite_hasLowInk_andPdfIsNotBlack() throws Exception {
        BufferedImage rgb = solidRgb(64, 64, Color.WHITE);
        BufferedImage cmyk = imageConverter.convertRgbToCmykBufferedImage(
                rgb, RenderingIntent.PERCEPTUAL, "sRGB.icc", "FOGRA39.icc"
        );
        int[] whitePx = sample(cmyk, 10, 10);
        int inkSum = whitePx[0] + whitePx[1] + whitePx[2] + whitePx[3];
        assertTrue(inkSum < 80, "Blanco CMYK no debería tener mucha tinta: " + java.util.Arrays.toString(whitePx));

        byte[] png = toPng(rgb);
        ConversionRequest req = new ConversionRequest(
                png, "white.png", "image/png", RenderingIntent.PERCEPTUAL, "FOGRA39.icc", "u", OutputFormat.PDF
        );
        byte[] pdf = pdfConverter.convertRasterImageToCmykPdf(req, "sRGB.icc", "FOGRA39.icc").pdfBytes();

        try (PDDocument doc = Loader.loadPDF(new RandomAccessReadBuffer(pdf))) {
            BufferedImage rendered = new PDFRenderer(doc).renderImageWithDPI(0, 72, ImageType.RGB);
            int rgbSample = rendered.getRGB(rendered.getWidth() / 2, rendered.getHeight() / 2);
            int r = (rgbSample >> 16) & 0xff;
            int g = (rgbSample >> 8) & 0xff;
            int b = rgbSample & 0xff;
            assertTrue(r + g + b > 500, "PDF blanco no debe verse negro: rgb=" + r + "," + g + "," + b);
        }
    }

    @Test
    void cmykRed_isNotGrayBlack() throws Exception {
        BufferedImage rgb = solidRgb(32, 32, Color.RED);
        BufferedImage cmyk = imageConverter.convertRgbToCmykBufferedImage(
                rgb, RenderingIntent.PERCEPTUAL, "sRGB.icc", "FOGRA39.icc"
        );
        int[] px = sample(cmyk, 5, 5);
        assertTrue(px[1] > 40 || px[2] > 40, "Rojo debería tener Magenta/Yellow: " + java.util.Arrays.toString(px));
    }

    @Test
    void photoLikePdf_rendersRedNotBlack() throws Exception {
        BufferedImage rgb = solidRgb(128, 128, new Color(220, 40, 40));
        byte[] png = toPng(rgb);
        ConversionRequest req = new ConversionRequest(
                png, "red.png", "image/png", RenderingIntent.PERCEPTUAL, "FOGRA39.icc", "u", OutputFormat.PDF
        );
        byte[] pdf = pdfConverter.convertRasterImageToCmykPdf(req, "sRGB.icc", "FOGRA39.icc").pdfBytes();

        try (PDDocument doc = Loader.loadPDF(new RandomAccessReadBuffer(pdf))) {
            BufferedImage rendered = new PDFRenderer(doc).renderImageWithDPI(0, 72, ImageType.RGB);
            int rgbSample = rendered.getRGB(rendered.getWidth() / 2, rendered.getHeight() / 2);
            int r = (rgbSample >> 16) & 0xff;
            int g = (rgbSample >> 8) & 0xff;
            int b = rgbSample & 0xff;
            assertTrue(r > 120, "Rojo debe verse rojo en PDF, no negro: rgb=" + r + "," + g + "," + b);
            assertTrue(r > g + 40 && r > b + 40, "Canal R dominante: rgb=" + r + "," + g + "," + b);
            assertTrue(r + g + b > 120, "No debe ser cuadro negro");
        }
    }

    @Test
    void defaults_areCommercialQuality_forScreenAppearance() {
        ColorConversionProperties defaults = new ColorConversionProperties();
        assertEquals(0.12f, defaults.getBrightnessLift(), 0.0001f);
        assertEquals(0.28f, defaults.getVibranceBoost(), 0.0001f);
        assertTrue(defaults.isSoftProofBrightnessMatch());

        BufferedImage rgb = solidRgb(24, 24, new Color(200, 60, 40));
        BufferedImage cmyk = imageConverter.convertRgbToCmykBufferedImage(
                rgb, RenderingIntent.PERCEPTUAL, "sRGB.icc", "FOGRA39.icc"
        );
        int[] px = sample(cmyk, 5, 5);
        assertTrue(px[1] + px[2] > 40, "Rojo debe separar M/Y: " + java.util.Arrays.toString(px));
    }

    @Test
    void pdf_embedsOutputIntent_andRendersWithoutBlackBox() throws Exception {
        BufferedImage rgb = solidRgb(64, 64, new Color(220, 40, 40));
        byte[] png = toPng(rgb);
        ConversionRequest req = new ConversionRequest(
                png, "red.png", "image/png", RenderingIntent.PERCEPTUAL, "FOGRA39.icc", "u", OutputFormat.PDF
        );
        byte[] pdf = pdfConverter.convertRasterImageToCmykPdf(req, "sRGB.icc", "FOGRA39.icc").pdfBytes();

        try (PDDocument doc = Loader.loadPDF(new RandomAccessReadBuffer(pdf))) {
            assertTrue(
                    doc.getDocumentCatalog().getOutputIntents() != null
                            && !doc.getDocumentCatalog().getOutputIntents().isEmpty(),
                    "PDF debe incluir OutputIntent con el ICC de destino"
            );
            BufferedImage rendered = new PDFRenderer(doc).renderImageWithDPI(0, 72, ImageType.RGB);
            int rgbSample = rendered.getRGB(rendered.getWidth() / 2, rendered.getHeight() / 2);
            int r = (rgbSample >> 16) & 0xff;
            int g = (rgbSample >> 8) & 0xff;
            int b = rgbSample & 0xff;
            assertTrue(r > 100, "No cuadro negro: rgb=" + r + "," + g + "," + b);
            assertTrue(r > g + 30 && r > b + 30, "Rojo dominante: rgb=" + r + "," + g + "," + b);
        }
    }

    @Test
    void brightnessLift_keepsMidGrayFromGoingTooDark() {
        BufferedImage mid = solidRgb(32, 32, new Color(160, 160, 160));
        BufferedImage lifted = ImageColorConverterAdapter.liftRgbTowardWhite(mid, 0.08f);
        int before = mid.getRGB(10, 10);
        int after = lifted.getRGB(10, 10);
        int y0 = ((before >> 16) & 0xff);
        int y1 = ((after >> 16) & 0xff);
        assertTrue(y1 > y0, "El lift debe subir el brillo del gris medio");
        assertTrue(y1 < 255, "No debe saturar a blanco puro");
    }

    @Test
    void brightnessLift_protectsDeepBlacks() {
        BufferedImage black = solidRgb(16, 16, new Color(0, 0, 0));
        BufferedImage lifted = ImageColorConverterAdapter.liftRgbTowardWhite(black, 0.16f);
        int p = lifted.getRGB(8, 8);
        int r = (p >> 16) & 0xff;
        int g = (p >> 8) & 0xff;
        int b = p & 0xff;
        assertTrue(r <= 2 && g <= 2 && b <= 2, "Negros no deben lavarse: rgb=" + r + "," + g + "," + b);
    }

    @Test
    void vibranceBoost_increasesFlowerLikeSaturation() {
        BufferedImage orange = solidRgb(16, 16, new Color(240, 90, 20));
        BufferedImage boosted = ImageColorConverterAdapter.boostVibranceHsb(orange, 0.18f);
        int o = orange.getRGB(8, 8);
        int b = boosted.getRGB(8, 8);
        int oMax = Math.max((o >> 16) & 0xff, Math.max((o >> 8) & 0xff, o & 0xff));
        int oMin = Math.min((o >> 16) & 0xff, Math.min((o >> 8) & 0xff, o & 0xff));
        int bMax = Math.max((b >> 16) & 0xff, Math.max((b >> 8) & 0xff, b & 0xff));
        int bMin = Math.min((b >> 16) & 0xff, Math.min((b >> 8) & 0xff, b & 0xff));
        double satO = oMax == 0 ? 0 : (oMax - oMin) / (double) oMax;
        double satB = bMax == 0 ? 0 : (bMax - bMin) / (double) bMax;
        assertTrue(satB >= satO - 0.001, "Vibrance no debe bajar saturación");
    }

    @Test
    void pngAndJpeg_preserveDimensions_andColorSeparation() throws Exception {
        BufferedImage rgb = new BufferedImage(96, 64, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.setColor(new Color(220, 40, 40));
        g.fillRect(0, 0, 48, 64);
        g.setColor(new Color(40, 80, 200));
        g.fillRect(48, 0, 48, 64);
        g.dispose();

        for (String format : new String[]{"png", "jpg"}) {
            byte[] bytes = encode(rgb, format);
            String mime = format.equals("png") ? "image/png" : "image/jpeg";
            ConversionRequest tiffReq = new ConversionRequest(
                    bytes, "sample." + format, mime, RenderingIntent.PERCEPTUAL, "FOGRA39.icc", "u", OutputFormat.TIFF
            );
            byte[] tiff = imageConverter.convertToCmykTiff(tiffReq, "sRGB.icc", "FOGRA39.icc").tiffBytes();
            var info = imageConverter.readTiffInfo(tiff);
            assertEquals(96, info.getWidthPx(), format + "→TIFF width");
            assertEquals(64, info.getHeightPx(), format + "→TIFF height");

            ConversionRequest pdfReq = new ConversionRequest(
                    bytes, "sample." + format, mime, RenderingIntent.PERCEPTUAL, "FOGRA39.icc", "u", OutputFormat.PDF
            );
            byte[] pdf = pdfConverter.convertRasterImageToCmykPdf(pdfReq, "sRGB.icc", "FOGRA39.icc").pdfBytes();
            try (PDDocument doc = Loader.loadPDF(new RandomAccessReadBuffer(pdf))) {
                BufferedImage rendered = new PDFRenderer(doc).renderImageWithDPI(0, 72, ImageType.RGB);
                int left = rendered.getRGB(rendered.getWidth() / 4, rendered.getHeight() / 2);
                int right = rendered.getRGB(3 * rendered.getWidth() / 4, rendered.getHeight() / 2);
                int lr = (left >> 16) & 0xff;
                int rr = (right >> 16) & 0xff;
                int rb = right & 0xff;
                assertTrue(lr > 100, format + " PDF izquierda roja: " + Integer.toHexString(left));
                assertTrue(rb > 80 || ((right >> 8) & 0xff) < lr, format + " PDF derecha azulada");
                assertTrue(Math.abs(lr - rr) > 30, format + " lados deben diferenciarse (no imagen plana/negra)");
            }
        }
    }

    @Test
    void pngWithAlpha_flattensWithoutShrinking() throws Exception {
        BufferedImage rgba = new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = rgba.createGraphics();
        g.setColor(new Color(0, 0, 0, 0));
        g.fillRect(0, 0, 40, 40);
        g.setColor(new Color(255, 0, 0, 255));
        g.fillRect(10, 10, 20, 20);
        g.dispose();

        byte[] png = toPng(rgba);
        ConversionRequest req = new ConversionRequest(
                png, "alpha.png", "image/png", RenderingIntent.PERCEPTUAL, "FOGRA39.icc", "u", OutputFormat.TIFF
        );
        byte[] tiff = imageConverter.convertToCmykTiff(req, "sRGB.icc", "FOGRA39.icc").tiffBytes();
        var info = imageConverter.readTiffInfo(tiff);
        assertEquals(40, info.getWidthPx());
        assertEquals(40, info.getHeightPx());
    }

    @Test
    void softProofRecovery_closesBrightnessGapOnVividPhoto() {
        BufferedImage rgb = new BufferedImage(128, 128, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.setColor(new Color(255, 255, 255));
        g.fillRect(0, 0, 128, 128);
        g.setColor(new Color(210, 90, 30));
        g.fillOval(20, 20, 90, 90);
        g.setColor(new Color(120, 50, 20));
        g.fillOval(40, 40, 50, 50);
        g.dispose();

        ConversionRequest fidelity = new ConversionRequest(
                new byte[]{1}, "x.png", "image/png", RenderingIntent.PERCEPTUAL, "FOGRA39.icc", "u",
                OutputFormat.TIFF, 0f, 0f, false
        );
        ConversionRequest commercial = new ConversionRequest(
                new byte[]{1}, "x.png", "image/png", RenderingIntent.PERCEPTUAL, "FOGRA39.icc", "u",
                OutputFormat.TIFF, 0.12f, 0.28f, true
        );

        BufferedImage cmykFidelity = imageConverter.convertRgbToCmykBufferedImage(
                rgb, RenderingIntent.PERCEPTUAL, "sRGB.icc", "FOGRA39.icc", fidelity
        );
        BufferedImage cmykCommercial = imageConverter.convertRgbToCmykBufferedImage(
                rgb, RenderingIntent.PERCEPTUAL, "sRGB.icc", "FOGRA39.icc", commercial
        );

        byte[] srgb = iccLoader.loadProfileBytes("sRGB.icc");
        byte[] fogra = iccLoader.loadProfileBytes("FOGRA39.icc");
        BufferedImage proofFidelity = littleCms.cmykToRgbImage(
                cmykFidelity, fogra, srgb, RenderingIntent.PERCEPTUAL, null
        );
        BufferedImage proofCommercial = littleCms.cmykToRgbImage(
                cmykCommercial, fogra, srgb, RenderingIntent.PERCEPTUAL, null
        );

        double src = meanLuma(rgb);
        double fid = meanLuma(proofFidelity);
        double com = meanLuma(proofCommercial);
        assertTrue(com > fid, "Commercial+softProof debe quedar más brillante que fidelidad: com=" + com + " fid=" + fid);
        // En fondos blancos la media global engaña; exigir mejora clara vs fidelidad
        assertTrue((com - fid) > 3.0 || com >= src * 0.95,
                "Debe recuperar brillo: src=" + src + " fid=" + fid + " com=" + com);
    }

    private static double meanLuma(BufferedImage image) {
        long sum = 0;
        int[] px = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        for (int rgb : px) {
            int r = (rgb >> 16) & 0xff;
            int g = (rgb >> 8) & 0xff;
            int b = rgb & 0xff;
            sum += (int) (0.2126 * r + 0.7152 * g + 0.0722 * b);
        }
        return sum / (double) px.length;
    }

    private static int[] sample(BufferedImage cmyk, int x, int y) {
        WritableRaster raster = cmyk.getRaster();
        int[] px = new int[Math.max(4, raster.getNumBands())];
        raster.getPixel(x, y, px);
        return new int[]{px[0], px[1], px[2], px[3]};
    }

    private static BufferedImage solidRgb(int w, int h, Color color) {
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, w, h);
        g.dispose();
        return image;
    }

    private static byte[] toPng(BufferedImage image) throws Exception {
        return encode(image, "png");
    }

    private static byte[] encode(BufferedImage image, String format) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, format, baos);
        return baos.toByteArray();
    }

    private static boolean classpathHas(String resource) {
        return ColorConversionQualityIT.class.getClassLoader().getResource(resource) != null;
    }
}
