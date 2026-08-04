package com.inkcore.infrastructure.out.inkestimation;

import com.inkcore.domain.inkestimation.exception.InkEstimationFailedException;
import com.inkcore.infrastructure.config.ColorConversionProperties;
import com.inkcore.infrastructure.out.colorconversion.IccProfileLoader;
import com.inkcore.infrastructure.out.colorconversion.lcms.LittleCmsColorConverter;
import com.inkcore.infrastructure.out.colorconversion.lcms.LcmsNativeUnavailableException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;

import java.awt.image.BufferedImage;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FastInkRgbToCmykTest {

    @Mock
    private IccProfileLoader loader;

    @Test
    void toCmyk_isCachedAndFastForManyColors() throws Exception {
        LittleCmsColorConverter lcms = new LittleCmsColorConverter(new ColorConversionProperties());
        assumeTrue(lcms.isNativeAvailable(), "lcms2 nativo requerido");
        when(loader.loadProfileBytes(anyString())).thenAnswer(inv -> loadClasspath((String) inv.getArgument(0)));

        try (FastInkRgbToCmyk converter = new FastInkRgbToCmyk(loader, lcms, "FOGRA39.icc", 256, true)) {
            for (int i = 0; i < 256; i++) {
                converter.toCmyk(i / 255f, 0.25f, 0.40f);
            }
            long start = System.nanoTime();
            for (int i = 0; i < 10_000; i++) {
                float[] cmyk = converter.toCmyk((i % 256) / 255f, 0.25f, 0.40f);
                assertTrue(cmyk[0] >= 0 && cmyk[0] <= 1);
            }
            long ms = (System.nanoTime() - start) / 1_000_000L;
            assertTrue(ms < 2_000, "10k hits de caché deberían ser <2s, fueron " + ms + "ms");
            assertEquals(LittleCmsColorConverter.ENGINE_LITTLECMS, converter.colorEngine());
        }
    }

    @Test
    void meanCmyk_fromRgbImage_usesBatchConvert() throws Exception {
        LittleCmsColorConverter lcms = new LittleCmsColorConverter(new ColorConversionProperties());
        assumeTrue(lcms.isNativeAvailable(), "lcms2 nativo requerido");
        when(loader.loadProfileBytes(anyString())).thenAnswer(inv -> loadClasspath((String) inv.getArgument(0)));

        try (FastInkRgbToCmyk converter = new FastInkRgbToCmyk(loader, lcms, "FOGRA39.icc", 128, true)) {
            BufferedImage rgb = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < 600; y++) {
                for (int x = 0; x < 800; x++) {
                    rgb.setRGB(x, y, 0x00A3E0);
                }
            }
            long start = System.nanoTime();
            double[] mean = converter.meanCmyk01FromRgbImage(rgb);
            long ms = (System.nanoTime() - start) / 1_000_000L;
            assertTrue(mean[0] > mean[1], "C debería dominar");
            assertTrue(ms < 2_000, "muestreo+batch ICC debería ser <2s, fueron " + ms + "ms");
        }
    }

    @Test
    void manyUniqueColors_reuseSession_isMuchFasterThanPerCallTransform() throws Exception {
        LittleCmsColorConverter lcms = new LittleCmsColorConverter(new ColorConversionProperties());
        assumeTrue(lcms.isNativeAvailable(), "lcms2 nativo requerido");
        when(loader.loadProfileBytes(anyString())).thenAnswer(inv -> loadClasspath((String) inv.getArgument(0)));

        try (FastInkRgbToCmyk converter = new FastInkRgbToCmyk(loader, lcms, "FOGRA39.icc", 64, true)) {
            long start = System.nanoTime();
            for (int i = 0; i < 2_000; i++) {
                float r = (i % 256) / 255f;
                float g = ((i * 3) % 256) / 255f;
                float b = ((i * 7) % 256) / 255f;
                float[] cmyk = converter.toCmyk(r, g, b);
                assertTrue(cmyk[3] >= 0f && cmyk[3] <= 1f);
            }
            long ms = (System.nanoTime() - start) / 1_000_000L;
            assertTrue(ms < 5_000, "2k colores únicos con sesión deberían ser <5s, fueron " + ms + "ms");
        }
    }

    @Test
    void whenProfilesUnavailable_failsHard() {
        LittleCmsColorConverter lcms = new LittleCmsColorConverter(new ColorConversionProperties());
        assumeTrue(lcms.isNativeAvailable(), "lcms2 nativo requerido");
        when(loader.loadProfileBytes(anyString())).thenThrow(new RuntimeException("no profile"));
        assertThrows(
                InkEstimationFailedException.class,
                () -> new FastInkRgbToCmyk(loader, lcms, "FOGRA39.icc", 64, true)
        );
    }

    @Test
    void whenNativeUnavailable_failsHard() {
        LittleCmsColorConverter unavailable = org.mockito.Mockito.mock(LittleCmsColorConverter.class);
        when(unavailable.isNativeAvailable()).thenReturn(false);
        assertThrows(
                LcmsNativeUnavailableException.class,
                () -> new FastInkRgbToCmyk(loader, unavailable, "FOGRA39.icc", 64, true)
        );
    }

    private static byte[] loadClasspath(String name) throws Exception {
        String file = name.endsWith(".icc") ? name : name + ".icc";
        try (InputStream in = new ClassPathResource("color-profiles/" + file).getInputStream()) {
            return in.readAllBytes();
        } catch (Exception first) {
            try (InputStream in = new ClassPathResource("color-profiles/FOGRA39.icc").getInputStream()) {
                return in.readAllBytes();
            }
        }
    }
}
