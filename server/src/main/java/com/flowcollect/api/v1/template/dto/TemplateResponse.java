package com.flowcollect.api.v1.template.dto;

import java.time.Instant;
import java.util.UUID;

import com.flowcollect.domain.template.TemplateChannel;
import com.flowcollect.domain.template.TemplateTone;

public class TemplateResponse {
    private UUID id;
    private String name;
    private TemplateChannel channel;
    private String subject;
    private String body;
    private TemplateTone tone;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
    
    // Getters & Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
