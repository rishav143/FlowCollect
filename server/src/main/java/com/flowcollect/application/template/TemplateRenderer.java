package com.flowcollect.application.template;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.flowcollect.domain.customer.Customer;
import com.flowcollect.domain.invoice.Invoice;
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
        return render(template.getSubject(), invoice, customer, null, null);
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
}
