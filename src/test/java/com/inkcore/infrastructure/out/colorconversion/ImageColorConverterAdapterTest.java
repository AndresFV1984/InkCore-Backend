package com.inkcore.infrastructure.out.colorconversion;

import com.inkcore.domain.colorconversion.model.RenderingIntent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
