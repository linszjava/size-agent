package com.size.model.agent;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AgentChatRequest(
        @NotBlank(message = "conversationId 不能为空")
        @Pattern(regexp = "[A-Za-z0-9_-]{1,64}", message = "conversationId 格式不合法")
        String conversationId,

        @NotBlank(message = "message 不能为空")
        @Size(max = 1000, message = "message 不能超过 1000 个字符")
        String message
) {
}
