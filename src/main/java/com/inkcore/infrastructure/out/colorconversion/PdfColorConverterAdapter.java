package com.inkcore.infrastructure.out.colorconversion;

import com.inkcore.domain.colorconversion.exception.ColorConversionFailedException;
import com.inkcore.domain.colorconversion.exception.UnsupportedColorFileException;
import com.inkcore.domain.colorconversion.model.ConversionRequest;
import com.inkcore.domain.colorconversion.model.RasterImageInfo;
import com.inkcore.domain.colorconversion.ports.out.PdfColorConverterPort;
import com.inkcore.infrastructure.config.ColorConversionProperties;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.color.PDICCBased;
import org.apache.pdfbox.pdmodel.graphics.color.PDOutputIntent;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Component;

import java.awt.color.ICC_ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Conversión PDF RGB→CMYK con Apache PDFBox + perfiles ICC (Java puro).
 * <p>
 * Calidad:
 * <ul>
 *   <li>si la página es una foto embebida a página completa → se extrae a resolución nativa (sin re-rasterizar)</li>
 *   <li>si hay vectores/texto → raster a {@code minImageResolutionDpi} (default 600) sin subsampling</li>
 *   <li>salida PDF CMYK Flate: samples convertidos con ICC + espacio ICCBased + OutputIntent (RIP/CTP)</li>
 *   <li>alternativa TIFF CMYK multipágina con ICC embebido</li>
 * </ul>
 */
@Component
public class PdfColorConverterAdapter implements PdfColorConverterPort {

    private final ColorConversionProperties properties;
    private final ImageColorConverterAdapter imageColorConverter;

    public PdfColorConverterAdapter(
            ColorConversionProperties properties,
            ImageColorConverterAdapter imageColorConverter
    ) {
        this.properties = properties;
        this.imageColorConverter = imageColorConverter;
    }

    @Override
    public boolean isAvailable() {
        return properties.isPdfEnabled();
    }

    @Override
    public byte[] convertToCmykPdf(
            ConversionRequest request,
            String sourceIccProfileName,
            String destinationIccProfileName
    ) {
        ensurePdfEnabled();
        int dpi = rasterDpi();
        try (PDDocument input = Loader.loadPDF(new RandomAccessReadBuffer(request.getFileBytes()));
             PDDocument output = new PDDocument()) {

            PDFRenderer renderer = createQualityRenderer(input);
            int pageCount = input.getNumberOfPages();
            if (pageCount < 1) {
                throw new ColorConversionFailedException("El PDF no contiene páginas");
            }

            PDICCBased sharedIcc = null;
            byte[] destinationIccBytes = null;
            for (int i = 0; i < pageCount; i++) {
                PDPage sourcePage = input.getPage(i);
                PDRectangle mediaBox = sourcePage.getMediaBox();
                BufferedImage rgb = rasterizePageHighFidelity(renderer, sourcePage, i, dpi);
                BufferedImage cmyk = imageColorConverter.convertRgbToCmykBufferedImage(
                        rgb,
                        request.getRenderingIntent(),
                        sourceIccProfileName,
                        destinationIccProfileName,
                        request
                );

                if (destinationIccBytes == null) {
                    destinationIccBytes = extractIccBytes(cmyk);
                    sharedIcc = createCmykIccBased(output, destinationIccBytes);
                }

                PDPage targetPage = new PDPage(new PDRectangle(mediaBox.getWidth(), mediaBox.getHeight()));
                output.addPage(targetPage);
                PDImageXObject image = createFlateCmykImage(output, cmyk, sharedIcc);
                try (PDPageContentStream contents = new PDPageContentStream(output, targetPage)) {
                    contents.drawImage(image, 0, 0, mediaBox.getWidth(), mediaBox.getHeight());
                }
            }

            addCmykOutputIntent(output, destinationIccBytes, destinationIccProfileName);
            return savePdf(output);
        } catch (UnsupportedColorFileException | ColorConversionFailedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ColorConversionFailedException("Fallo al convertir PDF a CMYK: " + ex.getMessage(), ex);
        }
    }

