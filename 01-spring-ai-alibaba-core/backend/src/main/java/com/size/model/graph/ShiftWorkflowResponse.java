package com.size.model.graph;

public record ShiftWorkflowResponse(
        String workflowId,
        String status,
        String currentShift,
        String targetShift,
        String policy,
        String requestId,
        String message
) {
}