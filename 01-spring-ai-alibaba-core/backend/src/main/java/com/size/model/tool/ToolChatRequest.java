package com.size.model.tool;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ToolChatRequest(
        @NotBlank(message = "消息不能为空")
        @Size(max = 1000, message = "消息长度不能超过 1000 个字符")
        String message
) {
}
