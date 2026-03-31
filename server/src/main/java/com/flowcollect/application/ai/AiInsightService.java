package com.flowcollect.application.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowcollect.api.v1.ai.dto.AskInsightRequest;
import com.flowcollect.application.customer.CustomerService;
import com.flowcollect.application.organization.OrganizationService;
import com.flowcollect.domain.customer.Customer;
import com.flowcollect.domain.invoice.Invoice;
import com.flowcollect.domain.invoice.LifeCycleStatus;
import com.flowcollect.domain.invoice.TimeStatus;
import com.flowcollect.domain.invoice.followup.FollowUp;
import com.flowcollect.domain.invoice.followup.FollowUpChannel;
import com.flowcollect.domain.invoice.followup.FollowUpStatus;
import com.flowcollect.exception.http.InternalException;
import com.flowcollect.infrastructure.ai.OpenAiClient;
import com.flowcollect.infrastructure.persistence.invoice.FollowUpJpaRepository;
import com.flowcollect.infrastructure.persistence.invoice.InvoiceJpaRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Generates AI-powered payment insights for the organization dashboard
 * and per-customer payment intelligence reports.
 *
 * Stats are computed in-memory from invoice data (appropriate for SMB scale),
 * then summarized into a structured prompt for gpt-4o-mini.
 */
@Service
public class AiInsightService {

    private static final int TOP_CUSTOMERS_LIMIT = 5;
    private static final int RECENT_DAYS = 30;

    private static final String INSIGHT_SYSTEM_PROMPT = """
            You are a financial advisor for a small business using FlowCollect to manage invoices and collect payments.
            You receive structured payment data and provide concise, specific, and actionable advice.
            Always respond with valid JSON only — no markdown, no code fences.
            The "insights" field must contain exactly 3 bullet points using "- " prefix, separated by newlines.
            Each bullet must be a single short sentence (under 20 words).
            Use **bold** for key amounts, customer names, and action words to aid readability.
            """;

    private final InvoiceJpaRepository invoiceRepository;
    private final CustomerService customerService;
    private final OrganizationService organizationService;
    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;
    private final FollowUpJpaRepository followUpRepository;

    public AiInsightService(
            InvoiceJpaRepository invoiceRepository,
            CustomerService customerService,
            OrganizationService organizationService,
            OpenAiClient openAiClient,
            ObjectMapper objectMapper,
            FollowUpJpaRepository followUpRepository
    ) {
        this.invoiceRepository = invoiceRepository;
        this.customerService = customerService;
        this.organizationService = organizationService;
        this.openAiClient = openAiClient;
        this.objectMapper = objectMapper;
        this.followUpRepository = followUpRepository;
    }

    /**
     * Flexible insight endpoint. The frontend sends a natural-language question
     * plus any optional filters; the backend resolves the filters to real data,
     * builds a scoped data context, and asks OpenAI the question against that context.
     *
     * @param organizationId scoping guard
     * @param request        question + optional filters
     * @return AI-generated answer as a bullet-point string
     */
    public String ask(UUID organizationId, AskInsightRequest request) {
        var org = organizationService.getById(organizationId);

        // --- Resolve invoices with applied filters ---
        List<Invoice> invoices = fetchFilteredInvoices(organizationId, request);

        // --- Resolve follow-ups scoped to this org (and optionally by channel) ---
        List<FollowUp> followUps = fetchFilteredFollowUps(organizationId, request.getChannel());

        // --- Build scoped data summary ---
        String dataSummary = buildScopedDataSummary(invoices, followUps, request, org.getCurrency());

        // --- Ask AI the question in context ---
        String userPrompt = """
                You have access to the following payment data for a business:

                %s

                Question from the business owner: %s

                Answer with exactly 3 specific, actionable bullet points based only on the data above.
                Each bullet must be a single short sentence (under 20 words).
                Use **bold** for key amounts, customer names, and action words.
                Each bullet on its own line starting with "- ".
                If the data is insufficient to answer confidently, say so in one bullet.

                Respond with JSON: {"insights": "<bullet points separated by \\n>"}
                """.formatted(dataSummary, request.getQuestion());

        String rawJson = openAiClient.chat(INSIGHT_SYSTEM_PROMPT, userPrompt);
        return parseInsights(rawJson);
    }

