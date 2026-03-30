package com.flowcollect.infrastructure.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class ChatCompletionRequest {

    private String model;
    private List<ChatMessage> messages;

    @JsonProperty("response_format")
    private Map<String, String> responseFormat = Map.of("type", "json_object");

    @JsonProperty("max_tokens")
    private int maxTokens;

    public ChatCompletionRequest() {
    }

    public ChatCompletionRequest(String model, List<ChatMessage> messages, int maxTokens) {
        this.model = model;
        this.messages = messages;
        this.maxTokens = maxTokens;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages;
    }

    public Map<String, String> getResponseFormat() {
        return responseFormat;
    }

    public void setResponseFormat(Map<String, String> responseFormat) {
        this.responseFormat = responseFormat;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }
}
