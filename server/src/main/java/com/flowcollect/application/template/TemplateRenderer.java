package com.flowcollect.application.template;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
        values.put("issueDate",         safe(first.getIssueDate()));
        values.put("dueDate",           safe(first.getDueDate()));
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

        Map<String, String> values = new LinkedHashMap<>();
        values.put("customerName",      safe(customer.getName()));
        values.put("companyName",       safe(customer.getCompanyName()));
        values.put("invoiceNumber",     safe(invoice.getInvoiceNumber()));
        values.put("issueDate",         safe(invoice.getIssueDate()));
        values.put("dueDate",           safe(invoice.getDueDate()));
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

    private String formatMoney(Object value, String currencyCode) {
        if (value == null) return "";
        try {
            BigDecimal amount = new BigDecimal(String.valueOf(value));
            Currency currency = Currency.getInstance(currencyCode);
            NumberFormat fmt = NumberFormat.getCurrencyInstance(localeForCurrency(currencyCode));
            fmt.setCurrency(currency);
            fmt.setMinimumFractionDigits(0);
            fmt.setMaximumFractionDigits(0);
            return fmt.format(amount);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private Locale localeForCurrency(String currencyCode) {
        return switch (currencyCode) {
            case "INR" -> Locale.of("en", "IN");
            case "USD" -> Locale.US;
            case "EUR" -> Locale.GERMANY;
            case "GBP" -> Locale.UK;
            case "AUD" -> Locale.of("en", "AU");
            case "SGD" -> Locale.of("en", "SG");
            case "AED" -> Locale.of("ar", "AE");
            default    -> Locale.US;
        };
    }

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    // -----------------------------------------------------------------------
    // Built-in consolidated templates (used when no templateId is provided)
    // -----------------------------------------------------------------------

    private static final String BUILT_IN_EMAIL_SUBJECT =
            "Invoice reminder – {{organizationName}}";

    private static final String BUILT_IN_EMAIL_BODY =
            "Hi {{customerName}},\n\n" +
            "This is a friendly reminder that you have {{invoiceCount}} outstanding " +
            "invoice{{invoiceCountSuffix}} with {{organizationName}} totalling {{remainingAmount}}.\n\n" +
            "We've attached an invoice summary for your reference. We'd appreciate " +
            "payment at your earliest convenience.\n\n" +
            "If you have any questions, please don't hesitate to reach out at {{organizationEmail}}.\n\n" +
            "Kind regards,\n" +
            "{{organizationName}}";

    private static final String BUILT_IN_SMS_WHATSAPP_BODY =
            "Hi {{customerName}}, you have {{invoiceCount}} outstanding " +
            "invoice{{invoiceCountSuffix}} with {{organizationName}} totalling " +
            "{{remainingAmount}}. Please settle at your earliest convenience. " +
            "Questions? Contact us at {{organizationEmail}}.";

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
