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
        return render(template.getSubject(), invoice, customer);
    }

    public String renderBody(Template template, Invoice invoice, Customer customer) {
        return render(template.getBody(), invoice, customer);
    }

    private String render(String raw, Invoice invoice, Customer customer) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("customerName", safe(customer.getName()));
        values.put("companyName", safe(customer.getCompanyName()));
        values.put("invoiceNumber", safe(invoice.getInvoiceNumber()));
        values.put("issueDate", safe(invoice.getIssueDate()));
        values.put("dueDate", safe(invoice.getDueDate()));
        values.put("totalAmount", safe(invoice.getTotalAmount()));
        values.put("organizationName", safe(invoice.getOrganization().getName()));
        values.put("organizationEmail", safe(invoice.getOrganization().getEmail()));

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
