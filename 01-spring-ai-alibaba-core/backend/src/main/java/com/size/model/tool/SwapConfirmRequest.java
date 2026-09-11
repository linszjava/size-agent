package com.size.model.tool;

import jakarta.validation.constraints.NotBlank;

public record SwapConfirmRequest(
        @NotBlank(message = "确认令牌不能为空")
        String confirmationToken
) {
}
