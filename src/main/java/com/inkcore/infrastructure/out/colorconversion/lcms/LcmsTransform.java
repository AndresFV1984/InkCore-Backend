package com.inkcore.infrastructure.out.colorconversion.lcms;

import com.sun.jna.Pointer;

/**
 * Transformación lcms2 acotada (AutoCloseable). No compartir entre hilos.
 */
final class LcmsTransform implements AutoCloseable {

    private final Lcms2Library lib;
    private final Pointer inputProfile;
    private final Pointer outputProfile;
    private final Pointer transform;
    private boolean closed;

    LcmsTransform(
            Lcms2Library lib,
            byte[] inputIcc,
            byte[] outputIcc,
            int inputFormat,
            int outputFormat,
            int intent,
            int flags
    ) {
        this.lib = lib;
        this.inputProfile = open(lib, inputIcc, "entrada");
        this.outputProfile = open(lib, outputIcc, "salida");
        this.transform = lib.cmsCreateTransform(
                inputProfile, inputFormat, outputProfile, outputFormat, intent, flags
        );
        if (transform == null || Pointer.nativeValue(transform) == 0L) {
            closeQuietly();
            throw new LcmsProfileException(
                    "LittleCMS no pudo crear la transformación ICC (intent=" + intent + ", flags=" + flags + ")"
            );
        }
    }

    void transform(byte[] input, byte[] output, int pixelCount) {
        ensureOpen();
        if (pixelCount <= 0) {
            return;
        }
        lib.cmsDoTransform(transform, input, output, pixelCount);
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        if (transform != null && Pointer.nativeValue(transform) != 0L) {
            lib.cmsDeleteTransform(transform);
        }
        if (inputProfile != null && Pointer.nativeValue(inputProfile) != 0L) {
            lib.cmsCloseProfile(inputProfile);
        }
        if (outputProfile != null && Pointer.nativeValue(outputProfile) != 0L) {
            lib.cmsCloseProfile(outputProfile);
        }
    }

    private void closeQuietly() {
        try {
            close();
        } catch (Exception ignored) {
            // cleanup best-effort
        }
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Transformación LittleCMS ya cerrada");
        }
    }

    private static Pointer open(Lcms2Library lib, byte[] icc, String label) {
        if (icc == null || icc.length == 0) {
            throw new LcmsProfileException("Perfil ICC de " + label + " vacío");
        }
        Pointer profile = lib.cmsOpenProfileFromMem(icc, icc.length);
        if (profile == null || Pointer.nativeValue(profile) == 0L) {
            throw new LcmsProfileException(
                    "Perfil ICC de " + label + " inválido o corrupto (" + icc.length + " bytes)"
            );
        }
        return profile;
    }
}