    @Override
    public byte[] convertRasterImageToCmykPdf(
            ConversionRequest request,
            String sourceIccProfileName,
            String destinationIccProfileName
    ) {
        ensurePdfEnabled();
        try {
            ImageColorConverterAdapter.LoadedRaster loaded =
                    imageColorConverter.loadRasterHighFidelity(request.getFileBytes());
            double dpi = resolveDpi(loaded.xDpi(), loaded.yDpi());

            BufferedImage cmyk = imageColorConverter.convertRgbToCmykBufferedImage(
                    loaded.image(),
                    request.getRenderingIntent(),
                    sourceIccProfileName,
                    destinationIccProfileName,
                    request
            );

            float widthPts = (float) (cmyk.getWidth() * 72.0 / dpi);
            float heightPts = (float) (cmyk.getHeight() * 72.0 / dpi);

            try (PDDocument output = new PDDocument()) {
                PDPage page = new PDPage(new PDRectangle(widthPts, heightPts));
                output.addPage(page);
                byte[] destinationIccBytes = extractIccBytes(cmyk);
                PDICCBased iccBased = createCmykIccBased(output, destinationIccBytes);
                PDImageXObject image = createFlateCmykImage(output, cmyk, iccBased);
                try (PDPageContentStream contents = new PDPageContentStream(output, page)) {
                    contents.drawImage(image, 0, 0, widthPts, heightPts);
                }
                addCmykOutputIntent(output, destinationIccBytes, destinationIccProfileName);
                return savePdf(output);
            }
        } catch (UnsupportedColorFileException | ColorConversionFailedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ColorConversionFailedException(
                    "Fallo al convertir imagen a PDF CMYK: " + ex.getMessage(),
                    ex
            );
        }
    }

    @Override
    public byte[] convertPdfToCmykTiff(
            ConversionRequest request,
            String sourceIccProfileName,
            String destinationIccProfileName
    ) {
        ensurePdfEnabled();
        int dpi = rasterDpi();
        try (PDDocument input = Loader.loadPDF(new RandomAccessReadBuffer(request.getFileBytes()))) {
            PDFRenderer renderer = createQualityRenderer(input);
            int pageCount = input.getNumberOfPages();
            if (pageCount < 1) {
                throw new ColorConversionFailedException("El PDF no contiene páginas");
            }

            List<BufferedImage> pages = new ArrayList<>(pageCount);
            Double pageDpi = (double) dpi;
            for (int i = 0; i < pageCount; i++) {
                PDPage sourcePage = input.getPage(i);
                BufferedImage rgb = rasterizePageHighFidelity(renderer, sourcePage, i, dpi);
                // Si se extrajo imagen nativa, el DPI efectivo es el de esa imagen sobre el mediaBox
                Optional<BufferedImage> embedded = Optional.empty();
                if (properties.isPreferEmbeddedPdfImages()) {
                    embedded = tryExtractFullPageImage(sourcePage);
                }
                if (embedded.isPresent()) {
                    PDRectangle box = sourcePage.getMediaBox();
                    double wIn = box.getWidth() / 72.0;
                    double hIn = box.getHeight() / 72.0;
                    if (wIn > 0 && hIn > 0) {
                        pageDpi = Math.max(rgb.getWidth() / wIn, rgb.getHeight() / hIn);
                    }
                }
                BufferedImage cmyk = imageColorConverter.convertRgbToCmykBufferedImage(
                        rgb,
                        request.getRenderingIntent(),
                        sourceIccProfileName,
                        destinationIccProfileName,
                        request
                );
                pages.add(cmyk);
            }

            byte[] iccBytes = null;
            if (pages.get(0).getColorModel().getColorSpace() instanceof ICC_ColorSpace ics) {
                iccBytes = ics.getProfile().getData();
            }
            return imageColorConverter.writeCmykTiffPages(pages, iccBytes, pageDpi, pageDpi);
        } catch (UnsupportedColorFileException | ColorConversionFailedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ColorConversionFailedException("Fallo al convertir PDF a TIFF CMYK: " + ex.getMessage(), ex);
        }
    }

    @Override
    public RasterImageInfo readInfo(byte[] pdfBytes) {
        int dpi = rasterDpi();
        try (PDDocument doc = Loader.loadPDF(new RandomAccessReadBuffer(pdfBytes))) {
            int pages = doc.getNumberOfPages();
            if (pages < 1) {
                return new RasterImageInfo(0, 0, (double) dpi, (double) dpi, 8, "Flate", 0);
            }
            PDPage page0 = doc.getPage(0);
            if (properties.isPreferEmbeddedPdfImages()) {
                Optional<BufferedImage> embedded = tryExtractFullPageImage(page0);
                if (embedded.isPresent()) {
                    BufferedImage img = embedded.get();
                    PDRectangle box = page0.getMediaBox();
                    double wIn = box.getWidth() / 72.0;
                    double hIn = box.getHeight() / 72.0;
                    double xDpi = wIn > 0 ? img.getWidth() / wIn : dpi;
                    double yDpi = hIn > 0 ? img.getHeight() / hIn : dpi;
                    return new RasterImageInfo(img.getWidth(), img.getHeight(), xDpi, yDpi, 8, null, pages);
                }
            }
            PDRectangle box = page0.getMediaBox();
            int widthPx = Math.max(1, Math.round(box.getWidth() / 72f * dpi));
            int heightPx = Math.max(1, Math.round(box.getHeight() / 72f * dpi));
            return new RasterImageInfo(widthPx, heightPx, (double) dpi, (double) dpi, 8, "Flate", pages);
        } catch (IOException ex) {
            int pages = Math.max(1, countPageMarkers(pdfBytes));
            return new RasterImageInfo(0, 0, (double) dpi, (double) dpi, 8, null, pages);
        }
    }

