package com.size.controller;

import com.size.model.SwapConfirmRequest;
import com.size.model.ToolChatRequest;
import com.size.model.ToolChatResponse;
import com.size.service.ToolChatService;
import com.size.tools.EmployeeAssistantTool;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/tools")
public class ToolChatController {

    private static final String ID_PATTERN = "[A-Za-z0-9_-]{1,64}";

    private final ToolChatService toolChatService;

    public ToolChatController(ToolChatService toolChatService) {
        this.toolChatService = toolChatService;
    }

    @PostMapping("/chat")
    public ToolChatResponse chat(
            @RequestHeader("X-Employee-Id")
            @NotBlank
            @Pattern(regexp = ID_PATTERN)
            String employeeId,
            @Valid @RequestBody ToolChatRequest request) {
        return new ToolChatResponse(
                toolChatService.chat(employeeId, request.message())
        );
    }

    @PostMapping("/shift-swaps/confirm")
    public EmployeeAssistantTool.SwapResult confirmSwap(
            @RequestHeader("X-Employee-Id")
            @NotBlank
            @Pattern(regexp = ID_PATTERN)
            String employeeId,
            @Valid @RequestBody SwapConfirmRequest request) {
        return toolChatService.confirmSwap(
                employeeId,
                request.confirmationToken()
        );
    }
}