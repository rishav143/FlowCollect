package com.flowcollect.infrastructure.email;

/**
 * Immutable value object carrying the two parts of an outbound email:
 * the subject line and the fully-rendered HTML body.
 *
 * <p>All system email templates return an {@code EmailContent} instance
 * so callers only need to handle one type, regardless of which template
 * was used.
 */
public record EmailContent(String subject, String html) {}
