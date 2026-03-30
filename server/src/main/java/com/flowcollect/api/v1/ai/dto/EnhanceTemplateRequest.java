package com.flowcollect.api.v1.ai.dto;

import com.flowcollect.domain.template.TemplateChannel;
import com.flowcollect.domain.template.TemplateTone;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class EnhanceTemplateRequest {

    @NotNull(message = "channel is required")
    private TemplateChannel channel;

    @NotNull(message = "tone is required")
    private TemplateTone tone;

    // Optional — only meaningful for EMAIL; ignored for SMS / WHATSAPP
    private String subject;

    @NotBlank(message = "body is required")
    private String body;

    public TemplateChannel getChannel() {
        return channel;
    }

    public void setChannel(TemplateChannel channel) {
        this.channel = channel;
    }

    public TemplateTone getTone() {
        return tone;
    }

    public void setTone(TemplateTone tone) {
        this.tone = tone;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
