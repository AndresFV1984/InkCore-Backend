package com.inkcore.infrastructure.out.pdf;

import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.productionorder.ports.out.ProductionOrderPdfPort;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Component
public class ProductionOrderPdfBoxAdapter implements ProductionOrderPdfPort {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    @Override
    public byte[] generateBillingPdf(ProductionOrder order, String clientName, BigDecimal total) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);
            PDType1Font titleFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font bodyFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                float y = 740;
                cs.beginText();
                cs.setFont(titleFont, 16);
                cs.newLineAtOffset(50, y);
                cs.showText("InkCore - Orden de Produccion / Cobro");
                cs.endText();

                y -= 40;
                writeLine(cs, bodyFont, 50, y, "Numero: " + safe(order.getOrderNumber()));
                y -= 18;
                writeLine(cs, bodyFont, 50, y, "Trabajo: " + safe(order.getWorkName()));
                y -= 18;
                writeLine(cs, bodyFont, 50, y, "Cliente: " + safe(clientName));
                y -= 18;
                writeLine(cs, bodyFont, 50, y, "Fecha: "
                        + (order.getOrderDate() == null ? "-" : DATE.format(order.getOrderDate())));
                y -= 18;
                writeLine(cs, bodyFont, 50, y, "Cantidad: " + order.getRequestedQuantity());
                y -= 28;
                writeLine(cs, titleFont, 50, y, "Total a cobrar: "
                        + (total == null ? "0.00" : total.toPlainString()));
                y -= 24;
                if (order.getBilling() != null && order.getBilling().getAdvancePercentage() != null) {
                    writeLine(cs, bodyFont, 50, y, "Anticipo %: "
                            + order.getBilling().getAdvancePercentage().toPlainString());
                }
            }

            document.save(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo generar el PDF de cobro", ex);
        }
    }

    private static void writeLine(PDPageContentStream cs, PDType1Font font, float x, float y, String text)
            throws IOException {
        cs.beginText();
        cs.setFont(font, 11);
        cs.newLineAtOffset(x, y);
        cs.showText(sanitize(text));
        cs.endText();
    }

    private static String safe(String value) {
        return value == null ? "-" : value;
    }

    private static String sanitize(String value) {
        // Helvetica Standard14 no soporta todos los glifos latinos; normalizamos.
        return value
                .replace('á', 'a').replace('é', 'e').replace('í', 'i').replace('ó', 'o').replace('ú', 'u')
                .replace('Á', 'A').replace('É', 'E').replace('Í', 'I').replace('Ó', 'O').replace('Ú', 'U')
                .replace('ñ', 'n').replace('Ñ', 'N');
    }
}
