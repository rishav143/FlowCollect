package com.cashclarity.domain.invoice.followup;

import com.cashclarity.domain.invoice.Invoice;
import com.cashclarity.domain.template.Template;
import com.cashclarity.exception.followup.InvalidFollowUpFieldException;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "follow_ups")
public class FollowUp {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /* ======================
       Relationships
       ====================== */

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private Template template;

    /* ======================
       Core Attributes
       ====================== */

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

    /* ======================
       Timing
       ====================== */

    private Instant sentAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /* ======================
       JPA
       ====================== */

    public FollowUp() {
        // JPA only
    }

    /* ======================
       Callbacks
       ====================== */

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    /* ======================
       Getters & Setters
       ====================== */

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

    public Instant getSentAt() {
        return sentAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
    /* ======================
       Domain Behavior
       ====================== */

    public void send() {
        this.status = FollowUpStatus.SENT;
        this.sentAt = Instant.now();
    }

    public void fail() {
        if (this.status == FollowUpStatus.SENT) {
            throw new InvalidFollowUpFieldException("follow-up " + this.id + " cannot be failed because it has already been sent");
        }
        this.status = FollowUpStatus.FAILED;
        this.sentAt = null;
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
