package com.inkcore.domain.colorconversion.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversionRequestQualityParamsTest {

    @Test
    void parse_blank_returnsNull() {
        assertNull(ConversionRequest.parseBrightnessLift(null));
        assertNull(ConversionRequest.parseBrightnessLift("  "));
        assertNull(ConversionRequest.parseVibranceBoost(""));
        assertNull(ConversionRequest.parseSoftProofBrightnessMatch(null));
    }

    @Test
    void parse_validRanges() {
        assertEquals(0.05f, ConversionRequest.parseBrightnessLift("0.05"), 0.0001f);
        assertEquals(0.15f, ConversionRequest.parseBrightnessLift("0,15"), 0.0001f);
        assertEquals(0.18f, ConversionRequest.parseVibranceBoost("0.18"), 0.0001f);
        assertTrue(ConversionRequest.parseSoftProofBrightnessMatch("true"));
        assertFalse(ConversionRequest.parseSoftProofBrightnessMatch("false"));
        assertTrue(ConversionRequest.parseSoftProofBrightnessMatch("sí"));
    }

    @Test
    void parse_outOfRange_throws() {
        assertThrows(IllegalArgumentException.class, () -> ConversionRequest.parseBrightnessLift("0.25"));
        assertThrows(IllegalArgumentException.class, () -> ConversionRequest.parseBrightnessLift("-0.01"));
        assertThrows(IllegalArgumentException.class, () -> ConversionRequest.parseVibranceBoost("0.5"));
        assertThrows(IllegalArgumentException.class, () -> ConversionRequest.parseSoftProofBrightnessMatch("maybe"));
    }

    @Test
    void resolve_usesOverrideOrServerDefault() {
        ConversionRequest withOverride = new ConversionRequest(
                new byte[]{1}, "a.png", "image/png", RenderingIntent.PERCEPTUAL, null, "u",
                OutputFormat.TIFF, 0.04f, 0.12f, true
        );
        assertEquals(0.04f, withOverride.resolveBrightnessLift(0f), 0.0001f);
        assertEquals(0.12f, withOverride.resolveVibranceBoost(0f), 0.0001f);
        assertTrue(withOverride.resolveSoftProofBrightnessMatch(false));

        ConversionRequest defaults = new ConversionRequest(
                new byte[]{1}, "a.png", "image/png", RenderingIntent.PERCEPTUAL, null, "u"
        );
        assertEquals(0f, defaults.resolveBrightnessLift(0f), 0.0001f);
        assertEquals(0.1f, defaults.resolveVibranceBoost(0.1f), 0.0001f);
        assertFalse(defaults.resolveSoftProofBrightnessMatch(false));
    }

    @Test
    void qualityPreset_appliesWhenOverridesAbsent() {
        ConversionRequest commercial = new ConversionRequest(
                new byte[]{1}, "a.png", "image/png", RenderingIntent.PERCEPTUAL, null, "u",
                OutputFormat.TIFF, null, null, null, QualityPreset.COMMERCIAL
        );
        assertEquals(0.12f, commercial.resolveBrightnessLift(0f), 0.0001f);
        assertEquals(0.28f, commercial.resolveVibranceBoost(0f), 0.0001f);
        assertTrue(commercial.resolveSoftProofBrightnessMatch(false));

        ConversionRequest vivid = new ConversionRequest(
                new byte[]{1}, "a.png", "image/png", RenderingIntent.PERCEPTUAL, null, "u",
                OutputFormat.TIFF, null, null, null, QualityPreset.VIVID
        );
        assertEquals(0.16f, vivid.resolveBrightnessLift(0f), 0.0001f);
        assertEquals(0.35f, vivid.resolveVibranceBoost(0f), 0.0001f);
        assertTrue(vivid.resolveSoftProofBrightnessMatch(false));

        ConversionRequest overrideWins = new ConversionRequest(
                new byte[]{1}, "a.png", "image/png", RenderingIntent.PERCEPTUAL, null, "u",
                OutputFormat.TIFF, 0.01f, 0.05f, false, QualityPreset.VIVID
        );
        assertEquals(0.01f, overrideWins.resolveBrightnessLift(0f), 0.0001f);
        assertEquals(0.05f, overrideWins.resolveVibranceBoost(0f), 0.0001f);
        assertFalse(overrideWins.resolveSoftProofBrightnessMatch(true));
    }

    @Test
    void qualityPreset_fromParam() {
        assertEquals(QualityPreset.COMMERCIAL, QualityPreset.fromParam("comercial"));
        assertEquals(QualityPreset.FIDELITY, QualityPreset.fromParam("CTP"));
        assertNull(QualityPreset.fromParam(null));
        assertThrows(IllegalArgumentException.class, () -> QualityPreset.fromParam("weird"));
    }
}
