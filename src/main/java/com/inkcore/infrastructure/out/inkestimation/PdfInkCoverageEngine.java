package com.inkcore.infrastructure.out.inkestimation;

import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDVectorFont;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDColorSpace;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceCMYK;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceGray;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceN;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.graphics.color.PDICCBased;
import org.apache.pdfbox.pdmodel.graphics.color.PDIndexed;
import org.apache.pdfbox.pdmodel.graphics.color.PDPattern;
import org.apache.pdfbox.pdmodel.graphics.color.PDSeparation;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.util.Vector;

import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cobertura de tinta PDF sin RIP de pago ni Ghostscript (AGPL).
 * <p>
 * Acumula área × tint en unidades de página para:
 * <ul>
 *   <li>Rellenos/trazos vectoriales DeviceCMYK, Separation, DeviceN, RGB/Gray (RGB→ICC)</li>
 *   <li>Glifos de texto vectorial</li>
 *   <li>Imágenes con muestras nativas CMYK / Separation / DeviceN (getRawRaster)</li>
 * </ul>
 * Licencia: PDFBox Apache 2.0.
 */
final class PdfInkCoverageEngine extends PDFGraphicsStreamEngine {

    private final RgbToCmykConverter rgbToCmyk;
    private final double pageArea;
    private final GeneralPath linePath = new GeneralPath();
    private Point2D.Float currentPoint = new Point2D.Float(0, 0);
    private int clipWindingRule = -1;

    private final double[] processInkArea = new double[4];
    private final Map<String, SpotAccum> spots = new LinkedHashMap<>();

    PdfInkCoverageEngine(PDPage page, RgbToCmykConverter rgbToCmyk) {
        super(page);
        this.rgbToCmyk = rgbToCmyk;
        float w = page.getCropBox().getWidth();
        float h = page.getCropBox().getHeight();
        this.pageArea = Math.max(1e-6, (double) w * (double) h);
    }

    PageInkCoverage result() {
        double[] means = new double[4];
        for (int i = 0; i < 4; i++) {
            means[i] = clamp100((processInkArea[i] / pageArea) * 100.0);
        }
        List<SpotCoverage> spotList = new ArrayList<>();
        for (SpotAccum s : spots.values()) {
            spotList.add(new SpotCoverage(
                    s.name,
                    clamp100((s.inkArea / pageArea) * 100.0),
                    s.swatchHex,
                    s.measured
            ));
        }
        return new PageInkCoverage(means, spotList);
    }

    @Override
    public void appendRectangle(Point2D p0, Point2D p1, Point2D p2, Point2D p3) {
        linePath.moveTo((float) p0.getX(), (float) p0.getY());
        linePath.lineTo((float) p1.getX(), (float) p1.getY());
        linePath.lineTo((float) p2.getX(), (float) p2.getY());
        linePath.lineTo((float) p3.getX(), (float) p3.getY());
        linePath.closePath();
        currentPoint = new Point2D.Float((float) p0.getX(), (float) p0.getY());
    }

    @Override
    public void drawImage(PDImage pdImage) throws IOException {
        if (!(pdImage instanceof PDImageXObject image)) {
            return;
        }
        Matrix ctm = getGraphicsState().getCurrentTransformationMatrix();
        AffineTransform at = ctm.createAffineTransform();
        // Imagen unitaria 0..1 en espacio de usuario del XObject
        Shape imageRect = at.createTransformedShape(new Rectangle2D.Float(0, 0, 1, 1));
        double paintedArea = clippedAreaFast(imageRect);
        if (paintedArea <= 0) {
            return;
        }

        PDColorSpace cs;
        try {
            cs = image.getColorSpace();
        } catch (Exception ex) {
            // ICC embebido inválido: estimar vía raster crudo/RGB seguro
            accumulateRgbImageSafe(image, paintedArea);
            return;
        }

        Integer iccComponents = iccComponentCount(cs);
        if (cs instanceof PDDeviceCMYK || (iccComponents != null && iccComponents == 4)) {
            accumulateNativeCmykImage(image, paintedArea);
            return;
        }
        if (cs instanceof PDSeparation sep) {
            accumulateSeparationImage(image, sep, paintedArea);
            return;
        }
        // Indexed / Pattern con Separation debajo
        PDColorSpace unwrapped;
        try {
            unwrapped = PdfSpotColorSpaces.unwrap(cs);
        } catch (Exception ex) {
            accumulateRgbImageSafe(image, paintedArea);
            return;
        }
        if (unwrapped instanceof PDSeparation sep) {
            accumulateSeparationImage(image, sep, paintedArea);
            return;
        }
        if (unwrapped instanceof PDDeviceN deviceN) {
            accumulateDeviceNImage(image, deviceN, paintedArea);
            return;
        }
        if (cs instanceof PDDeviceN deviceN) {
            accumulateDeviceNImage(image, deviceN, paintedArea);
            return;
        }
        // RGB / Gray / ICC RGB/Gray / otros: decode tolerante a ICC inválido
        accumulateRgbImageSafe(image, paintedArea);
    }

