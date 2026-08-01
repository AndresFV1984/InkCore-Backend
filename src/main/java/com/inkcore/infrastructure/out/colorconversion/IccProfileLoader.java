package com.inkcore.infrastructure.out.colorconversion;

import com.inkcore.domain.colorconversion.exception.IccProfileNotFoundException;
import com.inkcore.domain.colorconversion.model.DestinationIccProfile;
import com.inkcore.domain.colorconversion.ports.out.IccProfileCachePort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Carga perfiles ICC desde classpath:/color-profiles/ con caché (memoria/Redis).
 * Resuelve alias del catálogo {@link DestinationIccProfile} (p. ej. ISOcoated_v2_eci → FOGRA39).
 * Solo lee bytes en memoria; no escribe archivos en disco.
 */
@Component
public class IccProfileLoader {

    private static final String CLASSPATH_DIR = "color-profiles/";

    private final IccProfileCachePort cache;

    public IccProfileLoader(IccProfileCachePort cache) {
        this.cache = cache;
    }

    public byte[] loadProfileBytes(String profileFileName) {
        String key = resolveExistingFileName(profileFileName);
        return cache.get(key).orElseGet(() -> {
            byte[] bytes = readFromClasspath(key);
            cache.put(key, bytes);
            return bytes;
        });
    }

    /**
     * Nombre de archivo real en classpath usado para cargar (tras alias del catálogo).
     */
    public String resolveExistingFileName(String profileFileName) {
        String requested = normalize(profileFileName);
        List<String> candidates = candidateFileNames(requested);
        for (String candidate : candidates) {
            if (classpathExists(candidate)) {
                return candidate;
            }
        }
        throw new IccProfileNotFoundException(
                requested + " (candidatos: " + String.join(", ", candidates) + ")"
        );
    }

    public boolean isProfileAvailable(DestinationIccProfile profile) {
        if (profile == null) {
            return false;
        }
        for (String candidate : candidatesForCatalog(profile)) {
            if (classpathExists(candidate)) {
                return true;
            }
        }
        return false;
    }

    public boolean exists(String profileFileName) {
        try {
            resolveExistingFileName(profileFileName);
            return true;
        } catch (IccProfileNotFoundException ex) {
            return false;
        }
    }

    /**
     * Archivos .icc/.icm presentes en classpath:/color-profiles/ (excluye README).
     */
    public List<String> listInstalledFileNames() {
        Set<String> names = new LinkedHashSet<>();
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath*:" + CLASSPATH_DIR + "*.{icc,icm,ICC,ICM}");
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename != null && !filename.isBlank()) {
                    names.add(filename);
                }
            }
        } catch (IOException ignored) {
            // best-effort
        }
        // Fallback conocidos
        for (String known : List.of("sRGB.icc", "FOGRA39.icc")) {
            if (classpathExists(known)) {
                names.add(known);
            }
        }
        return List.copyOf(names);
    }

    private static List<String> candidateFileNames(String requested) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        DestinationIccProfile.findByNameOrAlias(requested).ifPresentOrElse(
                profile -> candidates.addAll(candidatesForCatalog(profile)),
                () -> candidates.add(requested)
        );
        return new ArrayList<>(candidates);
    }

    private static List<String> candidatesForCatalog(DestinationIccProfile profile) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        candidates.add(profile.getFileName());
        candidates.addAll(Arrays.asList(profile.getAliases()));
        return new ArrayList<>(candidates);
    }

    private static boolean classpathExists(String profileFileName) {
        return new ClassPathResource(CLASSPATH_DIR + profileFileName).exists();
    }

    private static byte[] readFromClasspath(String profileFileName) {
        ClassPathResource resource = new ClassPathResource(CLASSPATH_DIR + profileFileName);
        if (!resource.exists()) {
            throw new IccProfileNotFoundException(profileFileName);
        }
        try (InputStream in = resource.getInputStream()) {
            return in.readAllBytes();
        } catch (IOException ex) {
            throw new IccProfileNotFoundException(profileFileName);
        }
    }

    private static String normalize(String profileFileName) {
        if (profileFileName == null || profileFileName.isBlank()) {
            throw new IccProfileNotFoundException("(vacío)");
        }
        String name = profileFileName.trim().replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        String lower = name.toLowerCase(Locale.ROOT);
        if (!lower.endsWith(".icc") && !lower.endsWith(".icm")) {
            name = name + ".icc";
        }
        return name;
    }
}
