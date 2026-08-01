package com.inkcore.infrastructure.out.inkestimation;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Garantiza nombres de referencia literales contra un PDF real con Pantones.
 * Se omite si el archivo no está en la máquina del desarrollador.
 */
class RealPdfPantoneInventoryTest {

    private static final Path SAMPLE = Path.of(
            "E:/Desarrollos Personales/Imagenes con Pantone/Actividad-Fisica-Deportes-Presencial-MED-CVS.pdf"
    );

    static boolean sampleExists() {
        return Files.isRegularFile(SAMPLE);
    }

    @Test
    @EnabledIf("sampleExists")
    void declaredNames_matchExactPdfColorants() throws Exception {
        Set<String> found = new LinkedHashSet<>();
        try (PDDocument doc = Loader.loadPDF(SAMPLE.toFile())) {
            for (PDPage page : doc.getPages()) {
                Map<String, String> inventory = PdfSpotColorScanner.scanPage(page);
                found.addAll(inventory.keySet());
            }
        }

        assertTrue(found.contains("PANTONE Medium Blue C"), "faltó Medium Blue C: " + found);
        assertTrue(found.contains("PANTONE 2925 C"), "faltó 2925 C: " + found);
        assertTrue(found.contains("PANTONE 2915 C"), "faltó 2915 C: " + found);

        for (String name : found) {
            assertEquals(name, SpotColorantNames.exactSpotReference(name));
            assertTrue(SpotColorantNames.looksLikePantone(name), "esperado Pantone: " + name);
        }
    }
}