    /**
     * Generates a payment health overview for the organization.
     * Covers: overdue invoices, collection priorities, recent receipts, and cash flow signals.
     *
     * @param organizationId the org to analyze
     * @return AI-generated actionable insights as a bullet-point string
     */
    public String generateOverviewInsights(UUID organizationId) {
        var org = organizationService.getById(organizationId);

        List<Invoice> invoices = fetchActiveInvoices(organizationId);

        OrgStats stats = computeOrgStats(invoices);
        String prompt = buildOverviewPrompt(stats, org.getCurrency());
        String rawJson = openAiClient.chat(INSIGHT_SYSTEM_PROMPT, prompt);
        return parseInsights(rawJson);
    }

    /**
     * Generates a payment behavior profile and recommended next actions for a specific customer.
     *
     * @param organizationId scoping guard
     * @param customerId     the customer to profile
     * @return AI-generated customer intelligence as a bullet-point string
     */
    public String generateCustomerInsights(UUID organizationId, UUID customerId) {
        var org = organizationService.getById(organizationId);
        Customer customer = customerService.getCustomerById(organizationId, customerId);

        List<Invoice> invoices = fetchCustomerInvoices(organizationId, customerId);

        CustomerStats stats = computeCustomerStats(invoices, customer);
        String prompt = buildCustomerPrompt(stats, customer, org.getCurrency());
        String rawJson = openAiClient.chat(INSIGHT_SYSTEM_PROMPT, prompt);
        return parseInsights(rawJson);
    }

    // --- Data fetching (filtered) ---

