package com.size.model.graph;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ShiftWorkflowRequest(
        @NotBlank(message = "换班日期不能为空")
        @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "换班日期格式必须为 yyyy-MM-dd")
        String shiftDate,

        @NotBlank(message = "目标班次不能为空")
        @Pattern(regexp = "白班|晚班", message = "目标班次只能是白班或晚班")
        String targetShift,

        @NotBlank(message = "换班原因不能为空")
        @Size(max = 200, message = "换班原因不能超过 200 个字符")
        String reason
) {
}
