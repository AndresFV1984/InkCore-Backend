package com.inkcore.infrastructure.out.colorconversion;

import com.inkcore.domain.colorconversion.exception.IccProfileNotFoundException;
import com.inkcore.infrastructure.config.ColorConversionProperties;
import com.inkcore.infrastructure.out.cache.InMemoryIccProfileCacheAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.color.ICC_Profile;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IccProfileLoaderTest {

    private IccProfileLoader loader;

    @BeforeEach
    void setUp() {
        loader = new IccProfileLoader(new InMemoryIccProfileCacheAdapter(new ColorConversionProperties()));
    }

    @Test
    void fogra39_and_srgb_areValidIccBinariesLoadableByJava() {
        byte[] fogra = loader.loadProfileBytes("FOGRA39.icc");
        byte[] srgb = loader.loadProfileBytes("sRGB.icc");

        assertEquals('a', fogra[36]);
        assertEquals('c', fogra[37]);
        assertEquals('s', fogra[38]);
        assertEquals('p', fogra[39]);
        assertDoesNotThrow(() -> ICC_Profile.getInstance(fogra));
        assertDoesNotThrow(() -> ICC_Profile.getInstance(srgb));
    }

    @Test
    void assertValidIccBinary_rejectsUtf8CorruptedProfile() {
        // Simula filtrado UTF-8: byte alto → EF BF BD (replacement)
        byte[] corrupted = new byte[40];
        corrupted[36] = (byte) 0xEF;
        corrupted[37] = (byte) 0xBF;
        corrupted[38] = (byte) 0xBD;
        corrupted[39] = 'p';

        IccProfileNotFoundException ex = assertThrows(
                IccProfileNotFoundException.class,
                () -> IccProfileLoader.assertValidIccBinary("FOGRA39.icc", corrupted)
        );
        assertTrue(ex.getMessage().contains("corrupto"));
    }
}
