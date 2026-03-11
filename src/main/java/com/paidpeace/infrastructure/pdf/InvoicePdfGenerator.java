package com.paidpeace.infrastructure.pdf;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

import com.paidpeace.domain.customer.Customer;
import com.paidpeace.domain.invoice.Invoice;
import com.paidpeace.domain.invoice.InvoiceItem;
import com.paidpeace.domain.organization.Organization;
import com.paidpeace.exception.http.InternalException;
import com.paidpeace.exception.http.ValidationException;

@Component
public class InvoicePdfGenerator {

    private static final PDRectangle PAGE_SIZE = PDRectangle.A4;
    private static final float PAGE_WIDTH = PAGE_SIZE.getWidth();
    private static final float PAGE_HEIGHT = PAGE_SIZE.getHeight();
    private static final float MARGIN = 50f;
    private static final float CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2);
    private static final float BOTTOM_MARGIN = 55f;
    private static final float HEADER_GAP = 18f;
    private static final float SECTION_GAP = 22f;
    private static final float LINE_GAP = 14f;
    private static final float TABLE_ROW_PADDING = 6f;
    private static final float MIN_TOTALS_SPACE = 120f;

    private static final PDFont FONT_REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDFont FONT_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDFont FONT_ITALIC = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

    private static final float FONT_SMALL = 9f;
    private static final float FONT_NORMAL = 10.5f;
    private static final float FONT_SECTION = 12f;
    private static final float FONT_TITLE = 20f;

    private static final Color COLOR_TEXT = new Color(34, 34, 34);
    private static final Color COLOR_MUTED = new Color(100, 100, 100);
    private static final Color COLOR_BORDER = new Color(214, 220, 229);
    private static final Color COLOR_HEADER_FILL = new Color(245, 247, 250);
    private static final Color COLOR_ACCENT = new Color(36, 99, 235);

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM uuuu");

    public byte[] generate(Invoice invoice) {
        validateInvoice(invoice);

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            PageContext page = startPage(document);
            writeHeader(page, invoice);
            writeInvoiceMeta(page, invoice);
            writeBillingSection(page, invoice);
            writeItemsTable(page, document, invoice);
            writeTotals(page, document, invoice);
            writeFooter(page, invoice);
            page.close();

            document.save(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new InternalException("Failed to generate invoice PDF: " + ex.getMessage());
        }
    }

    public String buildFileName(Invoice invoice) {
        validateInvoice(invoice);
        return "invoice-" + sanitizeFileName(invoice.getInvoiceNumber()) + ".pdf";
    }

    private void validateInvoice(Invoice invoice) {
        if (invoice == null) {
            throw new ValidationException("Invoice must not be null");
        }
        if (invoice.getOrganization() == null) {
            throw new ValidationException("Invoice organization must not be null");
        }
        if (invoice.getInvoiceNumber() == null || invoice.getInvoiceNumber().isBlank()) {
            throw new ValidationException("Invoice number must not be blank");
        }
    }

    private void writeHeader(PageContext page, Invoice invoice) throws IOException {
        Organization organization = invoice.getOrganization();

        drawText(page, FONT_BOLD, FONT_TITLE, MARGIN, page.y, safe(organization.getName()), COLOR_ACCENT);
        page.y -= 18f;

        for (String line : buildOrganizationLines(organization)) {
            drawText(page, FONT_REGULAR, FONT_NORMAL, MARGIN, page.y, line, COLOR_TEXT);
            page.y -= LINE_GAP;
        }

        float rightX = PAGE_WIDTH - MARGIN;
        drawRightAlignedText(page, FONT_BOLD, 22f, rightX, PAGE_HEIGHT - MARGIN, "INVOICE", COLOR_TEXT);
        drawRightAlignedText(page, FONT_BOLD, FONT_NORMAL, rightX, PAGE_HEIGHT - MARGIN - 22f,
                "#" + invoice.getInvoiceNumber(), COLOR_TEXT);

        page.y -= 6f;
        drawHorizontalRule(page, page.y);
        page.y -= HEADER_GAP;
    }

    private void writeInvoiceMeta(PageContext page, Invoice invoice) throws IOException {
        float leftColumnX = MARGIN;
        float rightColumnX = MARGIN + (CONTENT_WIDTH / 2f);
        float baseY = page.y;

        drawLabelValue(page, leftColumnX, baseY, "Invoice ID", invoice.getId() != null ? invoice.getId().toString() : "-");
        drawLabelValue(page, leftColumnX, baseY - 18f, "Lifecycle", invoice.getLifeCycleStatus().name());
        drawLabelValue(page, leftColumnX, baseY - 36f, "Time Status", invoice.getTimeStatus().name());

        drawLabelValue(page, rightColumnX, baseY, "Issue Date", formatDate(invoice.getIssueDate()));
        drawLabelValue(page, rightColumnX, baseY - 18f, "Due Date", formatDate(invoice.getDueDate()));
        drawLabelValue(page, rightColumnX, baseY - 36f, "Generated", formatDate(LocalDate.now(resolveZone(invoice))));

        page.y = baseY - 58f;
    }

    private void writeBillingSection(PageContext page, Invoice invoice) throws IOException {
        Customer customer = invoice.getCustomer();

        float leftX = MARGIN;
        float rightX = MARGIN + (CONTENT_WIDTH / 2f);
        float titleY = page.y;

        drawText(page, FONT_BOLD, FONT_SECTION, leftX, titleY, "From", COLOR_TEXT);
        drawText(page, FONT_BOLD, FONT_SECTION, rightX, titleY, "Bill To", COLOR_TEXT);

        float contentY = titleY - 16f;
        for (String line : buildOrganizationLines(invoice.getOrganization())) {
            drawText(page, FONT_REGULAR, FONT_NORMAL, leftX, contentY, line, COLOR_TEXT);
            contentY -= LINE_GAP;
        }

        float customerY = titleY - 16f;
        for (String line : buildCustomerLines(customer)) {
            drawText(page, FONT_REGULAR, FONT_NORMAL, rightX, customerY, line, COLOR_TEXT);
            customerY -= LINE_GAP;
        }

        page.y = Math.min(contentY, customerY) - SECTION_GAP;
    }

    private void writeItemsTable(PageContext page, PDDocument document, Invoice invoice) throws IOException {
        page = ensurePageSpace(page, document, 60f, invoice);
        writeItemsHeader(page);

        List<InvoiceItem> items = invoice.getItems();
        if (items == null || items.isEmpty()) {
            float emptyHeight = 24f;
            drawRowBorder(page, page.y, emptyHeight);
            drawText(page, FONT_ITALIC, FONT_NORMAL, MARGIN + 8f, page.y - 16f, "No line items", COLOR_MUTED);
            page.y -= emptyHeight;
            page.y -= SECTION_GAP;
            return;
        }

        float descriptionWidth = CONTENT_WIDTH * 0.43f;
        float quantityX = MARGIN + descriptionWidth + 16f;
        float unitPriceX = MARGIN + CONTENT_WIDTH * 0.69f;
        float amountX = PAGE_WIDTH - MARGIN - 8f;

        for (InvoiceItem item : items) {
            List<String> wrappedDescription = wrapText(
                    safe(item.getDescription()),
                    FONT_REGULAR,
                    FONT_NORMAL,
                    descriptionWidth - 10f
            );
            float rowHeight = Math.max(24f, wrappedDescription.size() * LINE_GAP + TABLE_ROW_PADDING);

            if (page.y - rowHeight < BOTTOM_MARGIN + MIN_TOTALS_SPACE) {
                page.close();
                page = startContinuationPage(document, invoice);
                writeItemsHeader(page);
            }

            drawRowBorder(page, page.y, rowHeight);
            float rowTextY = page.y - 14f;

            for (int i = 0; i < wrappedDescription.size(); i++) {
                drawText(page, FONT_REGULAR, FONT_NORMAL, MARGIN + 8f, rowTextY - (i * LINE_GAP),
                        wrappedDescription.get(i), COLOR_TEXT);
            }

            drawRightAlignedText(page, FONT_REGULAR, FONT_NORMAL, quantityX + 44f, rowTextY,
                    String.valueOf(item.getQuantity()), COLOR_TEXT);
            drawRightAlignedText(page, FONT_REGULAR, FONT_NORMAL, unitPriceX + 72f, rowTextY,
                    formatMoney(invoice, item.getUnitPrice()), COLOR_TEXT);
            drawRightAlignedText(page, FONT_BOLD, FONT_NORMAL, amountX, rowTextY,
                    formatMoney(invoice, item.getAmount()), COLOR_TEXT);

            page.y -= rowHeight;
        }

        page.y -= SECTION_GAP;
    }

    private void writeItemsHeader(PageContext page) throws IOException {
        float headerHeight = 22f;
        page.stream.setNonStrokingColor(COLOR_HEADER_FILL);
        page.stream.addRect(MARGIN, page.y - headerHeight, CONTENT_WIDTH, headerHeight);
        page.stream.fill();
        page.stream.setNonStrokingColor(COLOR_TEXT);

        drawText(page, FONT_BOLD, FONT_SMALL, MARGIN + 8f, page.y - 14f, "Description", COLOR_TEXT);
        drawRightAlignedText(page, FONT_BOLD, FONT_SMALL, MARGIN + CONTENT_WIDTH * 0.60f, page.y - 14f, "Qty", COLOR_TEXT);
        drawRightAlignedText(page, FONT_BOLD, FONT_SMALL, MARGIN + CONTENT_WIDTH * 0.82f, page.y - 14f, "Unit Price", COLOR_TEXT);
        drawRightAlignedText(page, FONT_BOLD, FONT_SMALL, PAGE_WIDTH - MARGIN - 8f, page.y - 14f, "Amount", COLOR_TEXT);

        drawRowBorder(page, page.y, headerHeight);
        page.y -= headerHeight;
    }

    private void writeTotals(PageContext page, PDDocument document, Invoice invoice) throws IOException {
        if (page.y < BOTTOM_MARGIN + MIN_TOTALS_SPACE) {
            page.close();
            page = startContinuationPage(document, invoice);
        }

        float totalsWidth = 210f;
        float totalsX = PAGE_WIDTH - MARGIN - totalsWidth;
        float labelX = totalsX + 12f;
        float valueX = PAGE_WIDTH - MARGIN - 12f;
        float boxTop = page.y;
        float boxHeight = 72f;

        page.stream.setNonStrokingColor(COLOR_HEADER_FILL);
        page.stream.addRect(totalsX, boxTop - boxHeight, totalsWidth, boxHeight);
        page.stream.fill();
        page.stream.setNonStrokingColor(COLOR_TEXT);
        strokeRect(page, totalsX, boxTop - boxHeight, totalsWidth, boxHeight);

        drawText(page, FONT_REGULAR, FONT_NORMAL, labelX, boxTop - 18f, "Subtotal", COLOR_TEXT);
        drawRightAlignedText(page, FONT_REGULAR, FONT_NORMAL, valueX, boxTop - 18f,
                formatMoney(invoice, invoice.getSubtotal()), COLOR_TEXT);

        drawText(page, FONT_REGULAR, FONT_NORMAL, labelX, boxTop - 36f,
                "Tax (" + formatPercentage(invoice.getTaxInPercentage()) + ")", COLOR_TEXT);
        drawRightAlignedText(page, FONT_REGULAR, FONT_NORMAL, valueX, boxTop - 36f,
                formatMoney(invoice, calculateTaxAmount(invoice)), COLOR_TEXT);

        drawText(page, FONT_BOLD, FONT_SECTION, labelX, boxTop - 58f, "Total", COLOR_TEXT);
        drawRightAlignedText(page, FONT_BOLD, FONT_SECTION, valueX, boxTop - 58f,
                formatMoney(invoice, invoice.getTotalAmount()), COLOR_TEXT);

        page.y = boxTop - boxHeight - SECTION_GAP;
    }

    private void writeFooter(PageContext page, Invoice invoice) throws IOException {
        String footer = invoice.isCancelled()
                ? "This invoice has been cancelled."
                : invoice.isDraft()
                ? "This is a draft invoice and may be subject to change."
                : "Thank you for your business.";

        if (page.y < BOTTOM_MARGIN + 30f) {
            drawFooterLine(page, invoice);
            return;
        }

        drawText(page, FONT_ITALIC, FONT_NORMAL, MARGIN, page.y, footer, COLOR_MUTED);
        drawFooterLine(page, invoice);
    }

    private void drawFooterLine(PageContext page, Invoice invoice) throws IOException {
        float footerY = BOTTOM_MARGIN - 10f;
        drawHorizontalRule(page, footerY + 12f);
        String left = safe(invoice.getOrganization().getName()) + " | " + safe(invoice.getOrganization().getEmail());
        String right = "Invoice " + invoice.getInvoiceNumber();
        drawText(page, FONT_REGULAR, FONT_SMALL, MARGIN, footerY, left, COLOR_MUTED);
        drawRightAlignedText(page, FONT_REGULAR, FONT_SMALL, PAGE_WIDTH - MARGIN, footerY, right, COLOR_MUTED);
    }

    private void drawLabelValue(PageContext page, float x, float y, String label, String value) throws IOException {
        drawText(page, FONT_BOLD, FONT_SMALL, x, y, label.toUpperCase(Locale.ROOT), COLOR_MUTED);
        drawText(page, FONT_REGULAR, FONT_NORMAL, x, y - 11f, value, COLOR_TEXT);
    }

    private void drawHorizontalRule(PageContext page, float y) throws IOException {
        page.stream.setStrokingColor(COLOR_BORDER);
        page.stream.moveTo(MARGIN, y);
        page.stream.lineTo(PAGE_WIDTH - MARGIN, y);
        page.stream.stroke();
    }

    private void drawRowBorder(PageContext page, float topY, float height) throws IOException {
        strokeRect(page, MARGIN, topY - height, CONTENT_WIDTH, height);
    }

    private void strokeRect(PageContext page, float x, float y, float width, float height) throws IOException {
        page.stream.setStrokingColor(COLOR_BORDER);
        page.stream.addRect(x, y, width, height);
        page.stream.stroke();
    }

    private PageContext ensurePageSpace(PageContext page, PDDocument document, float requiredHeight, Invoice invoice)
            throws IOException {
        if (page.y - requiredHeight >= BOTTOM_MARGIN) {
            return page;
        }
        page.close();
        return startContinuationPage(document, invoice);
    }

    private PageContext startPage(PDDocument document) throws IOException {
        PDPage pdfPage = new PDPage(PAGE_SIZE);
        document.addPage(pdfPage);
        PDPageContentStream stream = new PDPageContentStream(document, pdfPage);
        return new PageContext(stream, PAGE_HEIGHT - MARGIN);
    }

    private PageContext startContinuationPage(PDDocument document, Invoice invoice) throws IOException {
        PageContext page = startPage(document);
        drawText(page, FONT_BOLD, 16f, MARGIN, page.y, "Invoice " + invoice.getInvoiceNumber() + " (continued)", COLOR_TEXT);
        page.y -= 18f;
        drawHorizontalRule(page, page.y);
        page.y -= HEADER_GAP;
        return page;
    }

    private List<String> buildOrganizationLines(Organization organization) {
        List<String> lines = new ArrayList<>();
        lines.add(safe(organization.getName()));
        if (organization.getAddress() != null && !organization.getAddress().isBlank()) {
            lines.add(organization.getAddress().trim());
        }
        if (organization.getEmail() != null && !organization.getEmail().isBlank()) {
            lines.add(organization.getEmail().trim());
        }
        if (organization.getPhone() != null && !organization.getPhone().isBlank()) {
            lines.add(organization.getPhone().trim());
        }
        return lines;
    }

    private List<String> buildCustomerLines(Customer customer) {
        List<String> lines = new ArrayList<>();
        if (customer == null) {
            lines.add("No customer assigned");
            return lines;
        }
        lines.add(safe(customer.getName()));
        if (customer.getCompanyName() != null && !customer.getCompanyName().isBlank()) {
            lines.add(customer.getCompanyName().trim());
        }
        if (customer.getAddress() != null && !customer.getAddress().isBlank()) {
            lines.add(customer.getAddress().trim());
        }
        if (customer.getEmail() != null && !customer.getEmail().isBlank()) {
            lines.add(customer.getEmail().trim());
        }
        if (customer.getPhone() != null && !customer.getPhone().isBlank()) {
            lines.add(customer.getPhone().trim());
        }
        return lines;
    }

    private List<String> wrapText(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank()) {
            lines.add("");
            return lines;
        }

        StringBuilder currentLine = new StringBuilder();
        for (String word : text.trim().split("\\s+")) {
            String candidate = currentLine.isEmpty() ? word : currentLine + " " + word;
            if (textWidth(candidate, font, fontSize) <= maxWidth) {
                currentLine.setLength(0);
                currentLine.append(candidate);
                continue;
            }
            if (!currentLine.isEmpty()) {
                lines.add(currentLine.toString());
                currentLine.setLength(0);
            }
            if (textWidth(word, font, fontSize) <= maxWidth) {
                currentLine.append(word);
            } else {
                lines.add(truncateToWidth(word, font, fontSize, maxWidth));
            }
        }
        if (!currentLine.isEmpty()) {
            lines.add(currentLine.toString());
        }
        return lines;
    }

    private String truncateToWidth(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
        String ellipsis = "...";
        String candidate = text;
        while (!candidate.isEmpty() && textWidth(candidate + ellipsis, font, fontSize) > maxWidth) {
            candidate = candidate.substring(0, candidate.length() - 1);
        }
        return candidate.isEmpty() ? ellipsis : candidate + ellipsis;
    }

    private float textWidth(String text, PDFont font, float fontSize) throws IOException {
        return font.getStringWidth(text) / 1000f * fontSize;
    }

    private void drawText(PageContext page, PDFont font, float fontSize, float x, float y, String text, Color color)
            throws IOException {
        page.stream.beginText();
        page.stream.setFont(font, fontSize);
        page.stream.setNonStrokingColor(color);
        page.stream.newLineAtOffset(x, y);
        page.stream.showText(safe(text));
        page.stream.endText();
    }

    private void drawRightAlignedText(PageContext page, PDFont font, float fontSize, float rightX, float y,
                                      String text, Color color) throws IOException {
        String safeText = safe(text);
        float width = textWidth(safeText, font, fontSize);
        drawText(page, font, fontSize, rightX - width, y, safeText, color);
    }

    private String formatDate(LocalDate date) {
        return date == null ? "-" : DATE_FORMATTER.format(date);
    }

    private String formatMoney(Invoice invoice, BigDecimal amount) {
        Currency currency = invoice.getOrganization().getCurrency();
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.US);
        if (currency != null) {
            format.setCurrency(currency);
        }
        BigDecimal safeAmount = amount == null ? BigDecimal.ZERO : amount;
        return format.format(safeAmount);
    }

    private String formatPercentage(BigDecimal percentage) {
        BigDecimal safeValue = percentage == null ? BigDecimal.ZERO : percentage;
        return safeValue.stripTrailingZeros().toPlainString() + "%";
    }

    private BigDecimal calculateTaxAmount(Invoice invoice) {
        BigDecimal subtotal = invoice.getSubtotal() == null ? BigDecimal.ZERO : invoice.getSubtotal();
        BigDecimal total = invoice.getTotalAmount() == null ? BigDecimal.ZERO : invoice.getTotalAmount();
        return total.subtract(subtotal).max(BigDecimal.ZERO);
    }

    private ZoneId resolveZone(Invoice invoice) {
        return invoice.getOrganization().getTimezone() != null
                ? invoice.getOrganization().getTimezone()
                : ZoneId.systemDefault();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String sanitizeFileName(String value) {
        return safe(value).trim().replaceAll("[^a-zA-Z0-9-_\\.]", "_");
    }

    private static final class PageContext {
        private final PDPageContentStream stream;
        private float y;

        private PageContext(PDPageContentStream stream, float y) {
            this.stream = stream;
            this.y = y;
        }

        private void close() throws IOException {
            stream.close();
        }
    }
}
