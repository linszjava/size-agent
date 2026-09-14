package com.size.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * 企业级 AI 能力的统一预算配置。
 *
 * <p>把限制集中在配置对象中，避免超时、轮数、输入长度等数字散落在业务代码里。</p>
 */
@Validated
@ConfigurationProperties(prefix = "app.ai.engineering")
public record AiEngineeringProperties(
        @NotNull Duration requestTimeout,
        @Min(1) @Max(100_000) int maxInputCharacters,
        @Min(1) @Max(100) int maxAgentRounds,
        @Min(1) @Max(100) int maxRagResults) {
}
