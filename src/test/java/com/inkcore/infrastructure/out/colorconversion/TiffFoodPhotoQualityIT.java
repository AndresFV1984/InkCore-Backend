package com.inkcore.infrastructure.out.colorconversion;

import com.inkcore.domain.colorconversion.model.ConversionRequest;
import com.inkcore.domain.colorconversion.model.CmykTiffConversion;
import com.inkcore.domain.colorconversion.model.QualityPreset;
import com.inkcore.domain.colorconversion.model.RenderingIntent;
import com.inkcore.infrastructure.config.ColorConversionProperties;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class TiffFoodPhotoQualityIT {

    @Test
    void vividTiff_softProofNearTarget_andIccEmbedded() throws Exception {
        Path rgbPath = Path.of("target/compare-imgs2/rgb.png");
        assumeTrue(Files.exists(rgbPath), "rgb fixture missing");
        assumeTrue(classpathHas("color-profiles/sRGB.icc"));
        assumeTrue(classpathHas("color-profiles/FOGRA39.icc"));

        byte[] png = Files.readAllBytes(rgbPath);
        ColorConversionProperties properties = new ColorConversionProperties();
        properties.setBrightnessLift(0.16f);
        properties.setVibranceBoost(0.35f);
        properties.setSoftProofBrightnessMatch(true);
        properties.setBlackPointCompensation(true);
        var lcms = ColorConversionTestSupport.littleCms(properties);
        assumeTrue(lcms.isNativeAvailable(), "lcms2 required");

        ImageColorConverterAdapter adapter = ColorConversionTestSupport.imageAdapter(properties);
        ConversionRequest request = new ConversionRequest(
                png, "foto.png", "image/png", RenderingIntent.PERCEPTUAL, "FOGRA39.icc", null, null,
                0.16f, 0.35f, true, QualityPreset.VIVID, true
        );
        CmykTiffConversion conversion = adapter.convertToCmykTiff(request, "sRGB.icc", "FOGRA39.icc");
        assertNotNull(conversion.tiffBytes());
        assertNotNull(conversion.previewJpeg());
        assertNotNull(conversion.softProofLumaRatio());
        assertTrue(conversion.softProofLumaRatio() >= 0.90, "ratio=" + conversion.softProofLumaRatio());
        assertTrue(hasIccTag(conversion.tiffBytes()), "TIFF must embed ICC 34675 for correct viewing");

        Path out = Path.of("target/compare-imgs2");
        Files.write(out.resolve("out_vivid.tif"), conversion.tiffBytes());
        Files.write(out.resolve("out_vivid_preview.jpg"), conversion.previewJpeg());
        byte[] reproof = adapter.softProofRgbJpeg(conversion.tiffBytes(), 0.95f);
        Files.write(out.resolve("out_vivid_reproof.jpg"), reproof);

        BufferedImage preview = ImageIO.read(new ByteArrayInputStream(conversion.previewJpeg()));
        BufferedImage rgb = ImageIO.read(new ByteArrayInputStream(png));
        double previewChroma = meanChroma(preview);
        double rgbChroma = meanChroma(rgb);
        System.out.println("softProofLumaRatio=" + conversion.softProofLumaRatio());
        System.out.println("rgbChroma=" + rgbChroma + " previewChroma=" + previewChroma);
        // Soft-proof should retain a substantial share of chroma for this food photo
        assertTrue(previewChroma >= rgbChroma * 0.85,
                "preview chroma too low: rgb=" + rgbChroma + " preview=" + previewChroma);
    }

    private static double meanChroma(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        long sum = 0;
        int n = 0;
        for (int y = 0; y < h; y += 4) {
            for (int x = 0; x < w; x += 4) {
                int p = img.getRGB(x, y);
                int r = (p >> 16) & 0xff;
                int g = (p >> 8) & 0xff;
                int b = p & 0xff;
                sum += Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b));
                n++;
            }
        }
        return n == 0 ? 0 : (double) sum / n;
    }

    private static boolean hasIccTag(byte[] tiff) {
        // little-endian tag 34675 = 0x8773 -> bytes 73 87; big-endian 87 73
        for (int i = 0; i < tiff.length - 1; i++) {
            int a = tiff[i] & 0xff;
            int b = tiff[i + 1] & 0xff;
            if ((a == 0x73 && b == 0x87) || (a == 0x87 && b == 0x73)) {
                return true;
            }
        }
        return false;
    }

    private static boolean classpathHas(String resource) {
        return TiffFoodPhotoQualityIT.class.getClassLoader().getResource(resource) != null;
    }
}