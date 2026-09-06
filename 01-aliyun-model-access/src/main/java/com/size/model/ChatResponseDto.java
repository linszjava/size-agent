package com.size.model;

public record ChatResponseDto(
        String content,
        String model,
        Long promptTokens,
        Long completionTokens,
        Long totalTokens,
        String finishReason
) {
}