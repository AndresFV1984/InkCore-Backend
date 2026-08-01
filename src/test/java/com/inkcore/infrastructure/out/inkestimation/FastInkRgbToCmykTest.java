package com.inkcore.infrastructure.out.inkestimation;

import com.inkcore.infrastructure.out.colorconversion.IccProfileLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;

import java.awt.image.BufferedImage;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FastInkRgbToCmykTest {

    @Mock
    private IccProfileLoader loader;

    @Test
    void toCmyk_isCachedAndFastForManyColors() throws Exception {
        when(loader.loadProfileBytes(anyString())).thenAnswer(inv -> loadClasspath((String) inv.getArgument(0)));

        FastInkRgbToCmyk converter = new FastInkRgbToCmyk(loader, "FOGRA39.icc", 256);
        long start = System.nanoTime();
        for (int i = 0; i < 10_000; i++) {
            float r = (i % 256) / 255f;
            float g = ((i * 3) % 256) / 255f;
            float b = ((i * 7) % 256) / 255f;
            float[] cmyk = converter.toCmyk(r, g, b);
            assertTrue(cmyk[0] >= 0 && cmyk[0] <= 1);
        }
        long ms = (System.nanoTime() - start) / 1_000_000L;
        assertTrue(ms < 5_000, "10k conversiones deberían ser <5s, fueron " + ms + "ms");
    }

    @Test
    void meanCmyk_fromRgbImage_usesBatchConvert() throws Exception {
        when(loader.loadProfileBytes(anyString())).thenAnswer(inv -> loadClasspath((String) inv.getArgument(0)));
        FastInkRgbToCmyk converter = new FastInkRgbToCmyk(loader, "FOGRA39.icc", 128);

        BufferedImage rgb = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 600; y++) {
            for (int x = 0; x < 800; x++) {
                rgb.setRGB(x, y, 0x00A3E0); // cyan-ish
            }
        }
        long start = System.nanoTime();
        double[] mean = converter.meanCmyk01FromRgbImage(rgb);
        long ms = (System.nanoTime() - start) / 1_000_000L;
        assertTrue(mean[0] > mean[1], "C debería dominar");
        assertTrue(ms < 3_000, "batch ICC de 800x600 debería ser <3s, fueron " + ms + "ms");
    }

    private static byte[] loadClasspath(String name) throws Exception {
        String file = name.endsWith(".icc") ? name : name + ".icc";
        // profiles live under color-profiles/
        try (InputStream in = new ClassPathResource("color-profiles/" + file).getInputStream()) {
            return in.readAllBytes();
        } catch (Exception first) {
            try (InputStream in = new ClassPathResource("color-profiles/FOGRA39.icc").getInputStream()) {
                return in.readAllBytes();
            }
        }
    }
}
