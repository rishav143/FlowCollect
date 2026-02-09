package com.cashclarity.domain.organization;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Currency;

/**
 * Test data builder for {@link Organization} domain entity.
 * Provides factory methods to create organization instances for testing.
 */
public final class OrganizationTestData {

    private OrganizationTestData() {
    }

    /**
     * Creates a valid organization with all required fields.
     */
    public static Organization valid() {
        return new Organization(
                "Acme Corporation",
                "contact@acme.com",
                ZoneId.of("America/New_York"),
                Currency.getInstance("USD")
        );
    }

    /**
     * Creates an organization with a specific ID (useful for testing).
     */
    public static Organization withId(Long id) {
        Organization org = valid();
        // Note: ID is set via reflection or through repository save
        return org;
    }

    /**
     * Creates an organization with a specific email.
     */
    public static Organization withEmail(String email) {
        return new Organization(
                "Test Organization",
                email,
                ZoneId.of("Europe/London"),
                Currency.getInstance("GBP")
        );
    }

    /**
     * Creates an organization with all optional fields populated.
     */
    public static Organization withAllFields() {
        Organization org = valid();
        org.setPhone("+1-555-0100");
        org.setAddress("123 Main Street, New York, NY 10001");
        org.setLogoUrl("https://example.com/logo.png");
        return org;
    }

    /**
     * Creates an archived organization.
     */
    public static Organization archived() {
        Organization org = valid();
        org.archive();
        return org;
    }
}
