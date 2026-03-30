package com.flowcollect.api.v1.ai.dto;

public class AiTemplateResponse {

    private String subject;
    private String body;

    public AiTemplateResponse(String subject, String body) {
        this.subject = subject;
        this.body = body;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }
}