    private BufferedImage rasterizePageHighFidelity(
            PDFRenderer renderer,
            PDPage page,
            int pageIndex,
            int dpi
    ) throws IOException {
        if (properties.isPreferEmbeddedPdfImages()) {
            Optional<BufferedImage> embedded = tryExtractFullPageImage(page);
            if (embedded.isPresent()) {
                return embedded.get();
            }
        }
        return renderer.renderImageWithDPI(pageIndex, dpi, ImageType.RGB);
    }

    /**
     * Extrae la imagen a resolución nativa si la página tiene una sola imagen embebida
     * (caso típico: PDF = foto exportada). Evita pérdida de nitidez por re-rasterizado.
     */
    private Optional<BufferedImage> tryExtractFullPageImage(PDPage page) throws IOException {
        List<PDImageXObject> images = new ArrayList<>();
        collectImages(page.getResources(), images, 0);
        if (images.size() != 1) {
            return Optional.empty();
        }
        PDImageXObject image = images.get(0);
        int iw = image.getWidth();
        int ih = image.getHeight();
        if (iw < 32 || ih < 32) {
            return Optional.empty();
        }
        PDRectangle box = page.getMediaBox();
        // Heurística: la imagen cubre al menos ~80% del área de página a 72–600 dpi equivalentes
        double pageW = box.getWidth();
        double pageH = box.getHeight();
        double minDpi = Math.min(iw / (pageW / 72.0), ih / (pageH / 72.0));
        if (minDpi < 72) {
            return Optional.empty();
        }
        return Optional.of(image.getImage());
    }

    private void collectImages(PDResources resources, List<PDImageXObject> out, int depth) throws IOException {
        if (resources == null || depth > 4) {
            return;
        }
        for (COSName name : resources.getXObjectNames()) {
            PDXObject xObject = resources.getXObject(name);
            if (xObject instanceof PDImageXObject image) {
                out.add(image);
            } else if (xObject instanceof PDFormXObject form) {
                collectImages(form.getResources(), out, depth + 1);
            }
        }
    }

    private PDFRenderer createQualityRenderer(PDDocument document) {
        PDFRenderer renderer = new PDFRenderer(document);
        renderer.setSubsamplingAllowed(false);
        return renderer;
    }

    private void ensurePdfEnabled() {
        if (!properties.isPdfEnabled()) {
            throw new UnsupportedColorFileException(
                    "Conversión PDF deshabilitada (inkcore.color-conversion.pdf-enabled=false)."
            );
        }
    }

    private int rasterDpi() {
        return Math.max(72, properties.getMinImageResolutionDpi());
    }

    private double resolveDpi(Double xDpi, Double yDpi) {
        if (xDpi != null && xDpi > 0) {
            return xDpi;
        }
        if (yDpi != null && yDpi > 0) {
            return yDpi;
        }
        // Imágenes sin metadatos: 300 dpi (impresión). No usar 600 (solo raster PDF con vectores).
        return 300.0;
    }

