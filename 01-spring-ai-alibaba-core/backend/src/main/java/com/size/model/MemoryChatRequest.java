package com.size.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MemoryChatRequest(
        @NotBlank(message = "conversationId 不能为空")
        @Pattern(regexp = "[A-Za-z0-9_-]{1,64}", message = "conversationId 格式不正确")
        String conversationId,

        @NotBlank(message = "message 不能为空")
        @Size(max = 4000, message = "message 最多 4000 个字符")
        String message
) {
}
