package com.flowcollect.api.v1.ai.dto;

import com.flowcollect.domain.invoice.LifeCycleStatus;
import com.flowcollect.domain.invoice.TimeStatus;
import com.flowcollect.domain.invoice.followup.FollowUpChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Flexible insight request.
 *
 * The frontend sends a natural-language question plus any optional filters
 * to scope the underlying data. Unset filters mean "all data".
 *
 * Examples:
 *   { "question": "Which customers should I chase this week?" }
 *   { "question": "Is SMS or email more effective for me?", "channel": "SMS" }
 *   { "question": "How is this customer doing?", "customerId": "uuid" }
 *   { "question": "Summarise my overdue situation", "lifeCycleStatus": "ISSUED", "timeStatus": "OVERDUE" }
 *   { "question": "How much did I collect in Q1?", "fromDate": "2026-01-01", "toDate": "2026-03-31" }
 */
public class AskInsightRequest {

    @NotBlank(message = "question is required")
    @Size(max = 500, message = "question must be 500 characters or fewer")
    private String question;

    // --- Optional scoping filters ---

    /** Narrow to a specific customer. */
    private UUID customerId;

    /** Narrow invoices by lifecycle status (e.g. ISSUED, PARTIALLY_PAID, PAID). */
    private LifeCycleStatus lifeCycleStatus;

    /** Narrow invoices by time status (e.g. OVERDUE, DUE_TODAY, NOT_DUE). */
    private TimeStatus timeStatus;

    /** Narrow follow-up data by channel (e.g. EMAIL, SMS, WHATSAPP). */
    private FollowUpChannel channel;

    /** Include invoices issued on or after this date. */
    private LocalDate fromDate;

    /** Include invoices issued on or before this date. */
    private LocalDate toDate;

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }

    public LifeCycleStatus getLifeCycleStatus() { return lifeCycleStatus; }
    public void setLifeCycleStatus(LifeCycleStatus lifeCycleStatus) { this.lifeCycleStatus = lifeCycleStatus; }

    public TimeStatus getTimeStatus() { return timeStatus; }
    public void setTimeStatus(TimeStatus timeStatus) { this.timeStatus = timeStatus; }

    public FollowUpChannel getChannel() { return channel; }
    public void setChannel(FollowUpChannel channel) { this.channel = channel; }

    public LocalDate getFromDate() { return fromDate; }
    public void setFromDate(LocalDate fromDate) { this.fromDate = fromDate; }

    public LocalDate getToDate() { return toDate; }
    public void setToDate(LocalDate toDate) { this.toDate = toDate; }
}
