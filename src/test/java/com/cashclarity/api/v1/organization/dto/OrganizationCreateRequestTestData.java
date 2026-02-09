package com.cashclarity.api.v1.organization.dto;

/**
 * Test data builder for {@link OrganizationCreateRequest}.
 * Provides factory methods to create valid and invalid request objects for testing.
 */
public final class OrganizationCreateRequestTestData {

    private OrganizationCreateRequestTestData() {
    }

    /**
     * Creates a valid organization create request with all required fields.
     */
    public static OrganizationCreateRequest valid() {
        OrganizationCreateRequest request = new OrganizationCreateRequest();
        request.setName("Acme Corporation");
        request.setEmail("contact@acme.com");
        request.setTimezone("America/New_York");
        request.setCurrency("USD");
        return request;
    }

    /**
     * Creates a valid organization create request with all fields including optional ones.
     */
    public static OrganizationCreateRequest validWithAllFields() {
        OrganizationCreateRequest request = valid();
        request.setPhone("+1-555-0100");
        request.setAddress("123 Main Street, New York, NY 10001");
        return request;
    }

    /**
     * Creates a request with blank name.
     */
    public static OrganizationCreateRequest withBlankName() {
        OrganizationCreateRequest request = valid();
        request.setName("   ");
        return request;
    }

    /**
     * Creates a request with null name.
     */
    public static OrganizationCreateRequest withNullName() {
        OrganizationCreateRequest request = valid();
        request.setName(null);
        return request;
    }

    /**
     * Creates a request with name exceeding max length.
     */
    public static OrganizationCreateRequest withNameTooLong() {
        OrganizationCreateRequest request = valid();
        request.setName("a".repeat(101));
        return request;
    }

    /**
     * Creates a request with invalid email format.
     */
    public static OrganizationCreateRequest withInvalidEmail() {
        OrganizationCreateRequest request = valid();
        request.setEmail("not-an-email");
        return request;
    }

    /**
     * Creates a request with blank email.
     */
    public static OrganizationCreateRequest withBlankEmail() {
        OrganizationCreateRequest request = valid();
        request.setEmail("   ");
        return request;
    }

    /**
     * Creates a request with null email.
     */
    public static OrganizationCreateRequest withNullEmail() {
        OrganizationCreateRequest request = valid();
        request.setEmail(null);
        return request;
    }

    /**
     * Creates a request with invalid timezone.
     */
    public static OrganizationCreateRequest withInvalidTimezone() {
        OrganizationCreateRequest request = valid();
        request.setTimezone("Invalid/Timezone");
        return request;
    }

    /**
     * Creates a request with blank timezone.
     */
    public static OrganizationCreateRequest withBlankTimezone() {
        OrganizationCreateRequest request = valid();
        request.setTimezone("   ");
        return request;
    }

    /**
     * Creates a request with null timezone.
     */
    public static OrganizationCreateRequest withNullTimezone() {
        OrganizationCreateRequest request = valid();
        request.setTimezone(null);
        return request;
    }

    /**
     * Creates a request with invalid currency code.
     */
    public static OrganizationCreateRequest withInvalidCurrency() {
        OrganizationCreateRequest request = valid();
        request.setCurrency("ZZZ"); // ZZZ is not a valid ISO 4217 currency code
        return request;
    }

    /**
     * Creates a request with blank currency.
     */
    public static OrganizationCreateRequest withBlankCurrency() {
        OrganizationCreateRequest request = valid();
        request.setCurrency("   ");
        return request;
    }

    /**
     * Creates a request with null currency.
     */
    public static OrganizationCreateRequest withNullCurrency() {
        OrganizationCreateRequest request = valid();
        request.setCurrency(null);
        return request;
    }

    /**
     * Creates a request with currency code that's not 3 characters.
     */
    public static OrganizationCreateRequest withInvalidCurrencyLength() {
        OrganizationCreateRequest request = valid();
        request.setCurrency("US");
        return request;
    }

    /**
     * Creates a request with phone exceeding max length.
     */
    public static OrganizationCreateRequest withPhoneTooLong() {
        OrganizationCreateRequest request = valid();
        request.setPhone("1".repeat(21));
        return request;
    }

    /**
     * Creates a request with address exceeding max length.
     */
    public static OrganizationCreateRequest withAddressTooLong() {
        OrganizationCreateRequest request = valid();
        request.setAddress("a".repeat(256));
        return request;
    }
}
