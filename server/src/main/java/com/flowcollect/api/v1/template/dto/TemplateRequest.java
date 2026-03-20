package com.flowcollect.api.v1.template.dto;

import com.flowcollect.domain.template.TemplateChannel;
import com.flowcollect.domain.template.TemplateTone;

public class TemplateRequest {

    private String name;

    private TemplateChannel channel;
    
    private String subject;

    private String body;
    private TemplateTone tone;
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

}
