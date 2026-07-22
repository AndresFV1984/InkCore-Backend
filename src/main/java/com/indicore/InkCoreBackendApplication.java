package com.indicore;

/**
 * Compatibilidad con run configurations antiguas de IntelliJ.
 * Preferir {@link com.inkcore.InkCoreBackendApplication}.
 */
@Deprecated(since = "0.0.1", forRemoval = true)
public final class InkCoreBackendApplication {

    private InkCoreBackendApplication() {
    }

    public static void main(String[] args) {
        com.inkcore.InkCoreBackendApplication.main(args);
    }
}
