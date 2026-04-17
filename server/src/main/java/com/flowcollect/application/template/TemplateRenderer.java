package com.flowcollect.application.template;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.flowcollect.infrastructure.util.CurrencyFormatter;

import org.springframework.stereotype.Service;

import com.flowcollect.domain.customer.Customer;
import com.flowcollect.domain.invoice.Invoice;
import com.flowcollect.domain.invoice.followup.FollowUpChannel;
import com.flowcollect.domain.template.Template;

/**
 * Service for rendering templates with invoice and customer context.
 * Improves SRP by moving rendering logic out of ReminderEngine.
 */
@Service
public class TemplateRenderer {

    public String renderSubject(Template template, Invoice invoice, Customer customer) {
        if (template.getSubject() == null || template.getSubject().isBlank()) {
            return "Invoice reminder: " + invoice.getInvoiceNumber();
        }
        return stripNewlines(render(template.getSubject(), invoice, customer, null, null));
    }

    public String renderBody(Template template, Invoice invoice, Customer customer) {
        return render(template.getBody(), invoice, customer, null, null);
    }

    /**
     * Renders the template body with an optional payment link URL.
     * Use this overload when dispatching a follow-up with a payment gateway link.
     * The URL is available in templates via the {@code {{paymentLink}}} placeholder.
     */
    public String renderBody(Template template, Invoice invoice, Customer customer, String paymentLinkUrl) {
        return render(template.getBody(), invoice, customer, paymentLinkUrl, null);
    }

    /**
     * Renders the template body with both a payment link URL and a confirmation link URL.
     *
     * <p>Use this overload from the follow-up dispatch path so that templates can use
     * either {@code {{paymentLink}}} (gateway mode) or {@code {{confirmationLink}}}
     * (confirmation-flow mode) without the renderer needing to know the org's mode.
     * Both placeholders are always substituted; unused ones resolve to an empty string.
     *
     * @param paymentLinkUrl     URL of the gateway payment page, or {@code null}
     * @param confirmationLinkUrl URL of the self-report confirmation page, or {@code null}
     */
    public String renderBody(
            Template template,
            Invoice invoice,
            Customer customer,
            String paymentLinkUrl,
            String confirmationLinkUrl
    ) {
        return render(template.getBody(), invoice, customer, paymentLinkUrl, confirmationLinkUrl);
    }

    /**
     * Renders the subject for a consolidated message covering multiple invoices.
     * Uses the first invoice for invoice-specific placeholders; aggregates totals.
     */
    public String renderConsolidatedSubject(Template template, List<Invoice> invoices, Customer customer) {
        if (invoices == null || invoices.isEmpty()) return "Invoice reminder";
        Invoice first = invoices.get(0);
        if (template.getSubject() == null || template.getSubject().isBlank()) {
            return invoices.size() == 1
                    ? "Invoice reminder: " + first.getInvoiceNumber()
                    : "Invoice reminder: " + invoices.size() + " outstanding invoices";
        }
        return stripNewlines(renderConsolidated(template.getSubject(), invoices, customer, null, null));
    }

    /**
     * Renders the body for a consolidated message covering multiple invoices.
     * Replaces {{invoiceList}} with a formatted summary of all invoices.
     * All single-invoice placeholders use aggregated totals or first-invoice values.
     */
    public String renderConsolidatedBody(Template template, List<Invoice> invoices, Customer customer,
                                         String paymentLinkUrl, String confirmationLinkUrl) {
        return renderConsolidated(template.getBody(), invoices, customer, paymentLinkUrl, confirmationLinkUrl);
    }

    private String renderConsolidated(String raw, List<Invoice> invoices, Customer customer,
                                      String paymentLinkUrl, String confirmationLinkUrl) {
        if (invoices == null || invoices.isEmpty()) return raw == null ? "" : raw;
        Invoice first = invoices.get(0);
        String currencyCode = first.getOrganization().getCurrency() != null
                ? first.getOrganization().getCurrency().getCurrencyCode()
                : "USD";

        // Aggregate remaining amount across all invoices
        BigDecimal totalRemaining = invoices.stream()
                .map(Invoice::getRemainingAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, String> values = new LinkedHashMap<>();
        values.put("customerName",      safe(customer.getName()));
        values.put("companyName",       safe(customer.getCompanyName()));
        values.put("invoiceCount",      String.valueOf(invoices.size()));
        values.put("invoiceCountSuffix", invoices.size() == 1 ? "" : "s");
        // Multi-invoice context: first invoice number, aggregated amounts
        values.put("invoiceNumber",     first.getInvoiceNumber());
        values.put("issueDate",         formatDate(first.getIssueDate()));
        values.put("dueDate",           formatDate(first.getDueDate()));
        values.put("totalAmount",       formatMoney(totalRemaining, currencyCode));
        values.put("remainingAmount",   formatMoney(totalRemaining, currencyCode));
        values.put("totalPaid",         "");
        values.put("organizationName",  safe(first.getOrganization().getName()));
        values.put("organizationEmail", safe(first.getOrganization().getEmail()));
        values.put("currency",          currencyCode);
        values.put("paymentLink",       safe(paymentLinkUrl));
        values.put("confirmationLink",  safe(confirmationLinkUrl));
        values.put("invoiceList",       "");

        String rendered = raw == null ? "" : raw;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }

        return rendered;
    }