    private static byte[] savePdf(PDDocument output) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        output.save(baos);
        byte[] result = baos.toByteArray();
        if (result.length == 0) {
            throw new ColorConversionFailedException("PDF CMYK vacío tras la conversión");
        }
        return result;
    }

    /**
     * Crea una imagen PDF CMYK 8-bit con Flate aplicado una sola vez.
     * <p>
     * Samples ya convertidos con el perfil destino en Java. El espacio de color es ICCBased
     * (con Alternate DeviceCMYK) cuando hay bytes ICC; si no, DeviceCMYK.
     * Además el documento lleva OutputIntent con el mismo perfil para RIP/CTP.
     * <p>
     * Importante: no pre-comprimir y pasar a {@code new PDStream(..., FLATE_DECODE)} —
     * ese constructor vuelve a codificar y el visor ve basura (cuadro negro en fotos reales).
     */
    static PDImageXObject createFlateCmykImage(PDDocument document, BufferedImage cmyk) throws IOException {
        return createFlateCmykImage(document, cmyk, createCmykIccBased(document, extractIccBytes(cmyk)));
    }

    static PDImageXObject createFlateCmykImage(
            PDDocument document,
            BufferedImage cmyk,
            PDICCBased iccBased
    ) throws IOException {
        int width = cmyk.getWidth();
        int height = cmyk.getHeight();
        byte[] samples = extractCmykSamples(cmyk);

        PDStream stream = new PDStream(document);
        try (OutputStream out = stream.createOutputStream(COSName.FLATE_DECODE)) {
            out.write(samples);
        }

        COSDictionary dict = stream.getCOSObject();
        dict.setItem(COSName.TYPE, COSName.XOBJECT);
        dict.setItem(COSName.SUBTYPE, COSName.IMAGE);
        dict.setInt(COSName.WIDTH, width);
        dict.setInt(COSName.HEIGHT, height);
        dict.setInt(COSName.BITS_PER_COMPONENT, 8);
        if (iccBased != null) {
            dict.setItem(COSName.COLORSPACE, iccBased);
        } else {
            dict.setItem(COSName.COLORSPACE, COSName.DEVICECMYK);
        }

        return new PDImageXObject(stream, null);
    }

    /**
     * Espacio ICCBased CMYK reutilizable (una instancia por documento).
     * Alternate = DeviceCMYK para lectores que no interpretan el perfil.
     */
    static PDICCBased createCmykIccBased(PDDocument document, byte[] iccBytes) throws IOException {
        if (document == null || iccBytes == null || iccBytes.length < 128) {
            return null;
        }
        PDICCBased iccBased = new PDICCBased(document);
        try (OutputStream out = iccBased.getPDStream().createOutputStream(COSName.FLATE_DECODE)) {
            out.write(iccBytes);
        }
        COSDictionary streamDict = iccBased.getPDStream().getCOSObject();
        streamDict.setInt(COSName.N, 4);
        streamDict.setItem(COSName.ALTERNATE, COSName.DEVICECMYK);
        return iccBased;
    }

    static void addCmykOutputIntent(PDDocument document, byte[] iccBytes, String profileName)
            throws IOException {
        if (document == null || iccBytes == null || iccBytes.length < 128) {
            return;
        }
        String label = (profileName == null || profileName.isBlank()) ? "CMYK ICC" : profileName.trim();
        try (ByteArrayInputStream in = new ByteArrayInputStream(iccBytes)) {
            PDOutputIntent intent = new PDOutputIntent(document, in);
            intent.setInfo(label);
            intent.setOutputCondition(label);
            intent.setOutputConditionIdentifier(label);
            intent.setRegistryName("http://www.color.org");
            document.getDocumentCatalog().addOutputIntent(intent);
        }
    }

    static byte[] extractIccBytes(BufferedImage cmyk) {
        if (cmyk != null
                && cmyk.getColorModel() != null
                && cmyk.getColorModel().getColorSpace() instanceof ICC_ColorSpace ics
                && ics.getProfile() != null) {
            byte[] data = ics.getProfile().getData();
            if (data != null && data.length >= 128) {
                return data;
            }
        }
        return null;
    }

    private static byte[] extractCmykSamples(BufferedImage cmyk) {
        int width = cmyk.getWidth();
        int height = cmyk.getHeight();
        WritableRaster raster = cmyk.getRaster();
        int bands = Math.min(4, raster.getNumBands());
        boolean ushort = raster.getDataBuffer().getDataType() == java.awt.image.DataBuffer.TYPE_USHORT;
        byte[] samples = new byte[Math.multiplyExact(Math.multiplyExact(width, height), 4)];

        if (bands == 4 && !ushort) {
            // Ruta rápida: samples interleaved C,M,Y,K
            int[] row = new int[width * 4];
            int idx = 0;
            for (int y = 0; y < height; y++) {
                raster.getPixels(0, y, width, 1, row);
                for (int v : row) {
                    samples[idx++] = (byte) clampByte(v);
                }
            }
            return samples;
        }

        int idx = 0;
        int[] pixel = new int[Math.max(4, bands)];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                raster.getPixel(x, y, pixel);
                if (ushort) {
                    for (int b = 0; b < 4; b++) {
                        int v = b < bands ? pixel[b] : 0;
                        samples[idx++] = (byte) Math.min(255, (v * 255) / 65535);
                    }
                } else {
                    samples[idx++] = (byte) clampByte(pixel[0]);
                    samples[idx++] = (byte) clampByte(bands > 1 ? pixel[1] : 0);
                    samples[idx++] = (byte) clampByte(bands > 2 ? pixel[2] : 0);
                    samples[idx++] = (byte) clampByte(bands > 3 ? pixel[3] : 0);
                }
            }
        }
        return samples;
    }

    private static int clampByte(int v) {
        if (v < 0) {
            return 0;
        }
        return Math.min(255, v);
    }

    private static int countPageMarkers(byte[] pdfBytes) {
        String text = new String(pdfBytes, java.nio.charset.StandardCharsets.ISO_8859_1);
        int count = 0;
        int from = 0;
        while (true) {
            int at = text.indexOf("/Type /Page", from);
            if (at < 0) {
                break;
            }
            if (at + 11 >= text.length() || text.charAt(at + 11) != 's') {
                count++;
            }
            from = at + 11;
        }
        return count;
    }
}
