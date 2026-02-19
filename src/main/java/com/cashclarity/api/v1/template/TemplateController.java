package com.cashclarity.api.v1.template;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cashclarity.api.v1.template.dto.TemplateMapper;
import com.cashclarity.api.v1.template.dto.TemplateRequest;
import com.cashclarity.api.v1.template.dto.TemplateResponse;
import com.cashclarity.application.template.TemplateService;
import com.cashclarity.domain.template.Template;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/templates")
public class TemplateController {
    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @PostMapping
    public ResponseEntity<TemplateResponse> createTemplate(@RequestBody @Valid TemplateRequest templateRequest) {
        Template template = templateService.createTemplate(templateRequest);
        return ResponseEntity.ok(TemplateMapper.toResponse(template));
    }
}
