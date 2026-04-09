package com.flowcollect.application.organization;

import com.flowcollect.api.v1.organization.dto.OrgPaymentDetailsRequest;
import com.flowcollect.api.v1.organization.dto.OrgPaymentDetailsResponse;
import com.flowcollect.domain.organization.OrgPaymentDetails;
import com.flowcollect.infrastructure.persistence.organization.OrgPaymentDetailsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class OrgPaymentDetailsService {

    private final OrgPaymentDetailsRepository repository;

    public OrgPaymentDetailsService(OrgPaymentDetailsRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public OrgPaymentDetailsResponse getByOrgId(UUID orgId) {
        return repository.findByOrgId(orgId)
                .map(OrgPaymentDetailsService::toResponse)
                .orElse(null);
    }

    @Transactional
    public OrgPaymentDetailsResponse saveForOrg(UUID orgId, OrgPaymentDetailsRequest req) {
        OrgPaymentDetails d = repository.findByOrgId(orgId)
                .orElseGet(() -> new OrgPaymentDetails(orgId));

        d.setBankName(trim(req.getBankName()));
        d.setAccountHolderName(trim(req.getAccountHolderName()));
        d.setAccountNumber(trim(req.getAccountNumber()));
        d.setIban(upper(trim(req.getIban())));
        d.setSwiftCode(upper(trim(req.getSwiftCode())));
        d.setRoutingNumber(trim(req.getRoutingNumber()));
        d.setIfscCode(upper(trim(req.getIfscCode())));
        d.setUpiId(trim(req.getUpiId()));
        d.setPaypalEmail(trim(req.getPaypalEmail()));
        d.setWiseEmail(trim(req.getWiseEmail()));
        d.setAdditionalNote(trim(req.getAdditionalNote()));

        return toResponse(repository.save(d));
    }

    @Transactional(readOnly = true)
    public Optional<OrgPaymentDetails> findByOrgId(UUID orgId) {
        return repository.findByOrgId(orgId);
    }

    public static OrgPaymentDetailsResponse toResponse(OrgPaymentDetails d) {
        OrgPaymentDetailsResponse r = new OrgPaymentDetailsResponse();
        r.setBankName(d.getBankName());
        r.setAccountHolderName(d.getAccountHolderName());
        r.setAccountNumber(d.getAccountNumber());
        r.setIban(d.getIban());
        r.setSwiftCode(d.getSwiftCode());
        r.setRoutingNumber(d.getRoutingNumber());
        r.setIfscCode(d.getIfscCode());
        r.setUpiId(d.getUpiId());
        r.setPaypalEmail(d.getPaypalEmail());
        r.setWiseEmail(d.getWiseEmail());
        r.setAdditionalNote(d.getAdditionalNote());
        return r;
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String upper(String s) {
        return s == null ? null : s.toUpperCase();
    }
}