    private String render(
            String raw,
            Invoice invoice,
            Customer customer,
            String paymentLinkUrl,
            String confirmationLinkUrl
    ) {
        String currencyCode = invoice.getOrganization().getCurrency() != null
                ? invoice.getOrganization().getCurrency().getCurrencyCode()
                : "USD";

        long daysOverdue = 0;
        try {
            Object rawDue = invoice.getDueDate();
            if (rawDue != null) {
                LocalDate due = rawDue instanceof LocalDate
                        ? (LocalDate) rawDue
                        : LocalDate.parse(String.valueOf(rawDue));
                long diff = ChronoUnit.DAYS.between(due, LocalDate.now());
                daysOverdue = Math.max(0, diff);
            }
        } catch (Exception ignored) {}

        Map<String, String> values = new LinkedHashMap<>();
        values.put("customerName",      safe(customer.getName()));
        values.put("companyName",       safe(customer.getCompanyName()));
        values.put("invoiceNumber",     safe(invoice.getInvoiceNumber()));
        values.put("issueDate",         formatDate(invoice.getIssueDate()));
        values.put("dueDate",           formatDate(invoice.getDueDate()));
        values.put("daysOverdue",       String.valueOf(daysOverdue));
        values.put("totalAmount",       formatMoney(invoice.getTotalAmount(),     currencyCode));
        values.put("totalPaid",         formatMoney(invoice.getTotalPaid(),       currencyCode));
        values.put("remainingAmount",   formatMoney(invoice.getRemainingAmount(), currencyCode));
        values.put("organizationName",  safe(invoice.getOrganization().getName()));
        values.put("organizationEmail", safe(invoice.getOrganization().getEmail()));
        values.put("currency",          currencyCode);
        values.put("paymentLink",       safe(paymentLinkUrl));
        values.put("confirmationLink",  safe(confirmationLinkUrl));

        String rendered = raw == null ? "" : raw;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return rendered;
    }

    private static final java.time.format.DateTimeFormatter DATE_FMT =
            java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy");

    private String formatDate(Object value) {
        if (value == null) return "";
        try {
            LocalDate date = value instanceof LocalDate
                    ? (LocalDate) value
                    : LocalDate.parse(String.valueOf(value));
            return date.format(DATE_FMT);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private String formatMoney(Object value, String currencyCode) {
        if (value == null) return "";
        try {
            BigDecimal amount = new BigDecimal(String.valueOf(value));
            return CurrencyFormatter.format(amount, currencyCode);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    // -----------------------------------------------------------------------
    // Built-in consolidated templates (used when no templateId is provided)
    // -----------------------------------------------------------------------

    private static final String BUILT_IN_EMAIL_SUBJECT =
            "Outstanding invoices from {{organizationName}}";

    private static final String BUILT_IN_EMAIL_BODY =
            "Hi {{customerName}},\n\n" +
            "I hope this note finds you well. I'm writing to let you know that you have " +
            "{{invoiceCount}} outstanding invoice{{invoiceCountSuffix}} with {{organizationName}} " +
            "totalling {{remainingAmount}}.\n\n" +
            "We've attached a full statement for your reference. When you have a moment, " +
            "we'd appreciate you looking it over and settling the balance at your convenience.\n\n" +
            "If you have any questions or would like to discuss the details, please feel free " +
            "to reach out to us directly at {{organizationEmail}}. We're always happy to help.\n\n" +
            "Thank you for your time and continued partnership.\n\n" +
            "Best regards,\n" +
            "{{organizationName}}\n" +
            "{{organizationEmail}}";

    private static final String BUILT_IN_SMS_WHATSAPP_BODY =
            "Hi {{customerName}}, you have {{invoiceCount}} outstanding " +
            "invoice{{invoiceCountSuffix}} with {{organizationName}} totalling " +
            "{{remainingAmount}}. A statement has been attached for your reference. " +
            "Questions? Reach us at {{organizationEmail}}.";

    public String renderBuiltInConsolidatedSubject(FollowUpChannel channel,
                                                    List<Invoice> invoices,
                                                    Customer customer) {
        String raw = channel == FollowUpChannel.EMAIL
                ? BUILT_IN_EMAIL_SUBJECT
                : "";   // SMS/WhatsApp have no subject
        return stripNewlines(renderConsolidated(raw, invoices, customer, null, null));
    }

    public String renderBuiltInConsolidatedBody(FollowUpChannel channel,
                                                 List<Invoice> invoices,
                                                 Customer customer) {
        String raw = channel == FollowUpChannel.EMAIL
                ? BUILT_IN_EMAIL_BODY
                : BUILT_IN_SMS_WHATSAPP_BODY;
        return renderConsolidated(raw, invoices, customer, null, null);
    }

    private String stripNewlines(String value) {
        return value == null ? "" : value.replaceAll("[\\r\\n]+", " ").strip();
    }
}
