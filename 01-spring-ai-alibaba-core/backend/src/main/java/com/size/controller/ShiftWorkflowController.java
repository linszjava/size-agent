package com.size.controller;

import com.size.model.graph.ShiftWorkflowRequest;
import com.size.model.graph.ShiftWorkflowResponse;
import com.size.model.graph.WorkflowApprovalRequest;
import com.size.service.ShiftWorkflowService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/workflows/shift-swaps")
public class ShiftWorkflowController {

    private static final String ID_PATTERN = "[A-Za-z0-9_-]{1,64}";

    private final ShiftWorkflowService shiftWorkflowService;

    public ShiftWorkflowController(ShiftWorkflowService shiftWorkflowService) {
        this.shiftWorkflowService = shiftWorkflowService;
    }

    @PostMapping
    public ShiftWorkflowResponse start(
            @RequestHeader("X-Employee-Id")
            @NotBlank @Pattern(regexp = ID_PATTERN) String employeeId,
            @Valid @RequestBody ShiftWorkflowRequest request) {
        return shiftWorkflowService.start(employeeId, request);
    }

    @PostMapping("/{workflowId}/approval")
    public ShiftWorkflowResponse approve(
            @RequestHeader("X-Employee-Id")
            @NotBlank @Pattern(regexp = ID_PATTERN) String employeeId,
            @PathVariable @Pattern(regexp = ID_PATTERN) String workflowId,
            @Valid @RequestBody WorkflowApprovalRequest request) {
        return shiftWorkflowService.approve(
                employeeId,
                workflowId,
                request.approved()
        );
    }
}