    private List<Invoice> fetchFilteredInvoices(UUID organizationId, AskInsightRequest request) {
        Specification<Invoice> spec = (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.equal(root.get("organization").get("id"), organizationId));

            if (request.getCustomerId() != null) {
                predicates.add(cb.equal(root.get("customer").get("id"), request.getCustomerId()));
            }
            if (request.getLifeCycleStatus() != null) {
                predicates.add(cb.equal(root.get("lifeCycleStatus"), request.getLifeCycleStatus()));
            }
            if (request.getTimeStatus() != null) {
                predicates.add(cb.equal(root.get("timeStatus"), request.getTimeStatus()));
            }
            if (request.getFromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("issueDate"), request.getFromDate()));
            }
            if (request.getToDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("issueDate"), request.getToDate()));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        return invoiceRepository.findAll(spec);
    }

    private List<FollowUp> fetchFilteredFollowUps(UUID organizationId, FollowUpChannel channel) {
        Specification<FollowUp> spec = (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.equal(root.get("invoice").get("organization").get("id"), organizationId));
            if (channel != null) {
                predicates.add(cb.equal(root.get("channel"), channel));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        return followUpRepository.findAll(spec);
    }

    private String buildScopedDataSummary(List<Invoice> invoices, List<FollowUp> followUps, AskInsightRequest request, Currency currency) {
        LocalDate today = LocalDate.now();
        StringBuilder sb = new StringBuilder();

        // Applied filters section
        sb.append("Applied filters: ");
        boolean hasFilter = false;
        if (request.getCustomerId() != null)    { sb.append("customer-scoped "); hasFilter = true; }
        if (request.getLifeCycleStatus() != null){ sb.append("status=").append(request.getLifeCycleStatus()).append(" "); hasFilter = true; }
        if (request.getTimeStatus() != null)     { sb.append("time=").append(request.getTimeStatus()).append(" "); hasFilter = true; }
        if (request.getChannel() != null)        { sb.append("channel=").append(request.getChannel()).append(" "); hasFilter = true; }
        if (request.getFromDate() != null)       { sb.append("from=").append(request.getFromDate()).append(" "); hasFilter = true; }
        if (request.getToDate() != null)         { sb.append("to=").append(request.getToDate()).append(" "); hasFilter = true; }
        if (!hasFilter) sb.append("none (full org data)");
        sb.append("\n\n");

        // Invoice summary
        sb.append("Invoices (").append(invoices.size()).append(" total):\n");
        Map<LifeCycleStatus, Long> byStatus = invoices.stream()
                .collect(Collectors.groupingBy(Invoice::getLifeCycleStatus, Collectors.counting()));
        byStatus.forEach((status, count) -> sb.append("- ").append(status).append(": ").append(count).append("\n"));

        BigDecimal totalBilled = sum(invoices.stream()
                .filter(i -> i.getLifeCycleStatus() != LifeCycleStatus.DRAFT && i.getLifeCycleStatus() != LifeCycleStatus.CANCELLED)
                .map(Invoice::getTotalAmount).toList());
        BigDecimal totalPaid = sum(invoices.stream().map(Invoice::getTotalPaid).toList());
        BigDecimal totalOutstanding = sum(invoices.stream()
                .filter(i -> i.getLifeCycleStatus() == LifeCycleStatus.ISSUED || i.getLifeCycleStatus() == LifeCycleStatus.PARTIALLY_PAID)
                .map(Invoice::getRemainingAmount).toList());

        sb.append("Total billed: ").append(formatAmount(totalBilled, currency)).append("\n");
        sb.append("Total collected: ").append(formatAmount(totalPaid, currency)).append("\n");
        sb.append("Total outstanding: ").append(formatAmount(totalOutstanding, currency)).append("\n");

        // Overdue detail
        List<Invoice> overdue = invoices.stream()
                .filter(i -> i.getTimeStatus() == TimeStatus.OVERDUE && i.getDueDate() != null)
                .toList();
        if (!overdue.isEmpty()) {
            int maxDays = overdue.stream().mapToInt(i -> (int) ChronoUnit.DAYS.between(i.getDueDate(), today)).max().orElse(0);
            sb.append("Overdue invoices: ").append(overdue.size())
              .append(", oldest: ").append(maxDays).append(" days past due\n");
        }

        // Top customers by outstanding
        Map<String, BigDecimal> topCustomers = invoices.stream()
                .filter(i -> i.getCustomer() != null)
                .filter(i -> i.getLifeCycleStatus() == LifeCycleStatus.ISSUED || i.getLifeCycleStatus() == LifeCycleStatus.PARTIALLY_PAID)
                .collect(Collectors.groupingBy(
                        i -> i.getCustomer().getName(),
                        Collectors.reducing(BigDecimal.ZERO, Invoice::getRemainingAmount, BigDecimal::add)
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(TOP_CUSTOMERS_LIMIT)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
        if (!topCustomers.isEmpty()) {
            sb.append("Top customers by outstanding:\n");
            topCustomers.forEach((name, amount) -> sb.append("- ").append(name).append(": ").append(formatAmount(amount, currency)).append("\n"));
        }

        // Follow-up summary
        if (!followUps.isEmpty()) {
            sb.append("\nReminder/follow-up activity (").append(followUps.size()).append(" total");
            if (request.getChannel() != null) sb.append(", channel=").append(request.getChannel());
            sb.append("):\n");
            Map<FollowUpStatus, Long> byFollowUpStatus = followUps.stream()
                    .collect(Collectors.groupingBy(FollowUp::getStatus, Collectors.counting()));
            byFollowUpStatus.forEach((status, count) -> sb.append("- ").append(status).append(": ").append(count).append("\n"));

            Map<FollowUpChannel, Long> byChannel = followUps.stream()
                    .collect(Collectors.groupingBy(FollowUp::getChannel, Collectors.counting()));
            sb.append("By channel: ");
            byChannel.forEach((ch, count) -> sb.append(ch).append("=").append(count).append(" "));
            sb.append("\n");
        }

        return sb.toString();
    }

    // --- Data fetching ---

    private List<Invoice> fetchActiveInvoices(UUID organizationId) {
        // Exclude DRAFT and CANCELLED — we want the invoices the business actually cares about
        List<LifeCycleStatus> relevantStatuses = List.of(
                LifeCycleStatus.ISSUED,
                LifeCycleStatus.PARTIALLY_PAID,
                LifeCycleStatus.PAID
        );
        Specification<Invoice> spec = (root, query, cb) ->
                cb.and(
                        cb.equal(root.get("organization").get("id"), organizationId),
                        root.get("lifeCycleStatus").in(relevantStatuses)
                );
        return invoiceRepository.findAll(spec);
    }

    private List<Invoice> fetchCustomerInvoices(UUID organizationId, UUID customerId) {
        Specification<Invoice> spec = (root, query, cb) ->
                cb.and(
                        cb.equal(root.get("organization").get("id"), organizationId),
                        cb.equal(root.get("customer").get("id"), customerId)
                );
        return invoiceRepository.findAll(spec);
    }

    // --- Stats computation ---

    private OrgStats computeOrgStats(List<Invoice> invoices) {
        Instant thirtyDaysAgo = Instant.now().minus(RECENT_DAYS, ChronoUnit.DAYS);
        LocalDate today = LocalDate.now();

        List<Invoice> outstanding = invoices.stream()
                .filter(i -> i.getLifeCycleStatus() == LifeCycleStatus.ISSUED
                        || i.getLifeCycleStatus() == LifeCycleStatus.PARTIALLY_PAID)
                .toList();

        List<Invoice> overdue = outstanding.stream()
                .filter(i -> i.getTimeStatus() == TimeStatus.OVERDUE)
                .toList();

        List<Invoice> dueToday = outstanding.stream()
                .filter(i -> i.getTimeStatus() == TimeStatus.DUE_TODAY)
                .toList();

        List<Invoice> notDue = outstanding.stream()
                .filter(i -> i.getTimeStatus() == TimeStatus.NOT_DUE)
                .toList();

        List<Invoice> recentlyPaid = invoices.stream()
                .filter(i -> i.getLifeCycleStatus() == LifeCycleStatus.PAID)
                .filter(i -> i.getUpdatedAt() != null && i.getUpdatedAt().isAfter(thirtyDaysAgo))
                .toList();

        BigDecimal overdueAmount = sum(overdue.stream().map(Invoice::getRemainingAmount).toList());
        BigDecimal dueTodayAmount = sum(dueToday.stream().map(Invoice::getRemainingAmount).toList());
        BigDecimal notDueAmount = sum(notDue.stream().map(Invoice::getRemainingAmount).toList());
        BigDecimal recentlyPaidAmount = sum(recentlyPaid.stream().map(Invoice::getTotalAmount).toList());

        // Longest overdue in days
        int maxDaysOverdue = overdue.stream()
                .filter(i -> i.getDueDate() != null)
                .mapToInt(i -> (int) ChronoUnit.DAYS.between(i.getDueDate(), today))
                .max()
                .orElse(0);

        // Top customers by outstanding balance
        Map<String, BigDecimal> topCustomers = outstanding.stream()
                .filter(i -> i.getCustomer() != null)
                .collect(Collectors.groupingBy(
                        i -> i.getCustomer().getName(),
                        Collectors.reducing(BigDecimal.ZERO, Invoice::getRemainingAmount, BigDecimal::add)
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(TOP_CUSTOMERS_LIMIT)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        return new OrgStats(
                overdue.size(), overdueAmount, maxDaysOverdue,
                dueToday.size(), dueTodayAmount,
                notDue.size(), notDueAmount,
                recentlyPaid.size(), recentlyPaidAmount,
                topCustomers
        );
    }

    private CustomerStats computeCustomerStats(List<Invoice> invoices, Customer customer) {
        long paid = invoices.stream().filter(i -> i.getLifeCycleStatus() == LifeCycleStatus.PAID).count();
        long partiallyPaid = invoices.stream().filter(i -> i.getLifeCycleStatus() == LifeCycleStatus.PARTIALLY_PAID).count();
        long overdue = invoices.stream()
                .filter(i -> (i.getLifeCycleStatus() == LifeCycleStatus.ISSUED || i.getLifeCycleStatus() == LifeCycleStatus.PARTIALLY_PAID)
                        && i.getTimeStatus() == TimeStatus.OVERDUE)
                .count();
        long notYetDue = invoices.stream()
                .filter(i -> i.getLifeCycleStatus() == LifeCycleStatus.ISSUED
                        && i.getTimeStatus() == TimeStatus.NOT_DUE)
                .count();
        long draft = invoices.stream().filter(i -> i.getLifeCycleStatus() == LifeCycleStatus.DRAFT).count();
        long cancelled = invoices.stream().filter(i -> i.getLifeCycleStatus() == LifeCycleStatus.CANCELLED).count();

        BigDecimal totalBilled = sum(invoices.stream()
                .filter(i -> i.getLifeCycleStatus() != LifeCycleStatus.DRAFT
                        && i.getLifeCycleStatus() != LifeCycleStatus.CANCELLED)
                .map(Invoice::getTotalAmount).toList());

        BigDecimal totalPaid = sum(invoices.stream().map(Invoice::getTotalPaid).toList());

        BigDecimal totalOutstanding = sum(invoices.stream()
                .filter(i -> i.getLifeCycleStatus() == LifeCycleStatus.ISSUED
                        || i.getLifeCycleStatus() == LifeCycleStatus.PARTIALLY_PAID)
                .map(Invoice::getRemainingAmount).toList());

        // Oldest overdue in days
        LocalDate today = LocalDate.now();
        int maxDaysOverdue = invoices.stream()
                .filter(i -> i.getTimeStatus() == TimeStatus.OVERDUE && i.getDueDate() != null)
                .mapToInt(i -> (int) ChronoUnit.DAYS.between(i.getDueDate(), today))
                .max()
                .orElse(0);

        return new CustomerStats(
                (long) invoices.size(), paid, partiallyPaid, overdue, notYetDue, draft, cancelled,
                totalBilled, totalPaid, totalOutstanding, maxDaysOverdue,
                customer.isAutomationEnabled()
        );
    }

    // --- Prompt builders ---

    private String buildOverviewPrompt(OrgStats s, Currency currency) {
        StringBuilder sb = new StringBuilder();
        sb.append("Organization Payment Health Data:\n\n");

        sb.append("Outstanding Invoices:\n");
        sb.append("- Overdue: ").append(s.overdueCount).append(" invoices, total outstanding: ").append(formatAmount(s.overdueAmount, currency));
        if (s.maxDaysOverdue > 0) sb.append(" (longest overdue: ").append(s.maxDaysOverdue).append(" days)");
        sb.append("\n");
        sb.append("- Due today: ").append(s.dueTodayCount).append(" invoices, total: ").append(formatAmount(s.dueTodayAmount, currency)).append("\n");
        sb.append("- Not yet due: ").append(s.notDueCount).append(" invoices, total: ").append(formatAmount(s.notDueAmount, currency)).append("\n");

        sb.append("\nLast ").append(RECENT_DAYS).append(" days:\n");
        sb.append("- Invoices paid: ").append(s.recentlyPaidCount).append(", total received: ").append(formatAmount(s.recentlyPaidAmount, currency)).append("\n");

        if (!s.topCustomersByOutstanding.isEmpty()) {
            sb.append("\nTop customers by outstanding balance:\n");
            s.topCustomersByOutstanding.forEach((name, amount) ->
                    sb.append("- ").append(name).append(": ").append(formatAmount(amount, currency)).append("\n"));
        }

        sb.append("""

                Based on this data, provide exactly 3 specific, actionable insights for the business owner.
                Focus on: collection priorities, which customers to contact first, payment trends, and cash flow.
                Each insight must be a single short sentence (under 20 words).
                Use **bold** for key amounts, customer names, and action words.
                Each insight on its own line starting with "- ".

                Respond with JSON: {"insights": "<bullet points separated by \\n>"}
                """);

        return sb.toString();
    }

    private String buildCustomerPrompt(CustomerStats s, Customer customer, Currency currency) {
        String displayName = customer.getCompanyName() != null && !customer.getCompanyName().isBlank()
                ? customer.getName() + " (" + customer.getCompanyName() + ")"
                : customer.getName();

        StringBuilder sb = new StringBuilder();
        sb.append("Customer Payment Profile: ").append(displayName).append("\n\n");

        sb.append("Invoice History (").append(s.totalInvoices).append(" total):\n");
        sb.append("- Fully paid: ").append(s.paid).append("\n");
        sb.append("- Partially paid: ").append(s.partiallyPaid).append("\n");
        sb.append("- Overdue: ").append(s.overdue);
        if (s.maxDaysOverdue > 0) sb.append(" (oldest: ").append(s.maxDaysOverdue).append(" days past due)");
        sb.append("\n");
        sb.append("- Not yet due: ").append(s.notYetDue).append("\n");
        if (s.draft > 0) sb.append("- Draft (unsent): ").append(s.draft).append("\n");
        if (s.cancelled > 0) sb.append("- Cancelled: ").append(s.cancelled).append("\n");

        sb.append("\nFinancials:\n");
        sb.append("- Total billed: ").append(formatAmount(s.totalBilled, currency)).append("\n");
        sb.append("- Total paid: ").append(formatAmount(s.totalPaid, currency)).append("\n");
        sb.append("- Total outstanding: ").append(formatAmount(s.totalOutstanding, currency)).append("\n");

        sb.append("\nAutomated reminders: ").append(s.automationEnabled ? "enabled" : "DISABLED").append("\n");

        sb.append("""

                Based on this data, provide exactly 3 insights covering payment behavior and recommended next actions.
                Each insight must be a single short sentence (under 20 words).
                Use **bold** for key amounts, customer names, and action words.
                Each insight on its own line starting with "- ".

                Respond with JSON: {"insights": "<bullet points separated by \\n>"}
                """);

        return sb.toString();
    }

    // --- Utilities ---

    private BigDecimal sum(List<BigDecimal> values) {
        return values.stream()
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String formatAmount(BigDecimal amount, Currency currency) {
        String symbol = currency.getSymbol();
        if (amount == null) return symbol + "0.00";
        return symbol + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String parseInsights(String rawJson) {
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode insightsNode = root.get("insights");
            if (insightsNode == null || insightsNode.isNull()) {
                throw new InternalException("AI response is missing the 'insights' field");
            }
            return insightsNode.asText();
        } catch (Exception ex) {
            throw new InternalException("Failed to parse AI insights response: " + ex.getMessage());
        }
    }

    // --- Value objects ---

    private record OrgStats(
            int overdueCount, BigDecimal overdueAmount, int maxDaysOverdue,
            int dueTodayCount, BigDecimal dueTodayAmount,
            int notDueCount, BigDecimal notDueAmount,
            int recentlyPaidCount, BigDecimal recentlyPaidAmount,
            Map<String, BigDecimal> topCustomersByOutstanding
    ) {}

    private record CustomerStats(
            long totalInvoices, long paid, long partiallyPaid, long overdue,
            long notYetDue, long draft, long cancelled,
            BigDecimal totalBilled, BigDecimal totalPaid, BigDecimal totalOutstanding,
            int maxDaysOverdue, boolean automationEnabled
    ) {}
}
