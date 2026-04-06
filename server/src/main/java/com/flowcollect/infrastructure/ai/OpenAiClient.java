package com.flowcollect.infrastructure.ai;

import com.flowcollect.exception.http.ServiceUnavailableException;
import com.flowcollect.infrastructure.ai.dto.ChatCompletionRequest;
import com.flowcollect.infrastructure.ai.dto.ChatCompletionResponse;
import com.flowcollect.infrastructure.ai.dto.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * Thin HTTP wrapper around the OpenAI Chat Completions API.
 * All callers receive a raw JSON string from the model's message content.
 * The model is always instructed to respond with JSON (response_format: json_object),
 * so callers can safely parse the content with an ObjectMapper.
 */
@Component
public class OpenAiClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiClient.class);
    private static final String CHAT_COMPLETIONS_PATH = "/v1/chat/completions";

    private final OpenAiProperties properties;
    private final RestClient restClient;

    public OpenAiClient(OpenAiProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    /**
     * Sends a system + user message pair to the chat completions endpoint.
     * Returns the raw JSON string from the assistant's reply.
     *
     * @throws ServiceUnavailableException if OpenAI is disabled, misconfigured, or the call fails
     */
    public String chat(String systemPrompt, String userPrompt) {
        ensureEnabled();

        List<ChatMessage> messages = List.of(
                new ChatMessage("system", systemPrompt),
                new ChatMessage("user", userPrompt)
        );

        ChatCompletionRequest request = new ChatCompletionRequest(
                properties.getModel(),
                messages,
                properties.getMaxTokens()
        );

        try {
            ChatCompletionResponse response = restClient.post()
                    .uri(CHAT_COMPLETIONS_PATH)
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ChatCompletionResponse.class);

            if (response == null || response.firstContent() == null) {
                throw new ServiceUnavailableException("OpenAI returned an empty response");
            }

            log.debug("OpenAI chat completion succeeded using model={}", properties.getModel());
            return response.firstContent();

        } catch (org.springframework.web.client.HttpClientErrorException ex) {
            log.error("OpenAI API client error: status={} body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new ServiceUnavailableException("AI request failed: " + ex.getStatusCode());
        } catch (RestClientException ex) {
            log.error("OpenAI API call failed: {}", ex.getMessage());
            throw new ServiceUnavailableException("AI service is temporarily unavailable. Please try again later.");
        }
    }

    private void ensureEnabled() {
        if (!properties.isEnabled()) {
            throw new ServiceUnavailableException("AI features are not enabled for this deployment");
        }
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new ServiceUnavailableException("OpenAI API key is not configured");
        }
    }
}
