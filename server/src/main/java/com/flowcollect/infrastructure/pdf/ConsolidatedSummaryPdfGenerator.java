package com.flowcollect.infrastructure.pdf;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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
import com.flowcollect.domain.organization.Organization;
import com.flowcollect.exception.http.InternalException;

/**
 * Generates a consolidated "Statement of Account" PDF covering multiple invoices
 * for a single customer. Used as an attachment in consolidated follow-up emails.
 */
@Component
public class ConsolidatedSummaryPdfGenerator {

    // ── Page geometry ──────────────────────────────────────────────────────────
    private static final PDRectangle PAGE_SIZE     = PDRectangle.A4;
    private static final float       PAGE_WIDTH    = PAGE_SIZE.getWidth();
    private static final float       PAGE_HEIGHT   = PAGE_SIZE.getHeight();
    private static final float       MARGIN        = 50f;
    private static final float       CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2);
    private static final float       BOTTOM_MARGIN = 55f;

    // ── Spacing ────────────────────────────────────────────────────────────────
    private static final float SECTION_GAP    = 28f;
    private static final float LINE_GAP       = 16f;
    private static final float TABLE_ROW_H    = 26f;
    private static final float TABLE_HEADER_H = 26f;
    private static final float MIN_TOTALS_SPACE = 150f;

    // ── Font sizes ─────────────────────────────────────────────────────────────
    private static final float FONT_SMALL   = 8.5f;
    private static final float FONT_NORMAL  = 10f;
    private static final float FONT_SECTION = 11.5f;
    private static final float FONT_TITLE   = 20f;

    // ── Colours — identical palette to InvoicePdfGenerator ────────────────────
    private static final Color COLOR_TEXT        = new Color(34,  34,  34);
    private static final Color COLOR_MUTED       = new Color(110, 110, 110);
    private static final Color COLOR_BORDER      = new Color(214, 220, 229);
    private static final Color COLOR_DIVIDER     = new Color(230, 233, 238);
    private static final Color COLOR_HEADER_FILL = new Color(245, 247, 250);
    private static final Color COLOR_ACCENT      = new Color(36,  99,  235);

    // ── Table columns: Invoice # | Due Date | Amount | Paid | Balance ─────────
    // Dropped ISSUE DATE — due date is what matters to the customer
    private static final float COL_INV_LEFT    = MARGIN + 8f;
    private static final float COL_DUE_RIGHT   = MARGIN + CONTENT_WIDTH * 0.40f;
    private static final float COL_AMOUNT_RIGHT = MARGIN + CONTENT_WIDTH * 0.60f;
    private static final float COL_PAID_RIGHT  = MARGIN + CONTENT_WIDTH * 0.78f;
    private static final float COL_BAL_RIGHT   = PAGE_WIDTH - MARGIN - 8f;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private static final String FONT_REGULAR = "/fonts/NotoSans-Regular.ttf";
    private static final String FONT_BOLD    = "/fonts/NotoSans-Bold.ttf";
    private static final String FONT_ITALIC  = "/fonts/NotoSans-Italic.ttf";

    // ── Public API ─────────────────────────────────────────────────────────────

    public byte[] generate(List<Invoice> invoices, Customer customer) {
        if (invoices == null || invoices.isEmpty()) {
            throw new InternalException("No invoices provided for consolidated summary PDF");
        }

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            FontSet fonts = loadFonts(document);
            PageContext page = startPage(document);

            writeHeader(page, fonts, invoices, customer);
            writeParties(page, fonts, invoices.get(0).getOrganization(), customer);
            page = writeTable(page, fonts, document, invoices);
            page = writeTotals(page, fonts, document, invoices);
            writeFooter(page, fonts, invoices.get(0).getOrganization());
            page.close();

            // Stamp footer bar on every page
            int totalPages = document.getNumberOfPages();
            for (int i = 0; i < totalPages; i++) {
                try (PDPageContentStream cs = new PDPageContentStream(
                        document, document.getPage(i),
                        PDPageContentStream.AppendMode.APPEND, true, true)) {
                    drawFooterLine(new PageContext(cs, 0), fonts,
                            invoices.get(0).getOrganization(), i + 1, totalPages);
                }
            }

            document.save(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new InternalException("Failed to generate consolidated summary PDF: " + ex.getMessage());
        }
    }

    public String buildFileName(Customer customer) {
        String name = customer != null && customer.getName() != null
                ? customer.getName().trim().replaceAll("[^a-zA-Z0-9\\-_]", "_")
                : "client";
        return "invoice-summary-" + name + ".pdf";
    }

    // ── Section writers ────────────────────────────────────────────────────────

    private void writeHeader(PageContext page, FontSet fonts,
                              List<Invoice> invoices, Customer customer) throws IOException {
        Organization org = invoices.get(0).getOrganization();

        // Org name (accent, top left)
        drawText(page, fonts.bold, FONT_TITLE, MARGIN, page.y, safe(org.getName()), COLOR_ACCENT);

        // "STATEMENT OF ACCOUNT" (top right)
        drawRightAlignedText(page, fonts.bold, 13f,
                PAGE_WIDTH - MARGIN, PAGE_HEIGHT - MARGIN,
                "INVOICE SUMMARY", COLOR_TEXT);
        drawRightAlignedText(page, fonts.regular, FONT_SMALL,
                PAGE_WIDTH - MARGIN, PAGE_HEIGHT - MARGIN - 18f,
                "As of: " + DATE_FMT.format(LocalDate.now()), COLOR_MUTED);

        page.y -= 26f;

        // Org sub-info — skip name, it's already shown as the large title above
        for (String line : buildOrgSubLines(org)) {
            drawText(page, fonts.regular, FONT_NORMAL, MARGIN, page.y, line, COLOR_MUTED);
            page.y -= LINE_GAP;
        }

        page.y -= 6f;
        drawHRule(page, page.y, COLOR_BORDER);
        page.y -= SECTION_GAP;
    }

    private void writeParties(PageContext page, FontSet fonts,
                               Organization org, Customer customer) throws IOException {
        float leftX  = MARGIN;
        float rightX = MARGIN + CONTENT_WIDTH / 2f;
        float titleY = page.y;

        drawText(page, fonts.bold, FONT_SECTION, leftX,  titleY, "From",    COLOR_TEXT);
        drawText(page, fonts.bold, FONT_SECTION, rightX, titleY, "Bill To", COLOR_TEXT);

        float leftY  = titleY - 18f;
        float rightY = titleY - 18f;

        for (String line : buildOrgLines(org)) {
            drawText(page, fonts.regular, FONT_NORMAL, leftX, leftY, line, COLOR_TEXT);
            leftY -= LINE_GAP;
        }
        for (String line : buildCustomerLines(customer)) {
            drawText(page, fonts.regular, FONT_NORMAL, rightX, rightY, line, COLOR_TEXT);
            rightY -= LINE_GAP;
        }

        page.y = Math.min(leftY, rightY) - SECTION_GAP;
    }

    private PageContext writeTable(PageContext page, FontSet fonts,
                                   PDDocument document, List<Invoice> invoices) throws IOException {
        page = ensureSpace(page, document, TABLE_HEADER_H + TABLE_ROW_H);
        writeTableHeader(page, fonts);

        String currencyCode = resolveCurrencyCode(invoices.get(0).getOrganization());

        for (Invoice inv : invoices) {
            if (page.y - TABLE_ROW_H < BOTTOM_MARGIN + MIN_TOTALS_SPACE) {
                page.close();
                page = startContinuationPage(document);
                writeTableHeader(page, fonts);
            }

            drawRowBorder(page, page.y, TABLE_ROW_H);

            float textY = page.y - TABLE_ROW_H / 2f - FONT_NORMAL / 2f;

            drawText(page, fonts.regular, FONT_NORMAL,
                    COL_INV_LEFT, textY, safe(inv.getInvoiceNumber()), COLOR_TEXT);
            drawRightAlignedText(page, fonts.regular, FONT_SMALL,
                    COL_DUE_RIGHT, textY, fmtDate(inv.getDueDate()), COLOR_MUTED);
            drawRightAlignedText(page, fonts.regular, FONT_NORMAL,
                    COL_AMOUNT_RIGHT, textY, fmtMoney(inv.getTotalAmount(), currencyCode), COLOR_TEXT);
            drawRightAlignedText(page, fonts.regular, FONT_NORMAL,
                    COL_PAID_RIGHT, textY, fmtMoney(inv.getTotalPaid(), currencyCode), COLOR_MUTED);
            drawRightAlignedText(page, fonts.bold, FONT_NORMAL,
                    COL_BAL_RIGHT, textY, fmtMoney(inv.getRemainingAmount(), currencyCode), COLOR_TEXT);

            page.y -= TABLE_ROW_H;
        }

        page.y -= SECTION_GAP;  // gap between table and totals box — mirrors InvoicePdfGenerator
        return page;
    }

    private void writeTableHeader(PageContext page, FontSet fonts) throws IOException {
        page.stream.setNonStrokingColor(COLOR_HEADER_FILL);
        page.stream.addRect(MARGIN, page.y - TABLE_HEADER_H, CONTENT_WIDTH, TABLE_HEADER_H);
        page.stream.fill();
        drawRowBorder(page, page.y, TABLE_HEADER_H);

        float y = page.y - TABLE_HEADER_H / 2f - FONT_SMALL / 2f;
        drawText(page, fonts.bold, FONT_SMALL, COL_INV_LEFT,      y, "INVOICE #",   COLOR_TEXT);
        drawRightAlignedText(page, fonts.bold, FONT_SMALL, COL_DUE_RIGHT,    y, "DUE DATE",    COLOR_TEXT);
        drawRightAlignedText(page, fonts.bold, FONT_SMALL, COL_AMOUNT_RIGHT, y, "AMOUNT",      COLOR_TEXT);
        drawRightAlignedText(page, fonts.bold, FONT_SMALL, COL_PAID_RIGHT,   y, "PAID",        COLOR_TEXT);
        drawRightAlignedText(page, fonts.bold, FONT_SMALL, COL_BAL_RIGHT,    y, "BALANCE",     COLOR_TEXT);

        page.y -= TABLE_HEADER_H;
    }

    /**
     * Draws the totals summary box — mirrors InvoicePdfGenerator.writeTotals() exactly:
     * right-aligned box with Total Invoiced / Total Paid / separator / Outstanding Balance.
     */
    private PageContext writeTotals(PageContext page, FontSet fonts,
                                    PDDocument document, List<Invoice> invoices) throws IOException {
        if (page.y < BOTTOM_MARGIN + MIN_TOTALS_SPACE) {
            page.close();
            page = startContinuationPage(document);
        }

        String currencyCode = resolveCurrencyCode(invoices.get(0).getOrganization());
        BigDecimal totalAmount    = invoices.stream().map(Invoice::getTotalAmount)
                .filter(v -> v != null).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPaid      = invoices.stream().map(Invoice::getTotalPaid)
                .filter(v -> v != null).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRemaining = invoices.stream().map(Invoice::getRemainingAmount)
                .filter(v -> v != null).reduce(BigDecimal.ZERO, BigDecimal::add);

        final float totalsWidth = 260f;
        final float boxHeight   = 88f;
        final float topPad      = 18f;
        final float rowSpacing  = 22f;

        float totalsX = PAGE_WIDTH - MARGIN - totalsWidth;
        float labelX  = totalsX + 14f;
        float valueX  = PAGE_WIDTH - MARGIN - 10f;
        float boxTop  = page.y;

        // Fill + border — identical to InvoicePdfGenerator
        page.stream.setNonStrokingColor(COLOR_HEADER_FILL);
        page.stream.addRect(totalsX, boxTop - boxHeight, totalsWidth, boxHeight);
        page.stream.fill();
        strokeRect(page, totalsX, boxTop - boxHeight, totalsWidth, boxHeight);

        float row1Y = boxTop - topPad;
        drawText(page, fonts.regular, FONT_NORMAL, labelX, row1Y, "Total Invoiced", COLOR_TEXT);
        drawRightAlignedText(page, fonts.regular, FONT_NORMAL, valueX, row1Y,
                fmtMoney(totalAmount, currencyCode), COLOR_TEXT);

        float row2Y = row1Y - rowSpacing;
        drawText(page, fonts.regular, FONT_NORMAL, labelX, row2Y, "Total Paid", COLOR_TEXT);
        drawRightAlignedText(page, fonts.regular, FONT_NORMAL, valueX, row2Y,
                fmtMoney(totalPaid, currencyCode), COLOR_TEXT);

        // Separator line — same as InvoicePdfGenerator
        float sepY = row2Y - rowSpacing * 0.6f;
        page.stream.setStrokingColor(COLOR_DIVIDER);
        page.stream.moveTo(totalsX + 6f, sepY);
        page.stream.lineTo(PAGE_WIDTH - MARGIN - 6f, sepY);
        page.stream.stroke();

        float row3Y = sepY - rowSpacing * 0.8f;
        drawText(page, fonts.bold, FONT_SECTION, labelX, row3Y, "Outstanding Balance", COLOR_TEXT);
        drawRightAlignedText(page, fonts.bold, FONT_SECTION, valueX, row3Y,
                fmtMoney(totalRemaining, currencyCode), COLOR_TEXT);

        page.y = boxTop - boxHeight - SECTION_GAP;
        return page;
    }

    private void writeFooter(PageContext page, FontSet fonts, Organization org) throws IOException {
        if (page.y >= BOTTOM_MARGIN + 30f) {
            drawText(page, fonts.italic, FONT_NORMAL, MARGIN, page.y,
                    "Please contact " + safe(org.getEmail()) + " if you have any questions.",
                    COLOR_MUTED);
        }
    }

    private void drawFooterLine(PageContext page, FontSet fonts, Organization org,
                                 int pageNum, int totalPages) throws IOException {
        float footerY = BOTTOM_MARGIN - 10f;
        drawHRule(page, footerY + 14f, COLOR_BORDER);
        String left   = safe(org.getName()) + "  |  " + safe(org.getEmail());
        String center = "Page " + pageNum + " of " + totalPages;
        String right  = "Invoice Summary";
        float  cx     = PAGE_WIDTH / 2f - textWidth(center, fonts.regular, FONT_SMALL) / 2f;
        drawText(page, fonts.regular, FONT_SMALL, MARGIN,  footerY, left,   COLOR_MUTED);
        drawText(page, fonts.regular, FONT_SMALL, cx,      footerY, center, COLOR_MUTED);
        drawRightAlignedText(page, fonts.regular, FONT_SMALL, PAGE_WIDTH - MARGIN, footerY, right, COLOR_MUTED);
    }

    // ── Drawing primitives ─────────────────────────────────────────────────────

    private void drawHRule(PageContext page, float y, Color color) throws IOException {
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

    private void drawText(PageContext page, PDFont font, float size,
                           float x, float y, String text, Color color) throws IOException {
        page.stream.beginText();
        page.stream.setFont(font, size);
        page.stream.setNonStrokingColor(color);
        page.stream.newLineAtOffset(x, y);
        page.stream.showText(safe(text));
        page.stream.endText();
    }

    private void drawRightAlignedText(PageContext page, PDFont font, float size,
                                       float rightX, float y, String text, Color color) throws IOException {
        String s = safe(text);
        drawText(page, font, size, rightX - textWidth(s, font, size), y, s, color);
    }

    // ── Page management ────────────────────────────────────────────────────────

    private PageContext ensureSpace(PageContext page, PDDocument document,
                                    float needed) throws IOException {
        if (page.y - needed >= BOTTOM_MARGIN) return page;
        page.close();
        return startContinuationPage(document);
    }

    private PageContext startPage(PDDocument document) throws IOException {
        PDPage p = new PDPage(PAGE_SIZE);
        document.addPage(p);
        return new PageContext(new PDPageContentStream(document, p), PAGE_HEIGHT - MARGIN);
    }

    private PageContext startContinuationPage(PDDocument document) throws IOException {
        return startPage(document);
    }

    // ── Font loading ───────────────────────────────────────────────────────────

    private FontSet loadFonts(PDDocument document) throws IOException {
        return new FontSet(
                loadFont(document, FONT_REGULAR),
                loadFont(document, FONT_BOLD),
                loadFont(document, FONT_ITALIC));
    }

    private PDType0Font loadFont(PDDocument document, String path) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) throw new InternalException("Font not found: " + path);
            return PDType0Font.load(document, is, true);
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /** Full org lines including name — used in the From billing section. */
    private List<String> buildOrgLines(Organization org) {
        List<String> l = new ArrayList<>();
        l.add(safe(org.getName()));
        if (org.getAddress() != null && !org.getAddress().isBlank()) l.add(org.getAddress().trim());
        if (org.getEmail()   != null && !org.getEmail().isBlank())   l.add(org.getEmail().trim());
        if (org.getPhone()   != null && !org.getPhone().isBlank())   l.add(org.getPhone().trim());
        return l;
    }

    /** Sub-lines only (no name) — used under the large title in the header to avoid duplication. */
    private List<String> buildOrgSubLines(Organization org) {
        List<String> l = new ArrayList<>();
        if (org.getAddress() != null && !org.getAddress().isBlank()) l.add(org.getAddress().trim());
        if (org.getEmail()   != null && !org.getEmail().isBlank())   l.add(org.getEmail().trim());
        if (org.getPhone()   != null && !org.getPhone().isBlank())   l.add(org.getPhone().trim());
        return l;
    }

    private List<String> buildCustomerLines(Customer customer) {
        List<String> l = new ArrayList<>();
        if (customer == null) { l.add("Unknown"); return l; }
        l.add(safe(customer.getName()));
        if (customer.getCompanyName() != null && !customer.getCompanyName().isBlank()) l.add(customer.getCompanyName().trim());
        if (customer.getAddress()     != null && !customer.getAddress().isBlank())     l.add(customer.getAddress().trim());
        if (customer.getEmail()       != null && !customer.getEmail().isBlank())       l.add(customer.getEmail().trim());
        if (customer.getPhone()       != null && !customer.getPhone().isBlank())       l.add(customer.getPhone().trim());
        return l;
    }

    private String resolveCurrencyCode(Organization org) {
        return org.getCurrency() != null ? org.getCurrency().getCurrencyCode() : "USD";
    }

    private String fmtMoney(BigDecimal amount, String currencyCode) {
        String result = CurrencyFormatter.format(amount == null ? BigDecimal.ZERO : amount, currencyCode);
        return result.isBlank() ? "-" : result;
    }

    private String fmtDate(LocalDate date) {
        return date == null ? "-" : DATE_FMT.format(date);
    }

    private float textWidth(String text, PDFont font, float size) throws IOException {
        return font.getStringWidth(safe(text)) / 1000f * size;
    }

    private String safe(String v) { return v == null ? "" : v; }

    // ── Inner types ────────────────────────────────────────────────────────────

    private record FontSet(PDFont regular, PDFont bold, PDFont italic) {}

    private static final class PageContext {
        final PDPageContentStream stream;
        float y;
        PageContext(PDPageContentStream stream, float y) { this.stream = stream; this.y = y; }
        void close() throws IOException { stream.close(); }
    }
}
