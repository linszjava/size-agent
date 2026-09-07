package com.size.model;

import jakarta.validation.constraints.*;

import java.util.List;

public record CoreChatRequest(
        @NotBlank(message = "message 不能为空")
        @Size(max = 4000, message = "message 最多 4000 个字符")
        String message,

        @Size(max = 1000, message = "systemPrompt 最多 1000 个字符")
        String systemPrompt,

        @DecimalMin(value = "0.0", message = "temperature 不能小于 0")
        @DecimalMax(value = "2.0", message = "temperature 不能大于 2")
        Double temperature,

        @DecimalMin(value = "0.0", inclusive = false, message = "topP 必须大于 0")
        @DecimalMax(value = "1.0", message = "topP 不能大于 1")
        Double topP,

        @Min(value = 1, message = "maxTokens 不能小于 1")
        @Max(value = 4000, message = "maxTokens 不能大于 4000")
        Integer maxTokens,

        @Size(max = 4, message = "stopSequences 最多 4 个")
        List<@NotBlank(message = "stopSequences 不能包含空值") String> stopSequences
) {
}
