package com.inkcore.infrastructure.out.inkestimation;

import org.junit.jupiter.api.Test;

import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InkCoverageAnalyzerAdapterTest {

    @Test
    void meanChannelCoverage_fullCyan_isNear100() {
        BufferedImage cmyk = solidCmyk(16, 16, 255, 0, 0, 0);
        double[] means = InkCoverageAnalyzerAdapter.meanChannelCoveragePercent(cmyk);
        assertEquals(100.0, means[0], 0.01);
        assertEquals(0.0, means[1], 0.01);
        assertEquals(0.0, means[2], 0.01);
        assertEquals(0.0, means[3], 0.01);
    }

    @Test
    void meanChannelCoverage_halfInk_isNear50() {
        BufferedImage cmyk = solidCmyk(8, 8, 128, 128, 128, 128);
        double[] means = InkCoverageAnalyzerAdapter.meanChannelCoveragePercent(cmyk);
        assertTrue(means[0] > 49 && means[0] < 51);
    }

    private static BufferedImage solidCmyk(int w, int h, int c, int m, int y, int k) {
        // Device CMYK-like 4-band byte image (TYPE via ComponentColorModel)
        ColorSpace cs = new ColorSpace(ColorSpace.TYPE_CMYK, 4) {
            @Override
            public float[] toRGB(float[] colorvalue) {
                return new float[]{1f, 1f, 1f};
            }

            @Override
            public float[] fromRGB(float[] rgbvalue) {
                return new float[]{0, 0, 0, 0};
            }

            @Override
            public float[] toCIEXYZ(float[] colorvalue) {
                return new float[]{0, 0, 0};
            }

            @Override
            public float[] fromCIEXYZ(float[] colorvalue) {
                return new float[]{0, 0, 0, 0};
            }
        };
        int[] bits = {8, 8, 8, 8};
        ComponentColorModel model = new ComponentColorModel(
                cs, bits, false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE
        );
        WritableRaster raster = model.createCompatibleWritableRaster(w, h);
        BufferedImage image = new BufferedImage(model, raster, false, null);
        int[] px = {c, m, y, k};
        for (int yy = 0; yy < h; yy++) {
            for (int xx = 0; xx < w; xx++) {
                raster.setPixel(xx, yy, px);
            }
        }
        return image;
    }
}
