package com.inkcore.infrastructure.out.colorconversion;

import com.inkcore.domain.colorconversion.model.ConversionRequest;
import com.inkcore.domain.colorconversion.model.RasterImageInfo;
import com.inkcore.domain.colorconversion.model.RenderingIntent;
import com.inkcore.domain.colorconversion.ports.out.IccProfileCachePort;
import com.inkcore.infrastructure.config.ColorConversionProperties;
import com.inkcore.infrastructure.out.cache.InMemoryIccProfileCacheAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Garantías de integridad con PNG/TIFF reales y perfiles ICC del classpath.
 */
class ImageColorConverterIntegrityIT {

    private ImageColorConverterAdapter adapter;

    @BeforeEach
    void setUp() {
        assumeTrue(classpathHas("color-profiles/sRGB.icc"));
        assumeTrue(classpathHas("color-profiles/FOGRA39.icc"));
        ColorConversionProperties properties = new ColorConversionProperties();
        IccProfileCachePort cache = new InMemoryIccProfileCacheAdapter(properties);
        IccProfileLoader loader = new IccProfileLoader(cache);
        assumeTrue(ColorConversionTestSupport.littleCms(properties).isNativeAvailable(), "lcms2 nativo requerido");
        adapter = ColorConversionTestSupport.imageAdapter(loader, properties);
    }

    @Test
    void convertPng_preservesDimensionsAndUsesLosslessTiff() throws Exception {
        byte[] png = createSamplePng(64, 48);
        ConversionRequest request = new ConversionRequest(
                png, "sample.png", "image/png", RenderingIntent.PERCEPTUAL, "FOGRA39.icc", "test-user"
        );

        RasterImageInfo input = adapter.readInfo(png, "sample.png");
        byte[] tiff = adapter.convertToCmykTiff(request, "sRGB.icc", "FOGRA39.icc").tiffBytes();
        RasterImageInfo output = adapter.readTiffInfo(tiff);

        assertEquals(input.getWidthPx(), output.getWidthPx());
        assertEquals(input.getHeightPx(), output.getHeightPx());
        assertEquals(8, output.getBitsPerSample());
        assertTrue(
                output.getCompression() == null
                        || !output.getCompression().toUpperCase().contains("JPEG"),
                "Salida no debe usar JPEG"
        );
        assertTrue(tiff.length > 0);
        // ICC embebido en binario (tag 34675) tras la escritura LZW
        assertTrue(tiff.length > 50_000, "TIFF debería incluir perfil ICC embebido");
    }

    private static byte[] createSamplePng(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, width / 2, height);
        g.setColor(Color.BLUE);
        g.fillRect(width / 2, 0, width / 2, height);
        g.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }

    private static boolean classpathHas(String resource) {
        return Thread.currentThread().getContextClassLoader().getResource(resource) != null
                || ImageColorConverterIntegrityIT.class.getClassLoader().getResource(resource) != null;
    }
}
