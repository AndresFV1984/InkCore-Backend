package com.inkcore.application.colorconversion;

import com.inkcore.domain.colorconversion.model.IccProfileInfo;
import com.inkcore.infrastructure.config.ColorConversionProperties;
import com.inkcore.infrastructure.out.cache.InMemoryIccProfileCacheAdapter;
import com.inkcore.infrastructure.out.colorconversion.IccProfileLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ListIccProfilesServiceTest {

    private ListIccProfilesService service;

    @BeforeEach
    void setUp() {
        ColorConversionProperties properties = new ColorConversionProperties();
        IccProfileLoader loader = new IccProfileLoader(new InMemoryIccProfileCacheAdapter(properties));
        service = new ListIccProfilesService(loader);
    }

    @Test
    void listsCatalog_andMarksInstalledProfilesAvailable() {
        assumeTrue(getClass().getClassLoader().getResource("color-profiles/FOGRA39.icc") != null);
        List<IccProfileInfo> profiles = service.listDestinationProfiles();
        assertTrue(profiles.size() >= 7);

        assertTrue(available(profiles, "FOGRA39.icc"));
        assertTrue(available(profiles, "FOGRA51.icc"));
        assertTrue(available(profiles, "FOGRA52.icc"));
        assertTrue(available(profiles, "GRACoL2013.icc"));
        assertTrue(available(profiles, "SWOP2006_Coated3v2.icc"));
        assertTrue(available(profiles, "JapanColor2011Coated.icc"));
        // FOGRA47 clasico no se redistribuye en el registry usado
        assertFalse(available(profiles, "FOGRA47.icc"));
    }

    private static boolean available(List<IccProfileInfo> profiles, String fileName) {
        return profiles.stream()
                .filter(p -> p.fileName().equals(fileName))
                .findFirst()
                .map(IccProfileInfo::available)
                .orElse(false);
    }
}
