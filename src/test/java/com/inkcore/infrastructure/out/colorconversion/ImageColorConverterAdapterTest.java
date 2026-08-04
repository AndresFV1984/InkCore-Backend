package com.inkcore.infrastructure.out.colorconversion;

import com.inkcore.domain.colorconversion.model.RenderingIntent;
import com.inkcore.infrastructure.config.ColorConversionProperties;
import com.inkcore.infrastructure.out.cache.InMemoryIccProfileCacheAdapter;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ImageColorConverterAdapterTest {

    @Test
    void applyRenderingIntentHeader_setsPerceptualAndRelative() {
        byte[] perceptual = new byte[128];
        byte[] relative = new byte[128];
        ImageColorConverterAdapter.applyRenderingIntentHeader(perceptual, RenderingIntent.PERCEPTUAL);
        ImageColorConverterAdapter.applyRenderingIntentHeader(relative, RenderingIntent.RELATIVE_COLORIMETRIC);
        assertEquals(0, perceptual[67]);
        assertEquals(1, relative[67]);
    }

    @Test
    void relativeIntent_changesCmykSeparationVsPerceptual() {
        assumeTrue(classpathHas("color-profiles/sRGB.icc"));
        assumeTrue(classpathHas("color-profiles/FOGRA39.icc"));

        ColorConversionProperties properties = new ColorConversionProperties();
        IccProfileLoader loader = new IccProfileLoader(new InMemoryIccProfileCacheAdapter(properties));
        var lcms = ColorConversionTestSupport.littleCms(properties);
        assumeTrue(lcms.isNativeAvailable(), "lcms2 nativo requerido");
        ImageColorConverterAdapter adapter = ColorConversionTestSupport.imageAdapter(loader, properties);

        BufferedImage rgb = solid(64, 64, new Color(220, 40, 40));
        BufferedImage perceptual = adapter.convertRgbToCmykBufferedImage(
                rgb, RenderingIntent.PERCEPTUAL, "sRGB.icc", "FOGRA39.icc"
        );
        BufferedImage relative = adapter.convertRgbToCmykBufferedImage(
                rgb, RenderingIntent.RELATIVE_COLORIMETRIC, "sRGB.icc", "FOGRA39.icc"
        );

        WritableRaster rp = perceptual.getRaster();
        WritableRaster rr = relative.getRaster();
        int[] pp = new int[4];
        int[] pr = new int[4];
        rp.getPixel(10, 10, pp);
        rr.getPixel(10, 10, pr);
        int diff = Math.abs(pp[0] - pr[0]) + Math.abs(pp[1] - pr[1])
                + Math.abs(pp[2] - pr[2]) + Math.abs(pp[3] - pr[3]);
        assertTrue(diff > 0, "RELATIVE debe diferir de PERCEPTUAL tras retarget de tags ICC: "
                + java.util.Arrays.toString(pp) + " vs " + java.util.Arrays.toString(pr));
        assertNotEquals(java.util.Arrays.toString(pp), java.util.Arrays.toString(pr));
    }

    private static BufferedImage solid(int w, int h, Color color) {
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, w, h);
        g.dispose();
        return image;
    }

    private static boolean classpathHas(String resource) {
        return ImageColorConverterAdapterTest.class.getClassLoader().getResource(resource) != null;
    }
}