    @Override
    public void clip(int windingRule) {
        clipWindingRule = windingRule;
    }

    @Override
    public void moveTo(float x, float y) {
        currentPoint = new Point2D.Float(x, y);
        linePath.moveTo(x, y);
    }

    @Override
    public void lineTo(float x, float y) {
        currentPoint = new Point2D.Float(x, y);
        linePath.lineTo(x, y);
    }

    @Override
    public void curveTo(float x1, float y1, float x2, float y2, float x3, float y3) {
        currentPoint = new Point2D.Float(x3, y3);
        linePath.curveTo(x1, y1, x2, y2, x3, y3);
    }

    @Override
    public Point2D getCurrentPoint() {
        return currentPoint;
    }

    @Override
    public void closePath() {
        linePath.closePath();
    }

    @Override
    public void endPath() {
        if (clipWindingRule != -1) {
            linePath.setWindingRule(clipWindingRule);
            if (!linePath.getPathIterator(null).isDone()) {
                getGraphicsState().intersectClippingPath(linePath);
            }
            clipWindingRule = -1;
        }
        linePath.reset();
    }

    @Override
    public void strokePath() throws IOException {
        strokeCurrentPath();
        linePath.reset();
    }

    @Override
    public void fillPath(int windingRule) throws IOException {
        linePath.setWindingRule(windingRule);
        fillShape(linePath, getGraphicsState().getNonStrokingColor());
        linePath.reset();
    }

    @Override
    public void fillAndStrokePath(int windingRule) throws IOException {
        GeneralPath path = (GeneralPath) linePath.clone();
        path.setWindingRule(windingRule);
        fillShape(path, getGraphicsState().getNonStrokingColor());
        strokeCurrentPath();
        linePath.reset();
    }

    @Override
    public void shadingFill(COSName shadingName) {
        // Sombreados complejos: omitidos (sin RIP). No bloquean el resto.
    }

    @Override
    protected void showFontGlyph(
            Matrix textRenderingMatrix,
            PDFont font,
            int code,
            Vector displacement
    ) throws IOException {
        if (!(font instanceof PDVectorFont vectorFont)) {
            super.showFontGlyph(textRenderingMatrix, font, code, displacement);
            return;
        }
        RenderingMode mode = getGraphicsState().getTextState().getRenderingMode();
        if (mode == RenderingMode.NEITHER) {
            return;
        }
        GeneralPath glyphPath = vectorFont.getPath(code);
        if (glyphPath == null) {
            return;
        }
        AffineTransform at = textRenderingMatrix.createAffineTransform();
        at.concatenate(font.getFontMatrix().createAffineTransform());
        if (!font.isEmbedded() && !font.isVertical() && !font.isStandard14() && font.hasExplicitWidth(code)) {
            float fontWidth = font.getWidthFromFont(code);
            if (displacement.getX() > 0 && fontWidth > 0
                    && Math.abs(fontWidth - displacement.getX() * 1000) > 0.0001) {
                at.scale((displacement.getX() * 1000) / fontWidth, 1);
            }
        }
        Shape glyph = at.createTransformedShape(glyphPath);
        // Texto: área por bounds (mucho más rápido que Area/shoelace por glifo; sesgo leve OK en estimación)
        if (mode.isFill()) {
            fillShapeApprox(glyph, getGraphicsState().getNonStrokingColor());
        }
        if (mode.isStroke()) {
            strokeShape(glyph, getGraphicsState().getStrokingColor());
        }
    }

