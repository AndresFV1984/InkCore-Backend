package com.inkcore.infrastructure.out.inkestimation;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.color.PDColorSpace;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.pattern.PDAbstractPattern;
import org.apache.pdfbox.pdmodel.graphics.pattern.PDTilingPattern;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceDictionary;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Inventario de colorantes Separation/DeviceN en recursos del PDF
 * (forms, patrones, imágenes, apariencias de anotaciones).
 */
final class PdfSpotColorScanner {

    private PdfSpotColorScanner() {
    }

    /**
     * @return mapa nombre exacto → swatch hex aproximado
     */
    static Map<String, String> scanPage(PDPage page) throws IOException {
        Map<String, String> found = new LinkedHashMap<>();
        collect(page.getResources(), found, 0);
        List<PDAnnotation> annotations = page.getAnnotations();
        if (annotations != null) {
            for (PDAnnotation annotation : annotations) {
                collectAnnotation(annotation, found);
            }
        }
        return found;
    }

    private static void collectAnnotation(PDAnnotation annotation, Map<String, String> found) {
        if (annotation == null) {
            return;
        }
        try {
            PDAppearanceDictionary appearance = annotation.getAppearance();
            if (appearance == null) {
                return;
            }
            collectAppearanceEntry(appearance.getNormalAppearance(), found);
        } catch (Exception ignored) {
            // apariencia no legible
        }
    }

    private static void collectAppearanceEntry(
            org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceEntry entry,
            Map<String, String> found
    ) throws IOException {
        if (entry == null) {
            return;
        }
        if (entry.isStream()) {
            PDAppearanceStream stream = entry.getAppearanceStream();
            if (stream != null) {
                collect(stream.getResources(), found, 1);
            }
            return;
        }
        if (entry.isSubDictionary()) {
            for (PDAppearanceStream stream : entry.getSubDictionary().values()) {
                if (stream != null) {
                    collect(stream.getResources(), found, 1);
                }
            }
        }
    }

    private static void collect(PDResources resources, Map<String, String> found, int depth) throws IOException {
        if (resources == null || depth > 10) {
            return;
        }
        for (COSName name : resources.getColorSpaceNames()) {
            try {
                register(resources.getColorSpace(name), found);
            } catch (Exception ignored) {
                // ICC / Separation ilegible: no tumbar inventario
            }
        }
        for (COSName name : resources.getXObjectNames()) {
            try {
                PDXObject xObject = resources.getXObject(name);
                if (xObject instanceof PDFormXObject form) {
                    collect(form.getResources(), found, depth + 1);
                } else if (xObject instanceof PDImageXObject image) {
                    try {
                        register(image.getColorSpace(), found);
                    } catch (Exception ignored) {
                        // imagen con ICC inválido
                    }
                }
            } catch (Exception ignored) {
                // XObject no legible
            }
        }
        for (COSName name : resources.getPatternNames()) {
            try {
                PDAbstractPattern pattern = resources.getPattern(name);
                if (pattern instanceof PDTilingPattern tiling) {
                    collect(tiling.getResources(), found, depth + 1);
                }
            } catch (Exception ignored) {
                // patrón no legible
            }
        }
    }

    private static void register(PDColorSpace cs, Map<String, String> found) throws IOException {
        found.putAll(PdfSpotColorSpaces.extractReportableSpots(cs));
    }
}
