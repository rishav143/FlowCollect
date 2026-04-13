package com.flowcollect.infrastructure.pdf;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

import com.flowcollect.infrastructure.util.CurrencyFormatter;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.stereotype.Component;

import com.flowcollect.domain.customer.Customer;
import com.flowcollect.domain.invoice.Invoice;
import com.flowcollect.domain.invoice.InvoiceItem;
import com.flowcollect.domain.invoice.InvoiceTaxLine;
import com.flowcollect.domain.organization.Organization;
import com.flowcollect.exception.http.InternalException;
import com.flowcollect.exception.http.ValidationException;

@Component
public class InvoicePdfGenerator {

    // ── Page geometry ──────────────────────────────────────────────────────────
    private static final PDRectangle PAGE_SIZE     = PDRectangle.A4;
    private static final float       PAGE_WIDTH    = PAGE_SIZE.getWidth();
    private static final float       PAGE_HEIGHT   = PAGE_SIZE.getHeight();
    private static final float       MARGIN        = 50f;
    private static final float       CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2);
    private static final float       BOTTOM_MARGIN = 55f;

    // ── Spacing rhythm ─────────────────────────────────────────────────────────
    private static final float HEADER_GAP      = 22f;
    private static final float SECTION_GAP     = 28f;
    private static final float LINE_GAP        = 16f;
    private static final float LABEL_VALUE_GAP = 13f;

    // ── Table geometry ─────────────────────────────────────────────────────────
    private static final float TABLE_HEADER_HEIGHT = 26f;
    private static final float TABLE_ROW_PADDING   = 12f;
    private static final float MIN_TOTALS_SPACE    = 150f;

    private static final float COL_DESC_LEFT        = MARGIN + 8f;
    private static final float COL_QTY_RIGHT        = MARGIN + CONTENT_WIDTH * 0.58f;
    private static final float COL_UNIT_PRICE_RIGHT = MARGIN + CONTENT_WIDTH * 0.81f;
    private static final float COL_AMOUNT_RIGHT     = PAGE_WIDTH - MARGIN - 8f;
    private static final float COL_DESC_WIDTH       = CONTENT_WIDTH * 0.43f;

    // ── Font sizes ─────────────────────────────────────────────────────────────
    private static final float FONT_SMALL   = 9f;
    private static final float FONT_NORMAL  = 10.5f;
    private static final float FONT_SECTION = 12f;
    private static final float FONT_TITLE   = 20f;

    // ── Colours ────────────────────────────────────────────────────────────────
    private static final Color COLOR_TEXT        = new Color(34, 34, 34);
    private static final Color COLOR_MUTED       = new Color(110, 110, 110);
    private static final Color COLOR_BORDER      = new Color(214, 220, 229);
    private static final Color COLOR_DIVIDER     = new Color(230, 233, 238);
    private static final Color COLOR_HEADER_FILL = new Color(245, 247, 250);
    private static final Color COLOR_ACCENT      = new Color(36, 99, 235);

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM uuuu");

    // ── Font resource paths ────────────────────────────────────────────────────
    private static final String FONT_PATH_REGULAR = "/fonts/NotoSans-Regular.ttf";
    private static final String FONT_PATH_BOLD    = "/fonts/NotoSans-Bold.ttf";
    private static final String FONT_PATH_ITALIC  = "/fonts/NotoSans-Italic.ttf";

    // ── Public API ─────────────────────────────────────────────────────────────

    public byte[] generate(Invoice invoice) {
        validateInvoice(invoice);

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            FontSet fonts = loadFonts(document);
            PageContext page = startPage(document);
            writeHeader(page, fonts, invoice);
            writeInvoiceMeta(page, fonts, invoice);
            writeBillingSection(page, fonts, invoice);
            page = writeItemsTable(page, fonts, document, invoice);
            page = writeTotals(page, fonts, document, invoice);
            writeFooter(page, fonts, invoice);
            page.close();

            // Second pass: stamp footer bar + page numbers on every page
            int totalPages = document.getNumberOfPages();
            for (int i = 0; i < totalPages; i++) {
                try (PDPageContentStream cs = new PDPageContentStream(
                        document, document.getPage(i),
                        PDPageContentStream.AppendMode.APPEND, true, true)) {
                    drawFooterLine(new PageContext(cs, 0), fonts, invoice, i + 1, totalPages);
                }
            }

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

    // ── Font loading ───────────────────────────────────────────────────────────

    /**
     * Loads TrueType fonts scoped to this document. PDType0Font embeds the font
     * subset into the PDF, which means every Unicode character (₹, €, ¥, ₩, etc.)
     * renders correctly regardless of what fonts the viewer has installed.
     */
    private FontSet loadFonts(PDDocument document) throws IOException {
        return new FontSet(
            loadFont(document, FONT_PATH_REGULAR),
            loadFont(document, FONT_PATH_BOLD),
            loadFont(document, FONT_PATH_ITALIC)
        );
    }

    private PDType0Font loadFont(PDDocument document, String resourcePath) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new InternalException("Font resource not found: " + resourcePath);
            }
            return PDType0Font.load(document, is, true);
        }
    }

    // ── Validation ─────────────────────────────────────────────────────────────

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

    // ── Section writers ────────────────────────────────────────────────────────

    private void writeHeader(PageContext page, FontSet fonts, Invoice invoice) throws IOException {
        Organization org = invoice.getOrganization();

        drawText(page, fonts.bold, FONT_TITLE, MARGIN, page.y, safe(org.getName()), COLOR_ACCENT);
        page.y -= 24f;

        for (String line : buildOrganizationLines(org)) {
            drawText(page, fonts.regular, FONT_NORMAL, MARGIN, page.y, line, COLOR_MUTED);
            page.y -= LINE_GAP;
        }

        float rightX = PAGE_WIDTH - MARGIN;
        drawRightAlignedText(page, fonts.bold, 22f, rightX, PAGE_HEIGHT - MARGIN, "INVOICE", COLOR_TEXT);
        drawRightAlignedText(page, fonts.bold, FONT_SECTION, rightX, PAGE_HEIGHT - MARGIN - 26f,
                "#" + invoice.getInvoiceNumber(), COLOR_MUTED);

        page.y -= 8f;
        drawHorizontalRule(page, page.y, COLOR_BORDER);
        page.y -= HEADER_GAP;
    }

    /**
     * Minimal date strip — only the two fields a recipient actually needs:
     * Invoice Date and Due Date.  Internal fields (UUID, lifecycle, time-status,
     * generated timestamp) are deliberately omitted from the customer-facing document.
     */
    private void writeInvoiceMeta(PageContext page, FontSet fonts, Invoice invoice) throws IOException {
        float col1X = MARGIN;
        float col2X = MARGIN + (CONTENT_WIDTH / 2f);
        float baseY = page.y;

        drawLabelValue(page, fonts, col1X, baseY, "INVOICE DATE", formatDate(invoice.getIssueDate()));
        drawLabelValue(page, fonts, col2X, baseY, "DUE DATE",     formatDate(invoice.getDueDate()));

        page.y = baseY - LABEL_VALUE_GAP - 16f;
        drawHorizontalRule(page, page.y, COLOR_DIVIDER);
        page.y -= SECTION_GAP;
    }

    private void writeBillingSection(PageContext page, FontSet fonts, Invoice invoice) throws IOException {
        float leftX  = MARGIN;
        float rightX = MARGIN + (CONTENT_WIDTH / 2f);
        float titleY = page.y;

        drawText(page, fonts.bold, FONT_SECTION, leftX,  titleY, "From",    COLOR_TEXT);
        drawText(page, fonts.bold, FONT_SECTION, rightX, titleY, "Bill To", COLOR_TEXT);

        float leftY  = titleY - 20f;
        float rightY = titleY - 20f;

        for (String line : buildOrganizationLines(invoice.getOrganization())) {
            drawText(page, fonts.regular, FONT_NORMAL, leftX, leftY, line, COLOR_TEXT);
            leftY -= LINE_GAP;
        }
        for (String line : buildCustomerLines(invoice.getCustomer())) {
            drawText(page, fonts.regular, FONT_NORMAL, rightX, rightY, line, COLOR_TEXT);
            rightY -= LINE_GAP;
        }

        page.y = Math.min(leftY, rightY) - SECTION_GAP;
    }

    private PageContext writeItemsTable(PageContext page, FontSet fonts, PDDocument document, Invoice invoice) throws IOException {
        page = ensurePageSpace(page, document, 60f);
        writeItemsHeader(page, fonts);

        List<InvoiceItem> items = invoice.getItems();
        if (items == null || items.isEmpty()) {
            float emptyHeight = 32f;
            drawRowBorder(page, page.y, emptyHeight);
            drawText(page, fonts.italic, FONT_NORMAL, COL_DESC_LEFT, page.y - 18f, "No line items", COLOR_MUTED);
            page.y -= emptyHeight + SECTION_GAP;
            return page;
        }

        for (InvoiceItem item : items) {
            List<String> wrappedDesc = wrapText(safe(item.getDescription()), fonts.regular, FONT_NORMAL, COL_DESC_WIDTH - 10f);
            float rowHeight = Math.max(28f, wrappedDesc.size() * LINE_GAP + TABLE_ROW_PADDING);

            if (page.y - rowHeight < BOTTOM_MARGIN + MIN_TOTALS_SPACE) {
                page.close();
                page = startContinuationPage(document);
                writeItemsHeader(page, fonts);
            }

            drawRowBorder(page, page.y, rowHeight);

            float rowTextY = page.y - ((rowHeight - (wrappedDesc.size() * LINE_GAP)) / 2f) - FONT_NORMAL;

            for (int i = 0; i < wrappedDesc.size(); i++) {
                drawText(page, fonts.regular, FONT_NORMAL,
                        COL_DESC_LEFT, rowTextY - (i * LINE_GAP), wrappedDesc.get(i), COLOR_TEXT);
            }

            drawRightAlignedText(page, fonts.regular, FONT_NORMAL, COL_QTY_RIGHT,        rowTextY, String.valueOf(item.getQuantity()),           COLOR_TEXT);
            drawRightAlignedText(page, fonts.regular, FONT_NORMAL, COL_UNIT_PRICE_RIGHT, rowTextY, formatMoney(invoice, item.getUnitPrice()),     COLOR_TEXT);
            drawRightAlignedText(page, fonts.bold,    FONT_NORMAL, COL_AMOUNT_RIGHT,     rowTextY, formatMoney(invoice, item.getAmount()),        COLOR_TEXT);

            page.y -= rowHeight;
        }

        page.y -= SECTION_GAP;
        return page;
    }

    private void writeItemsHeader(PageContext page, FontSet fonts) throws IOException {
        page.stream.setNonStrokingColor(COLOR_HEADER_FILL);
        page.stream.addRect(MARGIN, page.y - TABLE_HEADER_HEIGHT, CONTENT_WIDTH, TABLE_HEADER_HEIGHT);
        page.stream.fill();

        drawRowBorder(page, page.y, TABLE_HEADER_HEIGHT);

        float textY = page.y - TABLE_HEADER_HEIGHT / 2f - FONT_SMALL / 2f;

        drawText(page, fonts.bold, FONT_SMALL, COL_DESC_LEFT,        textY, "Description", COLOR_TEXT);
        drawRightAlignedText(page, fonts.bold, FONT_SMALL, COL_QTY_RIGHT,        textY, "Qty",        COLOR_TEXT);
        drawRightAlignedText(page, fonts.bold, FONT_SMALL, COL_UNIT_PRICE_RIGHT, textY, "Unit Price", COLOR_TEXT);
        drawRightAlignedText(page, fonts.bold, FONT_SMALL, COL_AMOUNT_RIGHT,     textY, "Amount",     COLOR_TEXT);

        page.y -= TABLE_HEADER_HEIGHT;
    }

    private PageContext writeTotals(PageContext page, FontSet fonts, PDDocument document, Invoice invoice) throws IOException {
        if (page.y < BOTTOM_MARGIN + MIN_TOTALS_SPACE) {
            page.close();
            page = startContinuationPage(document);
        }

        List<InvoiceTaxLine> taxLines  = invoice.getTaxLines();
        java.math.BigDecimal discount  = invoice.getDiscountAmount();
        boolean hasDiscount = discount != null && discount.compareTo(java.math.BigDecimal.ZERO) > 0;

        // data rows: subtotal + tax lines + optional discount
        int dataRows   = 1 + taxLines.size() + (hasDiscount ? 1 : 0);
        final float totalsWidth    = 220f;
        final float topPad         = 18f;
        final float rowSpacing     = 22f;
        final float totalRowHeight = 20f;
        final float sepGap         = 10f;   // gap between last row text and separator
        final float bottomPad      = 12f;
        // (dataRows-1) inter-row gaps + sepGap + totalRowHeight + bottomPad
        float boxHeight = topPad + (dataRows - 1) * rowSpacing + rowSpacing * 0.5f + sepGap + totalRowHeight + bottomPad;

        float totalsX = PAGE_WIDTH - MARGIN - totalsWidth;
        float labelX  = totalsX + 14f;
        float valueX  = PAGE_WIDTH - MARGIN - 10f;
        float boxTop  = page.y;

        page.stream.setNonStrokingColor(COLOR_HEADER_FILL);
        page.stream.addRect(totalsX, boxTop - boxHeight, totalsWidth, boxHeight);
        page.stream.fill();
        strokeRect(page, totalsX, boxTop - boxHeight, totalsWidth, boxHeight);

        float rowY = boxTop - topPad;

        // Subtotal
        drawText(page, fonts.regular, FONT_NORMAL, labelX, rowY, "Subtotal", COLOR_TEXT);
        drawRightAlignedText(page, fonts.regular, FONT_NORMAL, valueX, rowY, formatMoney(invoice, invoice.getSubtotal()), COLOR_TEXT);
        rowY -= rowSpacing;

        // Tax lines
        for (InvoiceTaxLine tl : taxLines) {
            drawText(page, fonts.regular, FONT_NORMAL, labelX, rowY, safe(tl.getLabel()), COLOR_TEXT);
            drawRightAlignedText(page, fonts.regular, FONT_NORMAL, valueX, rowY, formatMoney(invoice, tl.getAmount()), COLOR_TEXT);
            rowY -= rowSpacing;
        }

        // Discount
        if (hasDiscount) {
            drawText(page, fonts.regular, FONT_NORMAL, labelX, rowY, "Discount", COLOR_TEXT);
            drawRightAlignedText(page, fonts.regular, FONT_NORMAL, valueX, rowY, "-" + formatMoney(invoice, discount), COLOR_TEXT);
            rowY -= rowSpacing;
        }

        // Separator — rowY already moved one rowSpacing past last text, step back to close the gap
        float sepY = rowY + rowSpacing - sepGap;
        page.stream.setStrokingColor(COLOR_BORDER);
        page.stream.moveTo(totalsX + 6f, sepY);
        page.stream.lineTo(PAGE_WIDTH - MARGIN - 6f, sepY);
        page.stream.stroke();

        // Total
        float totalRowY = sepY - totalRowHeight;
        drawText(page, fonts.bold, FONT_SECTION, labelX, totalRowY, "Total", COLOR_TEXT);
        drawRightAlignedText(page, fonts.bold, FONT_SECTION, valueX, totalRowY, formatMoney(invoice, invoice.getTotalAmount()), COLOR_TEXT);

        page.y = boxTop - boxHeight - SECTION_GAP;
        return page;
    }

    private void writeFooter(PageContext page, FontSet fonts, Invoice invoice) throws IOException {
        String footerText = invoice.isCancelled()
                ? "This invoice has been cancelled."
                : invoice.isDraft()
                ? "This is a draft invoice and may be subject to change."
                : "Thank you for your business.";

        if (page.y >= BOTTOM_MARGIN + 30f) {
            drawText(page, fonts.italic, FONT_NORMAL, MARGIN, page.y, footerText, COLOR_MUTED);
        }
    }

    private void drawFooterLine(PageContext page, FontSet fonts, Invoice invoice,
                                 int pageNum, int totalPages) throws IOException {
        float footerY = BOTTOM_MARGIN - 10f;
        drawHorizontalRule(page, footerY + 14f, COLOR_BORDER);
        String left   = safe(invoice.getOrganization().getName()) + "  |  " + safe(invoice.getOrganization().getEmail());
        String center = "Page " + pageNum + " of " + totalPages;
        String right  = "Invoice " + invoice.getInvoiceNumber();
        float centerX = PAGE_WIDTH / 2f - textWidth(center, fonts.regular, FONT_SMALL) / 2f;
        drawText(page, fonts.regular, FONT_SMALL, MARGIN,   footerY, left,   COLOR_MUTED);
        drawText(page, fonts.regular, FONT_SMALL, centerX,  footerY, center, COLOR_MUTED);
        drawRightAlignedText(page, fonts.regular, FONT_SMALL, PAGE_WIDTH - MARGIN, footerY, right, COLOR_MUTED);
    }

    // ── Drawing primitives ─────────────────────────────────────────────────────

    private void drawLabelValue(PageContext page, FontSet fonts, float x, float y, String label, String value) throws IOException {
        drawText(page, fonts.bold, FONT_SMALL, x, y, label.toUpperCase(Locale.ROOT), COLOR_MUTED);
        drawText(page, fonts.regular, FONT_NORMAL, x, y - LABEL_VALUE_GAP, value, COLOR_TEXT);
    }

    private void drawHorizontalRule(PageContext page, float y, Color color) throws IOException {
        page.stream.setStrokingColor(color);
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

    // ── Page management ────────────────────────────────────────────────────────

    private PageContext ensurePageSpace(PageContext page, PDDocument document, float requiredHeight) throws IOException {
        if (page.y - requiredHeight >= BOTTOM_MARGIN) {
            return page;
        }
        page.close();
        return startContinuationPage(document);
    }

    private PageContext startPage(PDDocument document) throws IOException {
        PDPage pdfPage = new PDPage(PAGE_SIZE);
        document.addPage(pdfPage);
        PDPageContentStream stream = new PDPageContentStream(document, pdfPage);
        return new PageContext(stream, PAGE_HEIGHT - MARGIN);
    }

    private PageContext startContinuationPage(PDDocument document) throws IOException {
        return startPage(document);
    }

    // ── Text helpers ───────────────────────────────────────────────────────────

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

    // ── Content builders ───────────────────────────────────────────────────────

    private List<String> buildOrganizationLines(Organization org) {
        List<String> lines = new ArrayList<>();
        lines.add(safe(org.getName()));
        if (org.getAddress() != null && !org.getAddress().isBlank()) addAddressLines(lines, org.getAddress());
        if (org.getEmail()   != null && !org.getEmail().isBlank())   lines.add(org.getEmail().trim());
        if (org.getPhone()   != null && !org.getPhone().isBlank())   lines.add(org.getPhone().trim());
        return lines;
    }

    private List<String> buildCustomerLines(Customer customer) {
        List<String> lines = new ArrayList<>();
        if (customer == null) {
            lines.add("No customer assigned");
            return lines;
        }
        lines.add(safe(customer.getName()));
        if (customer.getCompanyName() != null && !customer.getCompanyName().isBlank()) lines.add(customer.getCompanyName().trim());
        if (customer.getAddress()     != null && !customer.getAddress().isBlank())     addAddressLines(lines, customer.getAddress());
        if (customer.getEmail()       != null && !customer.getEmail().isBlank())       lines.add(customer.getEmail().trim());
        if (customer.getPhone()       != null && !customer.getPhone().isBlank())       lines.add(customer.getPhone().trim());
        return lines;
    }

    /** Splits an address on newlines so each line renders as a separate PDF row. */
    private void addAddressLines(List<String> lines, String address) {
        for (String part : address.split("\\r?\\n")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) lines.add(trimmed);
        }
    }

    // ── Text wrapping ──────────────────────────────────────────────────────────

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
            } else {
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

    // ── Formatters ─────────────────────────────────────────────────────────────

    private String formatDate(LocalDate date) {
        return date == null ? "-" : DATE_FORMATTER.format(date);
    }

    /**
     * Formats a monetary amount using the organisation's currency symbol.
     * PDType0Font (NotoSans) is Unicode-capable so all currency symbols — including
     * ₹ (INR), ₩ (KRW), ₺ (TRY), ₦ (NGN), etc. — render without issue.
     */
    private String formatMoney(Invoice invoice, BigDecimal amount) {
        Currency currency = invoice.getOrganization().getCurrency();
        String code = currency != null ? currency.getCurrencyCode() : "USD";
        return CurrencyFormatter.format(amount == null ? BigDecimal.ZERO : amount, code);
    }

    private String safe(String value) {
        if (value == null) return "";
        // PDFBox showText() cannot render control characters (e.g. \n U+000A) —
        // replace any control character with a space so the font never chokes.
        return value.replaceAll("[\\p{Cntrl}]", " ").trim();
    }

    private String sanitizeFileName(String value) {
        return safe(value).trim().replaceAll("[^a-zA-Z0-9\\-_\\.]", "_");
    }

    // ── Inner types ────────────────────────────────────────────────────────────

    /**
     * Holds the three font weights loaded for a single PDF document.
     * PDType0Font instances are document-scoped and cannot be reused across documents.
     */
    private record FontSet(PDFont regular, PDFont bold, PDFont italic) {}

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