    private void fillShapeApprox(Shape shape, PDColor color) throws IOException {
        if (shape == null || color == null) {
            return;
        }
        Rectangle2D bounds = shape.getBounds2D();
        if (bounds.getWidth() <= 0 || bounds.getHeight() <= 0) {
            return;
        }
        double a = clippedAreaFast(bounds);
        if (a <= 0) {
            return;
        }
        // Compensación: el glifo no llena el bounding box (~55 % típico)
        applyColor(color, a * 0.55);
    }

    private void strokeCurrentPath() throws IOException {
        strokeShape(linePath, getGraphicsState().getStrokingColor());
    }

    private void strokeShape(Shape shape, PDColor color) throws IOException {
        float width = getGraphicsState().getLineWidth();
        Matrix ctm = getGraphicsState().getCurrentTransformationMatrix();
        float scaleX = (float) Math.hypot(ctm.getScaleX(), ctm.getShearY());
        float scaleY = (float) Math.hypot(ctm.getShearX(), ctm.getScaleY());
        float avgScale = Math.max(1e-6f, (Math.abs(scaleX) + Math.abs(scaleY)) / 2f);
        float strokeWidth = Math.max(0.25f / avgScale, width);
        BasicStroke stroke = new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        Shape stroked = stroke.createStrokedShape(shape);
        fillShape(stroked, color);
    }

    private void fillShape(Shape shape, PDColor color) throws IOException {
        if (shape == null || color == null) {
            return;
        }
        double a = clippedAreaFast(shape);
        if (a <= 0) {
            return;
        }
        applyColor(color, a);
    }

    /**
     * Área pintada tras clip. Evita crear {@link Area} / intersect cuando el clip
     * no recorta (caso típico: clip = página). Misma geometría que antes cuando sí hay corte.
     */
    private double clippedAreaFast(Shape shape) {
        Area clip = getGraphicsState().getCurrentClippingPath();
        if (clip == null || clip.isEmpty()) {
            return Math.abs(areaOfShape(shape));
        }
        Rectangle2D bounds = shape.getBounds2D();
        if (bounds.getWidth() > 0 && bounds.getHeight() > 0 && clip.contains(bounds)) {
            return Math.abs(areaOfShape(shape));
        }
        Area painted = new Area(shape);
        painted.intersect(clip);
        return Math.abs(areaOf(painted));
    }

    private void applyColor(PDColor color, double area) throws IOException {
        if (color == null) {
            return;
        }
        PDColorSpace cs;
        float[] components;
        try {
            cs = PdfSpotColorSpaces.unwrap(color.getColorSpace());
            components = color.getComponents();
        } catch (Exception ex) {
            return;
        }
        if (cs instanceof PDSeparation sep) {
            float tint = components.length > 0 ? components[0] : 1f;
            // Indexed→Separation: el componente es índice; si venía de Indexed ya unwrapeamos
            // y los componentes pueden no ser tint 0–1. Si el color original era Indexed, tint≈1 en práctica.
            try {
                if (color.getColorSpace() instanceof PDIndexed) {
                    tint = 1f;
                }
            } catch (Exception ignored) {
                // espacio original ilegible
            }
            applySeparation(sep, tint, area);
            return;
        }
        if (cs instanceof PDDeviceN deviceN) {
            applyDeviceN(deviceN, components, area);
            return;
        }
        if (cs instanceof PDPattern) {
            // Pattern coloreado: spots suelen estar en recursos del tiling (inventario).
            // No sumar proceso vía toRGB (duplicaría tinta de forma incorrecta).
            return;
        }
        if (cs instanceof PDDeviceCMYK) {
            addProcess(components, area);
            return;
        }
        if (cs instanceof PDDeviceGray) {
            float g = components.length > 0 ? components[0] : 0f;
            addProcess(new float[]{0, 0, 0, 1f - g}, area);
            return;
        }
        Integer iccN = iccComponentCount(cs);
        if (cs instanceof PDDeviceRGB || (iccN != null && iccN == 3)) {
            float r = components.length > 0 ? components[0] : 0f;
            float g = components.length > 1 ? components[1] : 0f;
            float b = components.length > 2 ? components[2] : 0f;
            addProcess(rgbToCmyk.toCmyk(r, g, b), area);
            return;
        }
        if (iccN != null && iccN == 4) {
            addProcess(components, area);
            return;
        }
        if (iccN != null && iccN == 1) {
            float g = components.length > 0 ? components[0] : 0f;
            addProcess(new float[]{0, 0, 0, 1f - g}, area);
            return;
        }
        try {
            float[] rgb = color.getColorSpace().toRGB(components);
            addProcess(rgbToCmyk.toCmyk(rgb[0], rgb[1], rgb[2]), area);
        } catch (Exception ignored) {
            // color no resoluble / ICC inválido
        }
    }

