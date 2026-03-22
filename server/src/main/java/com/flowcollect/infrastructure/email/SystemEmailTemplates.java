package com.flowcollect.infrastructure.email;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Single source of truth for all system-generated email templates.
 *
 * <p>Every email the application sends automatically (not user-written reminder text)
 * is defined here as a static method. Each method returns an {@link EmailContent}
 * with a pre-rendered subject and HTML body.
 *
 * <h2>To change any email content</h2>
 * Edit only this file. No other class needs to change.
 *
 * <h2>Template catalogue</h2>
 * <ul>
 *   <li>{@link #paymentSubmittedToBusiness} — org notified when customer submits a claim</li>
 *   <li>{@link #paymentConfirmedFull}        — customer notified when full payment approved</li>
 *   <li>{@link #partialPaymentApproved}      — customer notified when partial payment approved</li>
 *   <li>{@link #installmentRequest}          — customer requested to pay remaining balance</li>
 *   <li>{@link #paymentRejected}             — customer notified of rejected claim</li>
 * </ul>
 *
 * <h2>Design rules</h2>
 * <ul>
 *   <li>All user-supplied strings must be passed through {@link #esc} before interpolation.</li>
 *   <li>Return {@code EmailContent(subject, html)} — callers are not aware of internals.</li>
 *   <li>No Spring beans — pure static utility so templates can be tested without a context.</li>
 * </ul>
 */
public final class SystemEmailTemplates {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("d MMM yyyy");

    private SystemEmailTemplates() {}

    // -----------------------------------------------------------------------
    // Business-facing
    // -----------------------------------------------------------------------

    /**
     * Sent to the <strong>organization</strong> when a customer submits a payment claim.
     *
     * @param invoiceNumber     e.g. "INV-1023"
     * @param orgName           organization display name
     * @param customerName      customer display name (may be null — fallback to "A customer")
     * @param currency          ISO currency code, e.g. "INR"
     * @param amountClaimed     amount the customer says they paid
     * @param invoiceRemaining  remaining balance on the invoice before this claim
     * @param customerNote      optional note from customer (may be null)
     * @param appName           application brand name shown in footer
     */
    public static EmailContent paymentSubmittedToBusiness(
            String invoiceNumber,
            String orgName,
            String customerName,
            String currency,
            BigDecimal amountClaimed,
            BigDecimal invoiceRemaining,
            String customerNote,
            String appName
    ) {
        String subject = "Payment Claim Received — Invoice #" + invoiceNumber;

        String displayCustomer = (customerName != null && !customerName.isBlank())
                ? "<strong>" + esc(customerName) + "</strong>"
                : "A customer";

        BigDecimal balanceAfterApproval = invoiceRemaining.subtract(amountClaimed);
        if (balanceAfterApproval.compareTo(BigDecimal.ZERO) < 0) {
            balanceAfterApproval = BigDecimal.ZERO;
        }
        String balanceColor = balanceAfterApproval.compareTo(BigDecimal.ZERO) == 0 ? "#16a34a" : "#374151";

        String customerNoteRow = customerNote != null
                ? row("Customer note", esc(customerNote))
                : "";

        String html = page(
                header(esc(invoiceNumber), "Payment Claim Received", "#111827"),
                "<p style=\"margin:0 0 24px;\">Hi <strong>" + esc(orgName) + " team</strong>,"
                + " " + displayCustomer + " has submitted a payment claim for Invoice #"
                + esc(invoiceNumber) + ". Please log in to review and approve or reject it.</p>"
                + infoBox(
                        row("Customer", displayCustomer)
                        + row("Claimed amount", fmt(amountClaimed, currency))
                        + row("Balance after approval", "<span style=\"font-weight:bold;color:" + balanceColor + ";\">"
                                + fmt(balanceAfterApproval, currency) + "</span>")
                        + customerNoteRow
                  ),
                appName
        );
        return new EmailContent(subject, html);
    }

    // -----------------------------------------------------------------------
    // Customer-facing — approval outcomes
    // -----------------------------------------------------------------------

    /**
     * Sent to the <strong>customer</strong> when their full payment is approved and
     * the invoice is now completely settled.
     *
     * @param invoiceNumber   e.g. "INV-1023"
     * @param customerName    customer display name
     * @param orgName         organization display name
     * @param currency        ISO currency code
     * @param amountConfirmed amount that was confirmed
     * @param businessNote    optional note from business (may be null)
     * @param appName         application brand name shown in footer
     */
    public static EmailContent paymentConfirmedFull(
            String invoiceNumber,
            String customerName,
            String orgName,
            String currency,
            BigDecimal amountConfirmed,
            String businessNote,
            String appName
    ) {
        String subject = "Payment Confirmed — Invoice #" + invoiceNumber;

        String noteBlock = businessNote != null
                ? notePanel(esc(orgName), esc(businessNote), "#f0fdf4", "#16a34a")
                : "";

        String html = page(
                header(esc(invoiceNumber), "Payment Confirmed ✓", "#16a34a"),
                "<p style=\"margin:0 0 16px;\">Hi <strong>" + esc(customerName) + "</strong>,</p>"
                + "<p style=\"margin:0 0 16px;\">Great news! Your payment of <strong>"
                + fmt(amountConfirmed, currency) + "</strong> for <strong>Invoice #"
                + esc(invoiceNumber) + "</strong> has been confirmed by <strong>"
                + esc(orgName) + "</strong>. Thank you — your invoice is now fully settled!</p>"
                + noteBlock,
                appName
        );
        return new EmailContent(subject, html);
    }

    /**
     * Sent to the <strong>customer</strong> when their partial payment claim is approved
     * but a balance still remains on the invoice.
     *
     * @param invoiceNumber    e.g. "INV-1023"
     * @param customerName     customer display name
     * @param orgName          organization display name
     * @param currency         ISO currency code
     * @param amountApproved   partial amount that was confirmed
     * @param remainingBalance balance still outstanding after this approval
     * @param businessNote     optional note from business (may be null)
     * @param appName          application brand name shown in footer
     */
    public static EmailContent partialPaymentApproved(
            String invoiceNumber,
            String customerName,
            String orgName,
            String currency,
            BigDecimal amountApproved,
            BigDecimal remainingBalance,
            String businessNote,
            String appName
    ) {
        String subject = "Partial Payment Received — Invoice #" + invoiceNumber;

        String noteBlock = businessNote != null
                ? notePanel(esc(orgName), esc(businessNote), "#eff6ff", "#2563eb")
                : "";

        String html = page(
                header(esc(invoiceNumber), "Partial Payment Received", "#2563eb"),
                "<p style=\"margin:0 0 16px;\">Hi <strong>" + esc(customerName) + "</strong>,</p>"
                + "<p style=\"margin:0 0 16px;\">We have received and acknowledged your payment of <strong>"
                + fmt(amountApproved, currency) + "</strong> toward <strong>Invoice #"
                + esc(invoiceNumber) + "</strong>. Thank you!</p>"
                + infoBox(
                        row("Amount received", fmt(amountApproved, currency))
                        + row("Remaining balance", "<span style=\"font-weight:bold;color:#dc2626;\">"
                                + fmt(remainingBalance, currency) + "</span>")
                  )
                + "<p style=\"margin:16px 0 0;\">Please contact <strong>" + esc(orgName)
                + "</strong> if you have any questions.</p>"
                + noteBlock,
                appName
        );
        return new EmailContent(subject, html);
    }

    /**
     * Sent to the <strong>customer</strong> when the business explicitly requests the
     * remaining balance after approving a partial payment (the "Request Remaining" action).
     *
     * <p>This is more direct than {@link #partialPaymentApproved}: it acknowledges the
     * partial receipt and explicitly asks for the outstanding amount by the original due date.
     *
     * @param invoiceNumber    e.g. "INV-1023"
     * @param customerName     customer display name
     * @param orgName          organization display name
     * @param currency         ISO currency code
     * @param amountReceived   partial amount that was confirmed
     * @param remainingBalance balance still outstanding
     * @param dueDate          original invoice due date
     * @param appName          application brand name shown in footer
     */
    public static EmailContent installmentRequest(
            String invoiceNumber,
            String customerName,
            String orgName,
            String currency,
            BigDecimal amountReceived,
            BigDecimal remainingBalance,
            LocalDate dueDate,
            String appName
    ) {
        String subject = "Payment Request for Remaining Balance — Invoice #" + invoiceNumber;
        String dueDateStr = dueDate != null ? dueDate.format(DATE_FMT) : "as soon as possible";

        String html = page(
                header(esc(invoiceNumber), "Remaining Balance Due", "#f59e0b"),
                "<p style=\"margin:0 0 16px;\">Hi <strong>" + esc(customerName) + "</strong>,</p>"
                + "<p style=\"margin:0 0 16px;\">Thank you for your payment of <strong>"
                + fmt(amountReceived, currency) + "</strong> toward <strong>Invoice #"
                + esc(invoiceNumber) + "</strong>.</p>"
                + infoBox(
                        row("Amount received", fmt(amountReceived, currency))
                        + row("Remaining balance", "<span style=\"font-weight:bold;color:#dc2626;\">"
                                + fmt(remainingBalance, currency) + "</span>")
                        + row("Due date", esc(dueDateStr))
                  )
                + "<p style=\"margin:16px 0 0;\">We kindly request you to arrange the remaining <strong>"
                + fmt(remainingBalance, currency) + "</strong> by <strong>"
                + esc(dueDateStr) + "</strong>. Please contact <strong>"
                + esc(orgName) + "</strong> if you have any questions.</p>",
                appName
        );
        return new EmailContent(subject, html);
    }

    /**
     * Sent to the <strong>customer</strong> when their payment claim is rejected.
     *
     * @param invoiceNumber    e.g. "INV-1023"
     * @param customerName     customer display name
     * @param orgName          organization display name
     * @param currency         ISO currency code
     * @param remainingBalance current outstanding balance (unchanged after rejection)
     * @param reason           optional rejection reason from business (may be null)
     * @param appName          application brand name shown in footer
     */
    public static EmailContent paymentRejected(
            String invoiceNumber,
            String customerName,
            String orgName,
            String currency,
            BigDecimal remainingBalance,
            String reason,
            String appName
    ) {
        String subject = "Payment Not Verified — Invoice #" + invoiceNumber;

        String reasonBlock = reason != null
                ? notePanel("Reason", esc(reason), "#fef2f2", "#dc2626")
                : "";

        String html = page(
                header(esc(invoiceNumber), "Payment Not Verified", "#dc2626"),
                "<p style=\"margin:0 0 16px;\">Hi <strong>" + esc(customerName) + "</strong>,</p>"
                + "<p style=\"margin:0 0 16px;\">Unfortunately, your payment confirmation for"
                + " <strong>Invoice #" + esc(invoiceNumber) + "</strong> could not be verified at this time.</p>"
                + reasonBlock
                + "<p style=\"margin:16px 0 0;\">The outstanding balance of <strong>"
                + fmt(remainingBalance, currency) + "</strong> remains due."
                + " Please resubmit your confirmation or contact <strong>"
                + esc(orgName) + "</strong> for assistance.</p>",
                appName
        );
        return new EmailContent(subject, html);
    }

    // -----------------------------------------------------------------------
    // Shared layout primitives — edit here to change the look of ALL emails
    // -----------------------------------------------------------------------

    /** Wraps content in the shared page chrome (background, centered card, footer). */
    private static String page(String headerHtml, String bodyHtml, String appName) {
        return "<!DOCTYPE html>"
                + "<html><body style=\"margin:0;padding:0;background:#f4f4f5;font-family:Arial,sans-serif;\">"
                + "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">"
                + "<tr><td align=\"center\" style=\"padding:40px 20px;\">"
                + "<table cellpadding=\"0\" cellspacing=\"0\""
                + " style=\"width:100%;max-width:560px;background:#ffffff;"
                + "border-radius:8px;padding:36px;box-shadow:0 1px 4px rgba(0,0,0,.08);\">"
                + "<tr><td style=\"font-size:15px;line-height:1.7;color:#374151;\">"
                + headerHtml
                + bodyHtml
                + "<hr style=\"border:none;border-top:1px solid #e5e7eb;margin:24px 0;\">"
                + "<p style=\"color:#9ca3af;font-size:12px;margin:0;\">— " + esc(appName) + "</p>"
                + "</td></tr></table>"
                + "</td></tr></table>"
                + "</body></html>";
    }

    /** Two-line header: muted invoice number label + bold colored title. */
    private static String header(String invoiceNumber, String title, String titleColor) {
        return "<p style=\"margin:0 0 6px;font-size:13px;color:#6b7280;\">Invoice #" + invoiceNumber + "</p>"
                + "<p style=\"margin:0 0 20px;font-size:18px;font-weight:bold;color:" + titleColor + ";\">"
                + title + "</p>";
    }

    /** Light gray box with key/value rows — used for amounts, dates, balances. */
    private static String infoBox(String rows) {
        return "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\""
                + " style=\"background:#f9fafb;border-radius:6px;padding:16px 20px;margin-bottom:24px;\">"
                + rows
                + "</table>";
    }

    /** Single key/value row inside an info box. */
    private static String row(String label, String value) {
        return "<tr>"
                + "<td style=\"padding:8px 0;color:#6b7280;font-size:13px;\"><strong>" + label + "</strong></td>"
                + "<td style=\"padding:8px 0;color:#374151;font-size:13px;\">" + value + "</td>"
                + "</tr>";
    }

    /** Colored left-border note panel (used for business notes and rejection reasons). */
    private static String notePanel(String label, String text, String bgColor, String borderColor) {
        return "<p style=\"background:" + bgColor + ";border-left:4px solid " + borderColor + ";"
                + "padding:12px 16px;border-radius:4px;color:#374151;margin:20px 0;font-size:14px;\">"
                + "<strong>" + label + ":</strong><br>"
                + text + "</p>";
    }

    /** Formats a BigDecimal amount with its currency code, e.g. "45,000.00 INR". */
    private static String fmt(BigDecimal amount, String currency) {
        return esc(amount.toPlainString()) + " " + esc(currency);
    }

    /** Minimal HTML escaping for all user-supplied strings. */
    private static String esc(String input) {
        if (input == null) return "";
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
