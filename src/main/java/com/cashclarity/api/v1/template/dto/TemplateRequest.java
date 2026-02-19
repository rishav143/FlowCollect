package com.cashclarity.api.v1.template.dto;

import java.util.UUID;

import com.cashclarity.domain.template.TemplateChannel;
import com.cashclarity.domain.template.TemplateTone;

import jakarta.validation.constraints.Size;

public class TemplateRequest {

    @Size(max = 100, message = "Name must be less than 100 characters")
    private String name;

    private UUID organizationId;

    private TemplateChannel channel;
    
    @Size(max = 200, message = "Subject must be less than 200 characters")
    private String subject;

    private String body;
    private TemplateTone tone;
    private boolean active;

    public String getName() {
        return name;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TemplateChannel getChannel() {
        return channel;
    }

    public void setChannel(TemplateChannel channel) {
        this.channel = channel;
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

    public TemplateTone getTone() {
        return tone;
    }

    public void setTone(TemplateTone tone) {
        this.tone = tone;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
