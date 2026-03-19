package com.flowcollect.application.template;

import java.util.LinkedHashMap;
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
        Map<String, String> values = new LinkedHashMap<>();
        values.put("customerName", safe(customer.getName()));
        values.put("companyName", safe(customer.getCompanyName()));
        values.put("invoiceNumber", safe(invoice.getInvoiceNumber()));
        values.put("issueDate", safe(invoice.getIssueDate()));
        values.put("dueDate", safe(invoice.getDueDate()));
        values.put("totalAmount", safe(invoice.getTotalAmount()));
        values.put("totalPaid", safe(invoice.getTotalPaid()));
        values.put("remainingAmount", safe(invoice.getRemainingAmount()));
        values.put("organizationName", safe(invoice.getOrganization().getName()));
        values.put("organizationEmail", safe(invoice.getOrganization().getEmail()));
        values.put("currency", invoice.getOrganization().getCurrency() != null
                ? invoice.getOrganization().getCurrency().getCurrencyCode() : "");
        values.put("paymentLink", safe(paymentLinkUrl));
        values.put("confirmationLink", safe(confirmationLinkUrl));

        String rendered = raw == null ? "" : raw;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return rendered;
    }

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
