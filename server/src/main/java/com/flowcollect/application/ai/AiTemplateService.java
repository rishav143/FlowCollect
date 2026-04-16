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
            You are an expert at writing professional payment reminder messages for small businesses and freelancers.

            Core rules — follow these without exception:
            - Preserve every {{placeholder}} exactly as written — never alter spelling, case, or braces
            - Never write literal currency amounts (e.g. ₹5,000 or $1,200) — always use {{remainingAmount}} instead
            - Respond with valid JSON only — no markdown, no code fences, no explanation
            - Never use em dashes (—) anywhere in subject or body; use a comma, period, or rephrase instead

            Writing quality rules:
            - Be concise — every sentence must carry information; cut anything that does not
            - Use active voice; avoid passive constructions like "has not been settled" when "remains unpaid" is shorter
            - No filler openers: never start with "I hope this email finds you well", "Just a quick note", or "We wanted to follow up"
            - No hollow closers: never use "Please do not hesitate to contact us", "Feel free to reach out", or "Best regards" as a standalone line
            - Sign off with only {{organizationName}} (and {{organizationEmail}} on a second line if needed) — no "Regards," or "Thanks,"

            Anti-spam rules — output must pass Gmail filters for transactional email:
            - Subject: never use "Reminder", "Action Required", "URGENT", "Re:", "FWD:", "Past Due", "Payment Request", or ALL-CAPS words
            - Subject: be specific — include the invoice number or amount, not a generic phrase
            - Subject: avoid excessive punctuation (!!), symbols, or question marks
            - Body: never use "click the button below", "click here", "dear customer", "kindly", "as soon as possible" (use "promptly" or a deadline instead)
            - Body: no promotional language — this is a transactional message, not a marketing email
            - Body: do not open with "Dear {{customerName}}," — "Hi {{customerName}}," is fine
            """;

    // Placeholders that the TemplateRenderer understands
    private static final String PLACEHOLDER_GUIDE = """
            Available placeholders — use ONLY these, never invent others:
            - {{customerName}} — recipient's full name
            - {{invoiceNumber}} — invoice reference number
            - {{remainingAmount}} — remaining balance, already formatted with currency symbol — do not add any currency code or symbol around it
            - {{dueDate}} — payment due date
            - {{confirmationLink}} — link for the customer to confirm their payment
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
    public enum EnhanceAction { POLISH, REWRITE_TONE }

    public AiTemplateResult enhanceTemplate(
            UUID organizationId,
            TemplateChannel channel,
            TemplateTone tone,
            String existingSubject,
            String existingBody,
            EnhanceAction action
    ) {
        organizationService.getById(organizationId);

        String userPrompt = action == EnhanceAction.POLISH
                ? buildPolishPrompt(channel, tone, existingSubject, existingBody)
                : buildRewriteTonePrompt(channel, tone, existingSubject, existingBody);
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

                Rules:
                - Use at least {{customerName}}, {{invoiceNumber}}, {{remainingAmount}}, and {{dueDate}}
                - Include {{confirmationLink}} as the call-to-action
                - subject must be null for SMS and WHATSAPP; a short factual subject line for EMAIL
                - Be concise — do not pad the message with filler sentences; every line must carry information
                - Do not add explanations, marketing language, or content beyond what is needed to inform and prompt payment

                Respond with JSON in this exact shape, no extra fields:
                {"subject": "<email subject or null>", "body": "<message body>"}
                """.formatted(
                currency,
                channel.name(),
                tone.name(), toneDescription,
                PLACEHOLDER_GUIDE,
                channelInstructions
        );
    }

    private String buildPolishPrompt(
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
                Polish the following payment reminder template. Improve clarity, conciseness, and spam-safety — nothing else.

                Channel: %s
                Tone to preserve: %s — %s

                %s
                %s

                === EXISTING TEMPLATE ===
                %s
                Body:
                %s
                === END OF TEMPLATE ===

                Strict polish rules:
                - Do NOT add new sentences, ideas, or content that is not already in the original
                - Do NOT change the meaning or facts of any sentence
                - Remove filler phrases, em dashes, and spam-trigger words
                - Keep approximately the same length — do not expand the message
                - Preserve the original tone; only fix wording, not intent
                - subject must be null for SMS and WHATSAPP; lightly improve the subject line for EMAIL if present

                Respond with JSON in this exact shape:
                {"subject": "<email subject or null>", "body": "<polished body>"}
                """.formatted(
                channel.name(),
                tone.name(), toneDescription,
                PLACEHOLDER_GUIDE,
                channelInstructions,
                subjectSection,
                existingBody != null ? existingBody : ""
        );
    }

    private String buildRewriteTonePrompt(
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
                Rewrite the following payment reminder template in the target tone. Keep the same facts and structure — only the tone and phrasing should change.

                Channel: %s
                Target tone: %s — %s

                %s
                %s

                === EXISTING TEMPLATE ===
                %s
                Body:
                %s
                === END OF TEMPLATE ===

                Strict rewrite rules:
                - Every piece of information in the original must appear in the rewrite — do not add or remove facts
                - Do NOT add new content; do NOT drop existing content
                - Apply the target tone consistently across every sentence
                - Keep approximately the same length as the original
                - subject must be null for SMS and WHATSAPP; rewrite the subject line to match the new tone for EMAIL

                Respond with JSON in this exact shape:
                {"subject": "<email subject or null>", "body": "<rewritten body>"}
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
                    - Write a clear, professional email body with a natural opening, concise body, and a sign-off
                    - Paragraphs separated by blank lines (\\n\\n)
                    - Keep body under 1200 characters; 1600 at absolute maximum
                    - Subject line: use the invoice number or business name to make it specific (e.g. "Invoice {{invoiceNumber}} from {{organizationName}}")
                    - Subject line: must not contain trigger words — see system-level anti-spam rules
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
