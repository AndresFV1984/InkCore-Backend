package com.inkcore.infrastructure.out.colorconversion.lcms;

import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Carga perezosa de LittleCMS por nombre lógico {@code lcms2}.
 * <p>
 * JNA resuelve automáticamente los binarios empaquetados en el classpath:
 * {@code win32-x86-64/lcms2.dll} y {@code linux-x86-64/liblcms2.so}.
 * No requiere instalación manual ni {@code jna.library.path}.
 * <p>
 * Override opcional: {@code LCMS_LIBRARY_PATH} / ruta absoluta a la nativa
 * (solo diagnóstico; el default es el recurso empaquetado).
 */
final class LcmsNativeLoader {

    private static final Logger log = LoggerFactory.getLogger(LcmsNativeLoader.class);
    private static final String LOGICAL_NAME = "lcms2";
    private static final AtomicReference<Lcms2Library> INSTANCE = new AtomicReference<>();
    private static final AtomicReference<String> LOAD_ERROR = new AtomicReference<>();

    private LcmsNativeLoader() {
    }

    static Lcms2Library require() {
        Lcms2Library lib = tryLoad(null);
        if (lib == null) {
            throw new LcmsNativeUnavailableException(
                    "LittleCMS (lcms2) no está disponible: "
                            + (LOAD_ERROR.get() != null ? LOAD_ERROR.get() : "biblioteca nativa no cargada")
                            + ". Esperado en el JAR: win32-x86-64/lcms2.dll o linux-x86-64/liblcms2.so. "
                            + "Ver color-profiles/README.txt § LittleCMS."
            );
        }
        return lib;
    }

    static boolean isAvailable(String optionalLibraryPath) {
        return tryLoad(optionalLibraryPath) != null;
    }

    static Lcms2Library tryLoad(String optionalLibraryPath) {
        Lcms2Library cached = INSTANCE.get();
        if (cached != null) {
            return cached;
        }
        synchronized (LcmsNativeLoader.class) {
            cached = INSTANCE.get();
            if (cached != null) {
                return cached;
            }
            try {
                applyOptionalSearchPath(optionalLibraryPath);
                Lcms2Library lib = Native.load(LOGICAL_NAME, Lcms2Library.class);
                INSTANCE.set(lib);
                LOAD_ERROR.set(null);
                log.info("LittleCMS (lcms2) cargado correctamente");
                return lib;
            } catch (UnsatisfiedLinkError | Exception ex) {
                LOAD_ERROR.set(ex.getMessage());
                log.warn("No se pudo cargar LittleCMS (lcms2): {}", ex.getMessage());
                return null;
            }
        }
    }

    /**
     * Solo si se pasa una ruta absoluta (override). No altera el nombre lógico de carga.
     */
    private static void applyOptionalSearchPath(String optionalLibraryPath) {
        if (optionalLibraryPath == null || optionalLibraryPath.isBlank()) {
            return;
        }
        Path path = Path.of(optionalLibraryPath.trim());
        if (Files.isRegularFile(path)) {
            NativeLibrary.addSearchPath(LOGICAL_NAME, path.getParent().toAbsolutePath().toString());
        } else if (Files.isDirectory(path)) {
            NativeLibrary.addSearchPath(LOGICAL_NAME, path.toAbsolutePath().toString());
        }
    }
}
