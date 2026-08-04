package com.inkcore.infrastructure.out.colorconversion.lcms;

import com.sun.jna.Library;
import com.sun.jna.Pointer;

/**
 * Binding JNA mínimo de lcms2 (MIT). Solo las funciones necesarias para RGB↔CMYK.
 *
 * @see <a href="https://www.littlecms.com/">LittleCMS</a>
 */
public interface Lcms2Library extends Library {

    int INTENT_PERCEPTUAL = 0;
    int INTENT_RELATIVE_COLORIMETRIC = 1;
    int INTENT_SATURATION = 2;
    int INTENT_ABSOLUTE_COLORIMETRIC = 3;

    int cmsFLAGS_BLACKPOINTCOMPENSATION = 0x2000;
    int cmsFLAGS_NOOPTIMIZE = 0x0100;
    int cmsFLAGS_NOCACHE = 0x0040;

    /** PT_RGB=4, 3 canales, 1 byte */
    int TYPE_RGB_8 = (4 << 16) | (3 << 3) | 1;
    /** PT_CMYK=6, 4 canales, 1 byte */
    int TYPE_CMYK_8 = (6 << 16) | (4 << 3) | 1;
    int TYPE_RGB_16 = (4 << 16) | (3 << 3) | 2;
    int TYPE_CMYK_16 = (6 << 16) | (4 << 3) | 2;

    Pointer cmsOpenProfileFromMem(byte[] data, int size);

    int cmsCloseProfile(Pointer hProfile);

    Pointer cmsCreateTransform(
            Pointer input,
            int inputFormat,
            Pointer output,
            int outputFormat,
            int intent,
            int flags
    );

    void cmsDeleteTransform(Pointer hTransform);

    void cmsDoTransform(Pointer transform, byte[] inputBuffer, byte[] outputBuffer, int pixelCount);
}