    private void applySeparation(PDSeparation sep, float tint, double area) throws IOException {
        String raw = PdfSpotColorSpaces.separationColorantName(sep);
        if (raw == null) {
            return;
        }
        if (SpotColorantNames.isProcessName(raw)) {
            int idx = SpotColorantNames.processIndex(raw);
            if (idx >= 0) {
                processInkArea[idx] += clamp01(tint) * area;
            }
            return;
        }
        String inkName = SpotColorantNames.exactSpotReference(raw);
        if (inkName == null) {
            return;
        }
        SpotAccum spot = spots.computeIfAbsent(inkName, SpotAccum::new);
        spot.inkArea += clamp01(tint) * area;
        spot.measured = true;
        if (spot.swatchHex == null) {
            spot.swatchHex = PdfSpotColorSpaces.swatchFromSeparation(sep);
        }
    }

    private void applyDeviceN(PDDeviceN deviceN, float[] components, double area) throws IOException {
        List<String> names = deviceN.getColorantNames();
        if (names == null) {
            return;
        }
        for (int i = 0; i < names.size() && i < components.length; i++) {
            String raw = names.get(i);
            if (raw == null) {
                continue;
            }
            float tint = clamp01(components[i]);
            if (SpotColorantNames.isProcessName(raw)) {
                int idx = SpotColorantNames.processIndex(raw);
                if (idx >= 0) {
                    processInkArea[idx] += tint * area;
                }
                continue;
            }
            String inkName = SpotColorantNames.exactSpotReference(raw);
            if (inkName == null) {
                continue;
            }
            SpotAccum spot = spots.computeIfAbsent(inkName, SpotAccum::new);
            spot.inkArea += tint * area;
            spot.measured = true;
            if (spot.swatchHex == null) {
                spot.swatchHex = "#808080";
            }
        }
    }

    private void addProcess(float[] cmyk, double area) {
        if (cmyk == null) {
            return;
        }
        for (int i = 0; i < 4; i++) {
            float v = i < cmyk.length ? clamp01(cmyk[i]) : 0f;
            processInkArea[i] += v * area;
        }
    }

    private void accumulateNativeCmykImage(PDImageXObject image, double paintedArea) throws IOException {
        WritableRaster raw;
        try {
            raw = image.getRawRaster();
        } catch (Exception ex) {
            accumulateRgbImageSafe(image, paintedArea);
            return;
        }
        if (raw == null) {
            accumulateRgbImageSafe(image, paintedArea);
            return;
        }
        int bands = Math.min(4, raw.getNumBands());
        int w = raw.getWidth();
        int h = raw.getHeight();
        double max = sampleMax(raw);
        int step = FastInkRgbToCmyk.sampleStep(w, h);
        double[] sum = new double[4];
        long counted = 0;
        for (int y = 0; y < h; y += step) {
            for (int x = 0; x < w; x += step) {
                for (int b = 0; b < bands; b++) {
                    sum[b] += raw.getSample(x, y, b) / max;
                }
                counted++;
            }
        }
        if (counted == 0) {
            return;
        }
        for (int b = 0; b < 4; b++) {
            processInkArea[b] += (sum[b] / counted) * paintedArea;
        }
    }

