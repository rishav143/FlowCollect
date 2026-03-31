package com.flowcollect.api.v1.organization;

import com.flowcollect.api.v1.organization.dto.OrganizationResponse;
import com.flowcollect.domain.organization.Organization;

public final class OrganizationMapper {

    private OrganizationMapper() {
    }

    public static OrganizationResponse toResponse(Organization organization) {
        if (organization == null) {
            return null;
        }
        OrganizationResponse response = new OrganizationResponse();
        response.setId(organization.getId());
        response.setName(organization.getName());
        response.setEmail(organization.getEmail());
        response.setPhone(organization.getPhone());
        response.setAddress(organization.getAddress());
        response.setTimezone(organization.getTimezone() != null ? organization.getTimezone().getId() : null);
        response.setCurrency(organization.getCurrency() != null ? organization.getCurrency().getCurrencyCode() : null);
        response.setLogoUrl(organization.getLogoUrl());
        response.setStatus(organization.getStatus());
        response.setPaymentCollectionMode(organization.getPaymentCollectionMode());
        response.setAutoRecoveryEnabled(organization.isAutoRecoveryEnabled());
        response.setCreatedAt(organization.getCreatedAt());
        response.setUpdatedAt(organization.getUpdatedAt());
        return response;
    }
}
