package com.size.model.graph;

import jakarta.validation.constraints.NotNull;

public record WorkflowApprovalRequest(
        @NotNull(message = "审批结果不能为空") Boolean approved
) {
}
