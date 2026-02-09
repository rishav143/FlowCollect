package com.cashclarity.api.v1.organization.dto;

/**
 * Test data builder for {@link OrganizationUpdateRequest}.
 */
public final class OrganizationUpdateRequestTestData {

    private OrganizationUpdateRequestTestData() {
    }

    public static OrganizationUpdateRequest valid() {
        OrganizationUpdateRequest request = new OrganizationUpdateRequest();
        request.setName("Updated Org");
        request.setEmail("updated@acme.com");
        request.setTimezone("Europe/London");
        request.setCurrency("GBP");
        request.setPhone("+44-20-0000");
        request.setAddress("221B Baker Street, London");
        request.setLogoUrl("https://example.com/logo.png");
        return request;
    }

    public static OrganizationUpdateRequest nameBlank() {
        OrganizationUpdateRequest request = valid();
        request.setName("   ");
        return request;
    }

    public static OrganizationUpdateRequest emailBlank() {
        OrganizationUpdateRequest request = valid();
        request.setEmail("   ");
        return request;
    }

    public static OrganizationUpdateRequest emailInvalid() {
        OrganizationUpdateRequest request = valid();
        request.setEmail("not-an-email");
        return request;
    }

    public static OrganizationUpdateRequest currencyInvalidLength() {
        OrganizationUpdateRequest request = valid();
        request.setCurrency("US");
        return request;
    }

    public static OrganizationUpdateRequest timezoneInvalid() {
        OrganizationUpdateRequest request = valid();
        request.setTimezone("Invalid/Timezone");
        return request;
    }

    public static OrganizationUpdateRequest currencyInvalid() {
        OrganizationUpdateRequest request = valid();
        request.setCurrency("ZZZ");
        return request;
    }

    public static OrganizationUpdateRequest onlyContactInfo() {
        OrganizationUpdateRequest request = new OrganizationUpdateRequest();
        request.setPhone("+1-555-0000");
        request.setAddress("123 Main Street, NY");
        return request;
    }

    public static OrganizationUpdateRequest clearLogo() {
        OrganizationUpdateRequest request = new OrganizationUpdateRequest();
        request.setLogoUrl("   ");
        return request;
    }
}
