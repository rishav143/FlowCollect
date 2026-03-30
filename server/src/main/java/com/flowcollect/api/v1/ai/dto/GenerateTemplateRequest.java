package com.flowcollect.api.v1.ai.dto;

import com.flowcollect.domain.template.TemplateChannel;
import com.flowcollect.domain.template.TemplateTone;
import jakarta.validation.constraints.NotNull;

public class GenerateTemplateRequest {

    @NotNull(message = "channel is required")
    private TemplateChannel channel;

    @NotNull(message = "tone is required")
    private TemplateTone tone;

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
}
