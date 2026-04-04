package com.flowcollect.domain.invoice.followup;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.flowcollect.domain.invoice.Invoice;
import com.flowcollect.domain.invoice.paymentlink.PaymentLink;
import com.flowcollect.domain.reminder.ReminderRule;
import com.flowcollect.domain.template.Template;
@Entity
@Table(
        name = "follow_ups",
        uniqueConstraints = {
                // Guarantees each occurrence of a rule fires at most once per invoice.
                // occurrence_index is 0 for non-cyclic rules and 0..maxOccurrences-1 for cyclic ones.
                // reminder_rule_id is NULL for manual follow-ups, so the constraint only applies to automated ones.
                @UniqueConstraint(
                        name = "uk_follow_up_invoice_rule_occurrence",
                        columnNames = {"invoice_id", "reminder_rule_id", "occurrence_index"}
                )
        }
)
public class FollowUp {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Relationships

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private Template template;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reminder_rule_id")
    private ReminderRule reminderRule;

    /**
     * Optional payment link attached to this follow-up.
     * When set, the link's public URL is injected into the message body
     * via the {{paymentLink}} template placeholder.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_link_id")
    private PaymentLink paymentLink;

    @Column(name = "attach_pdf", nullable = false)
    private boolean attachPdf = false;

    // Core Attributes

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FollowUpChannel channel;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FollowUpStatus status;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FollowUpTriggerType triggerType;

    // Timing

    @Column(name = "scheduled_for_date")
    private LocalDate scheduledForDate;

    // Zero-based index of which occurrence within a cyclic rule this follow-up represents.
    // Always 0 for non-cyclic (single-occurrence) rules and for manual follow-ups.
    @Column(name = "occurrence_index", nullable = false)
    private int occurrenceIndex = 0;

    private Instant sentAt;

    /** Resend email ID returned by the Resend API when this follow-up is sent via EMAIL channel. */
    @Column(name = "resend_email_id")
    private String resendEmailId;

    /**
     * The real destination URL (payment or confirmation link) that the tracking
     * redirect endpoint forwards the client to after recording the click.
     * Populated at dispatch time for any follow-up that contains a trackable link.
     */
    @Column(name = "tracked_link_url", length = 2048)
    private String trackedLinkUrl;

    /**
     * Timestamp when the client clicked the tracking link embedded in the message.
     * Works across all channels (EMAIL, SMS, WhatsApp) via our own redirect endpoint.
     */
    @Column(name = "opened_at")
    private Instant openedAt;

    /** Which type of link the client clicked — PAYMENT_LINK or CONFIRMATION_LINK. */
    @Enumerated(EnumType.STRING)
    @Column(name = "clicked_link_type")
    private ClickedLinkType clickedLinkType;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    // JPA

    public FollowUp() {
        // JPA only
    }

    // Callbacks

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters & Setters

    public UUID getId() {
        return id;
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public void setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }

    public FollowUpChannel getChannel() {
        return channel;
    }

    public void setChannel(FollowUpChannel channel) {
        this.channel = channel;
    }

    public FollowUpStatus getStatus() {
        return status;
    }

    public void setStatus(FollowUpStatus status) {
        this.status = status;
    }

    public FollowUpTriggerType getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(FollowUpTriggerType triggerType) {
        this.triggerType = triggerType;
    }

    public Template getTemplate() {
        return template;
    }

    public void setTemplate(Template template) {
        this.template = template;
    }

    public ReminderRule getReminderRule() {
        return reminderRule;
    }

    public void setReminderRule(ReminderRule reminderRule) {
        this.reminderRule = reminderRule;
    }

    public boolean isAttachPdf() {
        return attachPdf;
    }

    public void setAttachPdf(boolean attachPdf) {
        this.attachPdf = attachPdf;
    }

    public PaymentLink getPaymentLink() {
        return paymentLink;
    }

    public void setPaymentLink(PaymentLink paymentLink) {
        this.paymentLink = paymentLink;
    }

    public LocalDate getScheduledForDate() {
        return scheduledForDate;
    }

    public void setScheduledForDate(LocalDate scheduledForDate) {
        this.scheduledForDate = scheduledForDate;
    }

    public int getOccurrenceIndex() {
        return occurrenceIndex;
    }

    public void setOccurrenceIndex(int occurrenceIndex) {
        this.occurrenceIndex = occurrenceIndex;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public String getResendEmailId() {
        return resendEmailId;
    }

    public void setResendEmailId(String resendEmailId) {
        this.resendEmailId = resendEmailId;
    }

    public String getTrackedLinkUrl() {
        return trackedLinkUrl;
    }

    public void setTrackedLinkUrl(String trackedLinkUrl) {
        this.trackedLinkUrl = trackedLinkUrl;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }

    public ClickedLinkType getClickedLinkType() {
        return clickedLinkType;
    }

    public void setClickedLinkType(ClickedLinkType clickedLinkType) {
        this.clickedLinkType = clickedLinkType;
    }

    public void markOpened(Instant at, ClickedLinkType linkType) {
        if (this.openedAt == null) {
            this.openedAt = at;
            this.clickedLinkType = linkType;
        }
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // Domain Behavior

    public void send() {
        this.status = FollowUpStatus.SENT;
        this.sentAt = Instant.now();
    }

    public void fail() {
        if (this.status == FollowUpStatus.SENT) {
            throw new IllegalStateException("Follow-up " + this.id + " cannot be failed because it has already been sent");
        }
        this.status = FollowUpStatus.FAILED;
        this.sentAt = null;
    }

    /**
     * Cancels this follow-up. Used when the invoice is no longer eligible
     * (e.g. paid or voided) before the follow-up has been dispatched.
     * Semantically distinct from FAILED: no delivery was attempted — the
     * follow-up was intentionally skipped because it was no longer needed.
     *
     * @throws IllegalStateException if the follow-up has already been sent.
     */
    public void cancel() {
        if (this.status == FollowUpStatus.SENT) {
            throw new IllegalStateException("Follow-up " + this.id + " cannot be cancelled because it has already been sent");
        }
        this.status = FollowUpStatus.CANCELLED;
        this.sentAt = null;
    }

    public boolean isCancelled() {
        return this.status == FollowUpStatus.CANCELLED;
    }

    public boolean isSent() {
        return this.status == FollowUpStatus.SENT;
    }

    public boolean isFailed() {
        return this.status == FollowUpStatus.FAILED;
    }

    public boolean isPending() {
        return this.status == FollowUpStatus.PENDING;
    }

    public boolean isManual() {
        return this.triggerType == FollowUpTriggerType.MANUAL;
    }

    public boolean isAutomated() {
        return this.triggerType == FollowUpTriggerType.AUTOMATED;
    }
}