    private void accumulateSeparationImage(PDImageXObject image, PDSeparation sep, double paintedArea)
            throws IOException {
        double meanTint = 0;
        try {
            WritableRaster raw = image.getRawRaster();
            if (raw != null) {
                meanTint = meanBand(raw, 0);
            } else {
                BufferedImage img = decodeImageIgnoringBadIcc(image, 1);
                meanTint = img == null ? 0 : meanBand(img.getRaster(), 0);
            }
        } catch (Exception ignored) {
            meanTint = 0;
        }
        applySeparation(sep, (float) meanTint, paintedArea);
    }

    private void accumulateDeviceNImage(PDImageXObject image, PDDeviceN deviceN, double paintedArea)
            throws IOException {
        List<String> names = deviceN.getColorantNames();
        WritableRaster raw = image.getRawRaster();
        if (names == null || raw == null) {
            return;
        }
        int bands = raw.getNumBands();
        float[] components = new float[names.size()];
        for (int i = 0; i < names.size() && i < bands; i++) {
            components[i] = (float) meanBand(raw, i);
        }
        applyDeviceN(deviceN, components, paintedArea);
    }

    private void accumulateRgbImage(BufferedImage rgb, double paintedArea) {
        double[] mean = rgbToCmyk.meanCmyk01FromRgbImage(rgb);
        for (int i = 0; i < 4; i++) {
            processInkArea[i] += mean[i] * paintedArea;
        }
    }

    private void accumulateRgbImageSafe(PDImageXObject image, double paintedArea) {
        int w = Math.max(1, image.getWidth());
        int h = Math.max(1, image.getHeight());
        int maxEdge = Math.max(w, h);
        int subsample = maxEdge <= 512 ? 1 : (int) Math.ceil(maxEdge / 512.0);
        BufferedImage rgb = decodeImageIgnoringBadIcc(image, subsample);
        if (rgb == null) {
            return;
        }
        accumulateRgbImage(rgb, paintedArea);
    }

