package com.inkcore.infrastructure.out.colorconversion.lcms;

import com.inkcore.domain.colorconversion.model.RenderingIntent;
import com.inkcore.infrastructure.config.ColorConversionProperties;
import com.inkcore.infrastructure.out.cache.InMemoryIccProfileCacheAdapter;
import com.inkcore.infrastructure.out.colorconversion.IccProfileLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class LittleCmsColorConverterTest {

    private LittleCmsColorConverter converter;
    private IccProfileLoader loader;

    @BeforeEach
    void setUp() {
        ColorConversionProperties properties = new ColorConversionProperties();
        converter = new LittleCmsColorConverter(properties);
        assumeTrue(converter.isNativeAvailable(), "lcms2 nativo requerido");
        loader = new IccProfileLoader(new InMemoryIccProfileCacheAdapter(properties));
    }

    @Test
    void rgbToCmyk_andSoftProof_roundTripProducesImage() {
        byte[] srgb = loader.loadProfileBytes("sRGB.icc");
        byte[] fogra = loader.loadProfileBytes("FOGRA39.icc");
        BufferedImage rgb = solid(32, 32, new Color(200, 40, 40));
        BufferedImage cmyk = converter.rgbToCmykImage(rgb, srgb, fogra, RenderingIntent.PERCEPTUAL, true);
        BufferedImage proof = converter.cmykToRgbImage(cmyk, fogra, srgb, RenderingIntent.PERCEPTUAL, true);
        assertTrue(cmyk.getWidth() == 32 && cmyk.getRaster().getNumBands() >= 4);
        assertTrue(proof.getType() == BufferedImage.TYPE_INT_RGB);
        int[] px = new int[4];
        cmyk.getRaster().getPixel(8, 8, px);
        assertTrue(px[1] + px[2] > 40, "rojo debe separar M/Y");
    }

    @Test
    void concurrentTransforms_doNotLeakOrCrash() throws Exception {
        byte[] srgb = loader.loadProfileBytes("sRGB.icc");
        byte[] fogra = loader.loadProfileBytes("FOGRA39.icc");
        BufferedImage rgb = solid(16, 16, new Color(30, 120, 200));
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Callable<BufferedImage>> tasks = new ArrayList<>();
            for (int i = 0; i < 32; i++) {
                tasks.add(() -> converter.rgbToCmykImage(
                        rgb, srgb, fogra, RenderingIntent.RELATIVE_COLORIMETRIC, true
                ));
            }
            List<Future<BufferedImage>> futures = pool.invokeAll(tasks);
            for (Future<BufferedImage> f : futures) {
                assertDoesNotThrow(() -> assertTrue(f.get().getWidth() == 16));
            }
        } finally {
            pool.shutdownNow();
        }
    }

    private static BufferedImage solid(int w, int h, Color color) {
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, w, h);
        g.dispose();
        return image;
    }
}
