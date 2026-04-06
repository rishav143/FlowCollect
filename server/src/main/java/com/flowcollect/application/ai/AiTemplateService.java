package com.flowcollect.application.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowcollect.application.organization.OrganizationService;
import com.flowcollect.domain.organization.Organization;
import com.flowcollect.domain.template.TemplateChannel;
import com.flowcollect.domain.template.TemplateTone;
import com.flowcollect.exception.http.InternalException;
import com.flowcollect.infrastructure.ai.OpenAiClient;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Application service that uses the OpenAI client to generate and enhance
 * payment reminder templates. Prompts are designed around the FlowCollect
 * domain: invoices, due dates, payment links, and confirmation flows.
 */
@Service
public class AiTemplateService {

    private static final String SYSTEM_PROMPT = """
            You are an expert at writing professional payment reminder messages for businesses.
            Your messages are concise, clear, and appropriate for the specified tone and channel.
            You always preserve {{placeholder}} variables exactly as-is — never alter their spelling or braces.
            You always respond with valid JSON only, no markdown, no code fences.
            """;

    // Placeholders that the TemplateRenderer understands — ONLY use these, no others
    private static final String PLACEHOLDER_GUIDE = """
            Available placeholders (use ONLY these — do not invent any others):
            - {{customerName}} — recipient's full name
            - {{invoiceNumber}} — invoice reference number
            - {{remainingAmount}} — remaining unpaid amount, already formatted with currency symbol (e.g. "₹5,000") — do NOT add a currency code or symbol before or after this placeholder
            - {{dueDate}} — payment due date
            - {{confirmationLink}} — link for the customer to confirm they have made the payment
            - {{organizationName}} — business name
            - {{organizationEmail}} — business contact email
            """;

    private final OpenAiClient openAiClient;
    private final OrganizationService organizationService;
    private final ObjectMapper objectMapper;

    public AiTemplateService(
            OpenAiClient openAiClient,
            OrganizationService organizationService,
            ObjectMapper objectMapper
    ) {
        this.openAiClient = openAiClient;
        this.organizationService = organizationService;
        this.objectMapper = objectMapper;
    }

    /**
     * Generates a brand-new template body (and subject for EMAIL) from scratch.
     *
     * @param organizationId used to provide currency context to the prompt
     * @param channel        EMAIL, SMS, or WHATSAPP
     * @param tone           POLITE, NEUTRAL, or FIRM
     * @return parsed result with subject (nullable) and body
     */
    public AiTemplateResult generateTemplate(UUID organizationId, TemplateChannel channel, TemplateTone tone) {
        Organization org = organizationService.getById(organizationId);

        String currencyCode = org.getCurrency() != null ? org.getCurrency().getCurrencyCode() : "USD";
        String userPrompt = buildGeneratePrompt(channel, tone, currencyCode);
        String rawJson = openAiClient.chat(SYSTEM_PROMPT, userPrompt);
        return parseResult(rawJson, channel);
    }

    /**
     * Takes an existing template body (and optional subject) and rewrites it
     * to better match the requested tone while keeping all placeholders intact.
     *
     * @param organizationId scoping guard — confirms the org exists
     * @param channel        channel of the original template (affects formatting rules)
     * @param tone           desired output tone
     * @param existingSubject existing subject line (may be null for SMS/WHATSAPP)
     * @param existingBody    existing body text
     * @return improved subject (nullable) and body
     */
    public AiTemplateResult enhanceTemplate(
            UUID organizationId,
            TemplateChannel channel,
            TemplateTone tone,
            String existingSubject,
            String existingBody
    ) {
        organizationService.getById(organizationId);

        String userPrompt = buildEnhancePrompt(channel, tone, existingSubject, existingBody);
        String rawJson = openAiClient.chat(SYSTEM_PROMPT, userPrompt);
        return parseResult(rawJson, channel);
    }

    // --- Prompt builders ---

