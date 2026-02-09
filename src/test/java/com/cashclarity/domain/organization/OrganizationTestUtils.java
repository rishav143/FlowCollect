package com.cashclarity.domain.organization;

import java.lang.reflect.Field;

/**
 * Utility methods for testing Organization entity.
 * Provides reflection-based helpers since Organization doesn't expose setters for immutable fields.
 */
public final class OrganizationTestUtils {

    private OrganizationTestUtils() {
    }

    /**
     * Sets the ID field on an Organization using reflection.
     * Useful for testing when you need to simulate a persisted entity with a specific ID.
     */
    public static void setId(Organization organization, Long id) {
        try {
            Field idField = Organization.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(organization, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set ID on Organization", e);
        }
    }
}
