package com.size.controller;


import com.size.model.rag.KnowledgeUploadResponse;
import com.size.model.rag.KnowledgeVisibility;
import com.size.model.rag.RagAnswerResponse;
import com.size.model.rag.RagQuestionRequest;
import com.size.service.KnowledgeBaseService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeBaseController {

    private static final String ID_PATTERN = "[A-Za-z0-9_-]{1,64}";

    private final KnowledgeBaseService knowledgeBaseService;

    public KnowledgeBaseController(
            KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @PostMapping(
            value = "/documents",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public KnowledgeUploadResponse upload(
            @RequestHeader("X-Tenant-Id")
            @NotBlank
            @Pattern(regexp = ID_PATTERN)
            String tenantId,
            @RequestHeader("X-Department-Id")
            @NotBlank
            @Pattern(regexp = ID_PATTERN)
            String departmentId,
            @RequestParam(defaultValue = "DEPARTMENT")
            KnowledgeVisibility visibility,
            @RequestPart("file") MultipartFile file) {
        return knowledgeBaseService.upload(
                tenantId,
                departmentId,
                visibility,
                file
        );
    }

    @PostMapping("/ask")
    public RagAnswerResponse ask(
            @RequestHeader("X-Tenant-Id")
            @NotBlank
            @Pattern(regexp = ID_PATTERN)
            String tenantId,
            @RequestHeader("X-Department-Id")
            @NotBlank
            @Pattern(regexp = ID_PATTERN)
            String departmentId,
            @Valid @RequestBody RagQuestionRequest request) {
        return knowledgeBaseService.ask(
                tenantId,
                departmentId,
                request.question()
        );
    }
}
