package com.flowcollect.application.dashboard;

import com.flowcollect.api.v1.dashboard.dto.DashboardStatsResponse;
import com.flowcollect.api.v1.dashboard.dto.NeedsAttentionItem;
import com.flowcollect.domain.invoice.Invoice;
import com.flowcollect.domain.invoice.LifeCycleStatus;
import com.flowcollect.domain.invoice.TimeStatus;
import com.flowcollect.domain.invoice.followup.FollowUpStatus;
import com.flowcollect.infrastructure.persistence.invoice.FollowUpJpaRepository;
import com.flowcollect.infrastructure.persistence.invoice.InvoiceJpaRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private static final int NEEDS_ATTENTION_LIMIT = 4;
    private static final int FOLLOW_UP_SILENCE_HOURS = 48;

    private final InvoiceJpaRepository invoiceRepository;
    private final FollowUpJpaRepository followUpRepository;

    public DashboardService(
            InvoiceJpaRepository invoiceRepository,
            FollowUpJpaRepository followUpRepository
    ) {
        this.invoiceRepository = invoiceRepository;
        this.followUpRepository = followUpRepository;
    }

    /**
     * Returns dashboard stats for the given organization.
     * needsAttention: OVERDUE invoices (ISSUED or PARTIALLY_PAID) that have had no SENT
     * follow-up in the last 48 hours, sorted by dueDate ascending (most overdue first), limited to 4.
     */
    public DashboardStatsResponse getStats(UUID organizationId) {
        // Fetch all OVERDUE invoices (ISSUED or PARTIALLY_PAID)
        Specification<Invoice> overdueSpec = (root, query, cb) -> cb.and(
                cb.equal(root.get("organization").get("id"), organizationId),
                cb.equal(root.get("timeStatus"), TimeStatus.OVERDUE),
                root.get("lifeCycleStatus").in(List.of(LifeCycleStatus.ISSUED, LifeCycleStatus.PARTIALLY_PAID))
        );
        List<Invoice> overdueInvoices = invoiceRepository.findAll(overdueSpec);

        // Collect invoice IDs that had a SENT follow-up in the last 48 hours
        Instant cutoff = Instant.now().minus(FOLLOW_UP_SILENCE_HOURS, ChronoUnit.HOURS);
        Set<UUID> recentlySentInvoiceIds = followUpRepository
                .findByStatusAndInvoiceOrganizationId(FollowUpStatus.SENT, organizationId)
                .stream()
                .filter(f -> f.getSentAt() != null && f.getSentAt().isAfter(cutoff))
                .map(f -> f.getInvoice().getId())
                .collect(Collectors.toSet());

        LocalDate today = LocalDate.now();

        List<NeedsAttentionItem> items = overdueInvoices.stream()
                .filter(i -> !recentlySentInvoiceIds.contains(i.getId()))
                .sorted((a, b) -> {
                    // Most overdue first (smallest/oldest dueDate at top)
                    if (a.getDueDate() == null && b.getDueDate() == null) return 0;
                    if (a.getDueDate() == null) return 1;
                    if (b.getDueDate() == null) return -1;
                    return a.getDueDate().compareTo(b.getDueDate());
                })
                .limit(NEEDS_ATTENTION_LIMIT)
                .map(i -> new NeedsAttentionItem(
                        i.getId(),
                        i.getInvoiceNumber(),
                        i.getCustomer() != null ? i.getCustomer().getId() : null,
                        i.getCustomer() != null ? i.getCustomer().getName() : null,
                        i.getDueDate(),
                        i.getRemainingAmount(),
                        i.getDueDate() != null ? (int) ChronoUnit.DAYS.between(i.getDueDate(), today) : 0
                ))
                .toList();

        return new DashboardStatsResponse(items);
    }
}
