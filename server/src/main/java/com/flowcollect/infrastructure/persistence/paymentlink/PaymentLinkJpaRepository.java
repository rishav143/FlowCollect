package com.flowcollect.infrastructure.persistence.paymentlink;

import com.flowcollect.domain.invoice.paymentlink.PaymentLink;
import com.flowcollect.domain.invoice.paymentlink.PaymentLinkStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentLinkJpaRepository extends JpaRepository<PaymentLink, UUID> {

    /** Look up a link by the public token embedded in the redirect URL. */
    Optional<PaymentLink> findByToken(String token);

    /** Look up a link by the gateway-side session/link ID for webhook matching. */
    Optional<PaymentLink> findByGatewaySessionId(String gatewaySessionId);

    /** Used by the expiry scheduler to find all active links past their expiry date. */
    List<PaymentLink> findByStatusInAndExpiresAtBefore(List<PaymentLinkStatus> statuses, Instant now);
}