    private String buildGeneratePrompt(TemplateChannel channel, TemplateTone tone, String currency) {
        String channelInstructions = channelInstructions(channel);
        String toneDescription = toneDescription(tone);

        return """
                Generate a payment reminder template for a business collecting payments in %s.

                Channel: %s
                Tone: %s — %s

                %s
                %s

                Respond with JSON in this exact shape (no extra fields):
                {"subject": "<email subject or null>", "body": "<message body>"}

                Rules:
                - subject must be null for SMS and WHATSAPP channels
                - subject must be a short, specific email subject line for EMAIL
                - body must follow the channel formatting rules above
                - Use at least {{customerName}}, {{invoiceNumber}}, {{remainingAmount}}, and {{dueDate}}
                - Include {{confirmationLink}} as the call-to-action
                - Do NOT use any placeholder not listed in the available placeholders above
                - NEVER write literal currency amounts (e.g. ₹5,000 or $1,200) — ALWAYS use {{remainingAmount}} or other placeholders instead
                """.formatted(
                currency,
                channel.name(),
                tone.name(), toneDescription,
                PLACEHOLDER_GUIDE,
                channelInstructions
        );
    }

    private String buildEnhancePrompt(
            TemplateChannel channel,
            TemplateTone tone,
            String existingSubject,
            String existingBody
    ) {
        String channelInstructions = channelInstructions(channel);
        String toneDescription = toneDescription(tone);
        String subjectSection = existingSubject != null && !existingSubject.isBlank()
                ? "Current subject: " + existingSubject
                : "No subject (not an email or subject not provided)";

        return """
                Enhance the following payment reminder template to better match the requested tone and channel best practices.

                Channel: %s
                Target tone: %s — %s

                %s
                %s

                === EXISTING TEMPLATE ===
                %s
                Body:
                %s
                === END OF TEMPLATE ===

                Rules:
                - Keep every {{placeholder}} exactly as written — do not alter or remove any
                - Preserve the core intent of the original message
                - Apply the target tone throughout
                - Follow channel formatting rules above
                - subject must be null for SMS and WHATSAPP
                - NEVER write literal currency amounts (e.g. ₹5,000 or $1,200) — ALWAYS use {{remainingAmount}} or other placeholders instead

                Respond with JSON in this exact shape:
                {"subject": "<email subject or null>", "body": "<improved body>"}
                """.formatted(
                channel.name(),
                tone.name(), toneDescription,
                PLACEHOLDER_GUIDE,
                channelInstructions,
                subjectSection,
                existingBody != null ? existingBody : ""
        );
    }

    // --- Channel and tone descriptors ---

    private String channelInstructions(TemplateChannel channel) {
        return switch (channel) {
            case EMAIL -> """
                    Channel formatting (EMAIL):
                    - Write a clear, professional email body with greeting, body paragraphs, and sign-off
                    - Paragraphs separated by blank lines (\\n\\n)
                    - Keep body under 1500 characters; 2000 at absolute maximum
                    - The body should be complete enough to stand alone as an email
                    """;
            case SMS -> """
                    Channel formatting (SMS):
                    - Target 160 characters or fewer for the body; 320 at absolute maximum
                    - No subject line
                    - Plain text only — no markdown, no HTML
                    - Include one clear call-to-action (e.g., a short URL placeholder)
                    """;
            case WHATSAPP -> """
                    Channel formatting (WHATSAPP):
                    - Conversational but professional tone
                    - No subject line
                    - Keep body under 800 characters; 1000 at absolute maximum
                    - Use *bold* (single asterisk) for key values like amounts or dates
                    - Use line breaks (\\n) for readability
                    - End with a friendly call-to-action including the {{confirmationLink}}
                    """;
        };
    }

    private String toneDescription(TemplateTone tone) {
        return switch (tone) {
            case POLITE -> "friendly, warm, understanding — assumes good faith; gentle reminder language";
            case NEUTRAL -> "professional, factual, no emotional language — matter-of-fact statement of the outstanding balance";
            case FIRM -> "assertive, direct, and urgent — makes consequences clear without being aggressive";
        };
    }

    // --- Response parsing ---

    private AiTemplateResult parseResult(String rawJson, TemplateChannel channel) {
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            String subject = null;
            if (channel == TemplateChannel.EMAIL) {
                JsonNode subjectNode = root.get("subject");
                if (subjectNode != null && !subjectNode.isNull()) {
                    subject = subjectNode.asText(null);
                }
            }
            JsonNode bodyNode = root.get("body");
            if (bodyNode == null || bodyNode.isNull()) {
                throw new InternalException("AI response is missing the 'body' field");
            }
            return new AiTemplateResult(subject, bodyNode.asText());
        } catch (Exception ex) {
            throw new InternalException("Failed to parse AI response: " + ex.getMessage());
        }
    }

    /**
     * Simple value object returned from both generate and enhance operations.
     */
    public record AiTemplateResult(String subject, String body) {
    }
}
