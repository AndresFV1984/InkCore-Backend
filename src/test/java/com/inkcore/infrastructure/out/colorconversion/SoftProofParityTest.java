package com.inkcore.infrastructure.out.colorconversion;

import com.inkcore.domain.colorconversion.model.ConversionRequest;
import com.inkcore.domain.colorconversion.model.CmykTiffConversion;
import com.inkcore.domain.colorconversion.model.QualityPreset;
import com.inkcore.domain.colorconversion.model.RenderingIntent;
import com.inkcore.infrastructure.config.ColorConversionProperties;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class SoftProofParityTest {

    @Test
    void commercialSoftProof_preservesSize_andKeepsLumaNearOriginal() throws Exception {
        assumeTrue(classpathHas("color-profiles/sRGB.icc"));
        assumeTrue(classpathHas("color-profiles/FOGRA39.icc"));

        ColorConversionProperties properties = new ColorConversionProperties();
        properties.setBrightnessLift(0.12f);
        properties.setVibranceBoost(0.28f);
        properties.setSoftProofBrightnessMatch(true);
        properties.setBlackPointCompensation(true);
        var lcms = ColorConversionTestSupport.littleCms(properties);
        assumeTrue(lcms.isNativeAvailable(), "lcms2 nativo requerido");

        ImageColorConverterAdapter adapter = ColorConversionTestSupport.imageAdapter(properties);
        BufferedImage rgb = photoLike(320, 240);
        byte[] png = toPng(rgb);

        ConversionRequest request = new ConversionRequest(
                png,
                "foto.png",
                "image/png",
                RenderingIntent.PERCEPTUAL,
                "FOGRA39.icc",
                null,
                null,
                0.12f,
                0.28f,
                true,
                QualityPreset.COMMERCIAL,
                true
        );

        CmykTiffConversion conversion = adapter.convertToCmykTiff(request, "sRGB.icc", "FOGRA39.icc");
        assertNotNull(conversion.tiffBytes());
        assertNotNull(conversion.previewJpeg());
        assertNotNull(conversion.softProofLumaRatio(), "debe medir softProofLumaRatio con soft-proof ON");
        assertTrue(
                conversion.softProofLumaRatio() >= 0.90,
                "soft-proof debe acercarse al target lift/vibrance (ratio=" + conversion.softProofLumaRatio() + ")"
        );

        var info = adapter.readTiffInfo(conversion.tiffBytes());
        assertEquals(320, info.getWidthPx());
        assertEquals(240, info.getHeightPx());
    }

    private static BufferedImage photoLike(int w, int h) {
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(new Color(240, 235, 220));
            g.fillRect(0, 0, w, h);
            g.setColor(new Color(200, 60, 40));
            g.fillOval(w / 6, h / 5, w / 3, h / 3);
            g.setColor(new Color(40, 120, 70));
            g.fillOval(w / 2, h / 3, w / 3, h / 3);
            g.setColor(new Color(30, 80, 170));
            g.fillRect(w / 8, (2 * h) / 3, w / 2, h / 5);
        } finally {
            g.dispose();
        }
        return image;
    }

    private static byte[] toPng(BufferedImage image) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }

    private static boolean classpathHas(String resource) {
        return SoftProofParityTest.class.getClassLoader().getResource(resource) != null;
    }
}
