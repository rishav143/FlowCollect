package com.flowcollect.application.billing;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.flowcollect.domain.organization.OrgPlan;
import com.flowcollect.domain.organization.Organization;
import com.flowcollect.exception.http.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Currency;
import java.util.List;
import java.util.Map;

/**
 * Creates Dodo Payments checkout sessions for FlowCollect subscription upgrades.
 *
 * Product selection:
 *   INR orgs  → dodo.pro-product-id-inr  (₹499/month, India-specific product)
 *   All others → dodo.pro-product-id-usd ($12/month, global product)
 *
 * Two products are required because the INR and USD prices are intentionally
 * different (PPP pricing) and cannot be expressed via Dodo's adaptive currency
 * (which uses live exchange rates, not fixed prices).
 *
 * Flow:
 *  1. Frontend calls POST /api/v1/organizations/{id}/billing/checkout { plan: "PRO" }
 *  2. This service builds a checkout session via Dodo's REST API
 *  3. Returns the hosted checkout URL
 *  4. Frontend redirects the browser to that URL
 *  5. After payment Dodo posts a webhook → DodoWebhookController activates the plan
 */
@Service
public class DodoBillingService {

    private static final Logger  log = LoggerFactory.getLogger(DodoBillingService.class);
    private static final Currency INR = Currency.getInstance("INR");

    private final boolean enabled;
    private final String  proProductIdInr;
    private final String  proProductIdUsd;
    private final RestClient restClient;

    public DodoBillingService(
            @Value("${dodo.enabled:false}")                                    boolean enabled,
            @Value("${dodo.api-key:}")                                         String  apiKey,
            @Value("${dodo.base-url:https://live.dodopayments.com}")            String  baseUrl,
            @Value("${dodo.pro-product-id-inr:}")                              String  proProductIdInr,
            @Value("${dodo.pro-product-id-usd:}")                              String  proProductIdUsd
    ) {
        this.enabled          = enabled;
        this.proProductIdInr  = proProductIdInr;
        this.proProductIdUsd  = proProductIdUsd;
        this.restClient       = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type",  MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Creates a Dodo checkout session for the given plan and returns the hosted URL.
     *
     * @param org       the organization subscribing
     * @param plan      target plan (currently only PRO supported)
     * @param returnUrl where Dodo redirects on successful payment
     * @param cancelUrl where Dodo redirects if the user abandons checkout
     */
    public String createCheckoutSession(Organization org, OrgPlan plan, String returnUrl, String cancelUrl) {
        if (!enabled) {
            throw new ServiceUnavailableException("Billing is not enabled on this server");
        }

        String productId = resolveProductId(plan, org.getCurrency());
        Object customer  = buildCustomer(org);

        String billingCurrency = org.getCurrency().getCurrencyCode();

        CheckoutSessionRequest body = new CheckoutSessionRequest(
                List.of(new ProductCartItem(productId, 1)),
                customer,
                returnUrl,
                cancelUrl,
                billingCurrency,
                Map.of("organizationId", org.getId().toString())
        );

        log.info("Creating Dodo checkout session for org={} plan={} currency={}",
                org.getId(), plan, org.getCurrency());

        CheckoutSessionResponse response = restClient.post()
                .uri("/checkouts")
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    String body2 = new String(res.getBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    log.error("Dodo API error: status={} body={}", res.getStatusCode(), body2);
                    throw new ServiceUnavailableException(
                            "Billing provider returned an error. Please try again later.");
                })
                .body(CheckoutSessionResponse.class);

        if (response == null || response.checkoutUrl() == null) {
            throw new ServiceUnavailableException("Billing provider returned an empty response");
        }

        return response.checkoutUrl();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Selects the Dodo product ID based on plan and org currency.
     * INR orgs → INR product (₹499); all others → USD product ($12).
     */
    private String resolveProductId(OrgPlan plan, Currency currency) {
        if (plan != OrgPlan.PRO) {
            throw new IllegalArgumentException("Only PRO plan checkout is supported: " + plan);
        }

        boolean isInr     = INR.equals(currency);
        String  productId = isInr ? proProductIdInr : proProductIdUsd;

        if (productId == null || productId.isBlank()) {
            String region = isInr ? "INR" : "USD";
            throw new ServiceUnavailableException(
                    "PRO plan product is not configured for region: " + region);
        }
        return productId;
    }

    /**
     * Reuses the existing Dodo customer ID if present (pre-fills saved payment method).
     * Otherwise lets Dodo create a new customer from the org's email and name.
     */
    private Object buildCustomer(Organization org) {
        if (org.getDodoCustomerId() != null && !org.getDodoCustomerId().isBlank()) {
            return new ExistingCustomer(org.getDodoCustomerId());
        }
        return new NewCustomer(org.getEmail(), org.getName());
    }

    // ------------------------------------------------------------------
    // Internal request / response records (package-private for tests)
    // ------------------------------------------------------------------

    record CheckoutSessionRequest(
            @JsonProperty("product_cart")     List<ProductCartItem> productCart,
            Object                                                   customer,
            @JsonProperty("return_url")       String                returnUrl,
            @JsonProperty("cancel_url")       String                cancelUrl,
            @JsonProperty("billing_currency") String                billingCurrency,
            Map<String, String>                                      metadata
    ) {}

    record ProductCartItem(
            @JsonProperty("product_id") String productId,
            int                                 quantity
    ) {}

    record ExistingCustomer(
            @JsonProperty("customer_id") String customerId
    ) {}

    record NewCustomer(
            String email,
            String name
    ) {}

    record CheckoutSessionResponse(
            @JsonProperty("session_id")   String sessionId,
            @JsonProperty("checkout_url") String checkoutUrl
    ) {}
}
