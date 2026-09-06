package com.size.model;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(

        @NotBlank(message = "message 不能为空")
        @Size(max = 4000, message = "message 最多 4000 个字符")
        String message
) {
}
