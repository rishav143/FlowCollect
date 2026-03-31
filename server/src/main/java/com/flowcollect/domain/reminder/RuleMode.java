package com.flowcollect.domain.reminder;

/**
 * Execution mode for a reminder rule or template.
 *
 * AUTO  — processed automatically by the recovery engine when autoRecoveryEnabled is on.
 * MANUAL — shown in the UI for user-initiated follow-ups only; engine ignores these.
 */
public enum RuleMode {
    AUTO,
    MANUAL
}