    /**
     * Decodifica imagen tolerando perfiles ICC embebidos corruptos
     * ({@code CMMException: Invalid ICC Profile Data}).
     */
    private static BufferedImage decodeImageIgnoringBadIcc(PDImageXObject image, int subsample) {
        try {
            return image.getImage(null, subsample);
        } catch (Exception ignored) {
            // continuar con fallbacks
        }
        try {
            return image.getOpaqueImage(null, subsample);
        } catch (Exception ignored) {
            // continuar
        }
        try {
            return rgbFromRawRaster(image);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static BufferedImage rgbFromRawRaster(PDImageXObject image) throws IOException {
        WritableRaster raw = image.getRawRaster();
        if (raw == null) {
            return null;
        }
        int w = raw.getWidth();
        int h = raw.getHeight();
        int bands = raw.getNumBands();
        double max = sampleMax(raw);
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int r;
                int g;
                int b;
                if (bands >= 3) {
                    r = clamp255Byte(raw.getSample(x, y, 0) / max);
                    g = clamp255Byte(raw.getSample(x, y, 1) / max);
                    b = clamp255Byte(raw.getSample(x, y, 2) / max);
                } else {
                    int gray = clamp255Byte(raw.getSample(x, y, 0) / max);
                    r = g = b = gray;
                }
                out.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
        return out;
    }

    /** null si no es ICC o si el perfil es ilegible. */
    private static Integer iccComponentCount(PDColorSpace cs) {
        if (!(cs instanceof PDICCBased icc)) {
            return null;
        }
        try {
            return icc.getNumberOfComponents();
        } catch (Exception ex) {
            return null;
        }
    }

    private static int clamp255Byte(double v01) {
        return Math.max(0, Math.min(255, (int) Math.round(v01 * 255.0)));
    }

    private Area clipToCurrent(Area shape) {
        Area clip = getGraphicsState().getCurrentClippingPath();
        if (clip == null || clip.isEmpty()) {
            return shape;
        }
        Rectangle2D bounds = shape.getBounds2D();
        // Evita intersect costoso cuando el clip contiene el shape (caso típico: clip = página)
        if (bounds.getWidth() > 0 && bounds.getHeight() > 0 && clip.contains(bounds)) {
            return shape;
        }
        Area out = new Area(shape);
        out.intersect(clip);
        return out;
    }

    private static double areaOfShape(Shape shape) {
        if (shape == null) {
            return 0;
        }
        if (shape instanceof Rectangle2D rect) {
            return Math.abs(rect.getWidth() * rect.getHeight());
        }
        Rectangle2D bounds = shape.getBounds2D();
        if (shape instanceof Area area) {
            return areaOf(area);
        }
        // Path directo (sin envolver en Area): mismo shoelace que antes
        double shoelace = Math.abs(shoelace(shape));
        if (shoelace <= 0) {
            return Math.abs(bounds.getWidth() * bounds.getHeight());
        }
        return shoelace;
    }

    private static double areaOf(Area area) {
        if (area == null || area.isEmpty()) {
            return 0;
        }
        Rectangle2D bounds = area.getBounds2D();
        if (area.isRectangular()) {
            return Math.abs(bounds.getWidth() * bounds.getHeight());
        }
        // Estimación: shoelace del path; si es demasiado denso, cae a bounds (más rápido)
        double shoelace = Math.abs(shoelace(area));
        if (shoelace <= 0) {
            return Math.abs(bounds.getWidth() * bounds.getHeight());
        }
        return shoelace;
    }

    private static double shoelace(Shape shape) {
        double sum = 0;
        double[] coords = new double[6];
        double startX = 0;
        double startY = 0;
        double lastX = 0;
        double lastY = 0;
        boolean started = false;
        var it = shape.getPathIterator(null);
        while (!it.isDone()) {
            int type = it.currentSegment(coords);
            switch (type) {
                case java.awt.geom.PathIterator.SEG_MOVETO -> {
                    startX = lastX = coords[0];
                    startY = lastY = coords[1];
                    started = true;
                }
                case java.awt.geom.PathIterator.SEG_LINETO -> {
                    if (started) {
                        sum += lastX * coords[1] - coords[0] * lastY;
                        lastX = coords[0];
                        lastY = coords[1];
                    }
                }
                case java.awt.geom.PathIterator.SEG_QUADTO -> {
                    if (started) {
                        // Aproximar con línea al punto final
                        sum += lastX * coords[3] - coords[2] * lastY;
                        lastX = coords[2];
                        lastY = coords[3];
                    }
                }
                case java.awt.geom.PathIterator.SEG_CUBICTO -> {
                    if (started) {
                        sum += lastX * coords[5] - coords[4] * lastY;
                        lastX = coords[4];
                        lastY = coords[5];
                    }
                }
                case java.awt.geom.PathIterator.SEG_CLOSE -> {
                    if (started) {
                        sum += lastX * startY - startX * lastY;
                        lastX = startX;
                        lastY = startY;
                    }
                }
                default -> {
                }
            }
            it.next();
        }
        return sum / 2.0;
    }

    private static double meanBand(WritableRaster raster, int band) {
        int w = raster.getWidth();
        int h = raster.getHeight();
        double max = sampleMax(raster);
        int step = FastInkRgbToCmyk.sampleStep(w, h);
        double sum = 0;
        long counted = 0;
        for (int y = 0; y < h; y += step) {
            for (int x = 0; x < w; x += step) {
                sum += raster.getSample(x, y, band) / max;
                counted++;
            }
        }
        return counted == 0 ? 0 : sum / counted;
    }

    private static double sampleMax(WritableRaster raster) {
        if (raster.getDataBuffer().getDataType() == DataBuffer.TYPE_USHORT) {
            return 65535.0;
        }
        if (raster.getSampleModel().getSampleSize(0) == 1) {
            return 1.0;
        }
        return 255.0;
    }

    private static float clamp01(float v) {
        if (v < 0f) {
            return 0f;
        }
        if (v > 1f) {
            return 1f;
        }
        return v;
    }

    private static double clamp100(double v) {
        if (v < 0) {
            return 0;
        }
        if (v > 100) {
            return 100;
        }
        return Math.round(v * 100.0) / 100.0;
    }

    private static int clamp255(int v) {
        return Math.max(0, Math.min(255, v));
    }

    record PageInkCoverage(double[] processMeans, List<SpotCoverage> spots) {
    }

    record SpotCoverage(String name, double coveragePercent, String swatchHex, boolean measured) {
    }

    private static final class SpotAccum {
        private final String name;
        private double inkArea;
        private String swatchHex;
        private boolean measured;

        private SpotAccum(String name) {
            this.name = name;
        }
    }
}
