package com.size.controller;

import com.size.model.agent.AgentChatRequest;
import com.size.model.agent.AgentChatResponse;
import com.size.service.EmployeeAgentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/agents")
public class EmployeeAgentController {

    private static final String ID_PATTERN = "[A-Za-z0-9_-]{1,64}";

    private final EmployeeAgentService employeeAgentService;

    public EmployeeAgentController(EmployeeAgentService employeeAgentService) {
        this.employeeAgentService = employeeAgentService;
    }

    @PostMapping("/employee/chat")
    public AgentChatResponse chat(
            @RequestHeader("X-Employee-Id")
            @NotBlank
            @Pattern(regexp = ID_PATTERN)
            String employeeId,
            @Valid @RequestBody AgentChatRequest request) {
        return employeeAgentService.chat(employeeId, request);
    }
}