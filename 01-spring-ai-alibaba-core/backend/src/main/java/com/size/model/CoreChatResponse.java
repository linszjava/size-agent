package com.size.model;

public record CoreChatResponse(
        String content,
        String model,
        Long promptTokens,
        Long completionTokens,
        Long totalTokens,
        String finishReason
) {
}
