package com.flowcollect.api.v1.tracking;

import com.flowcollect.domain.invoice.followup.FollowUp;
import com.flowcollect.infrastructure.persistence.invoice.FollowUpJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Public redirect endpoint for link click tracking.
 *
 * When a follow-up is dispatched, every trackable link (payment or confirmation)
 * is replaced with: {appBaseUrl}/track/{followUpId}
 *
 * When the client clicks the link (regardless of channel — EMAIL, SMS, WhatsApp):
 *   1. We record the click by setting followUp.openedAt (best-effort — never blocks redirect)
 *   2. We redirect the client to the real destination URL (stored in followUp.trackedLinkUrl)
 *
 * This endpoint is excluded from JWT auth via JwtFilter.SKIP_PREFIXES (/track/).
 */
@RestController
public class LinkTrackingController {

    private static final Logger log = LoggerFactory.getLogger(LinkTrackingController.class);

    private final FollowUpJpaRepository followUpRepository;

    public LinkTrackingController(FollowUpJpaRepository followUpRepository) {
        this.followUpRepository = followUpRepository;
    }

    @GetMapping("/track/{followUpId}")
    public ResponseEntity<Void> track(@PathVariable UUID followUpId) {
        Optional<FollowUp> opt = followUpRepository.findById(followUpId);

        if (opt.isEmpty()) {
            log.warn("Track click — no FollowUp found for id={}", followUpId);
            return ResponseEntity.notFound().build();
        }

        FollowUp followUp = opt.get();

        // Record the click — best-effort. A DB hiccup must never block the redirect.
        if (followUp.getOpenedAt() == null) {
            try {
                followUp.markOpened(Instant.now(), followUp.getClickedLinkType());
                followUpRepository.save(followUp);
                log.info("Link clicked — marked FollowUp {} as opened (channel={})",
                        followUp.getId(), followUp.getChannel());
            } catch (Exception e) {
                log.warn("Track click — failed to record open for FollowUp {} (will still redirect)",
                        followUpId, e);
            }
        }

        // Redirect to the real destination
        String destination = followUp.getTrackedLinkUrl();
        if (destination == null || destination.isBlank()) {
            log.warn("Track click — FollowUp {} has no trackedLinkUrl", followUpId);
            return ResponseEntity.notFound().build();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create(destination));
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        } catch (IllegalArgumentException e) {
            log.warn("Track click — FollowUp {} has malformed trackedLinkUrl: {}", followUpId, destination);
            return ResponseEntity.notFound().build();
        }
    }
}
