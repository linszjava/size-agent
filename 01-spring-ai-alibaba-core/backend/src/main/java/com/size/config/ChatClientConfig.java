package com.size.config;

import com.alibaba.cloud.ai.dashscope.agent.DashScopeAgent;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ChatClientConfig {

    /*ChatClient 是 Spring AI 的统一调用门面，DashScopeChatModel 是阿里云百炼的具体模型实现。
    你可以把自动配置生成的 DashScopeChatModel 注入进来，再创建 ChatClient*/

    @Value("${app.ai.system-prompt}")
    private String systemPrompt;

    /**
     * 方式一：使用 Spring Boot 自动配置的 ChatClient.Builder。
     * Builder 内部已经关联自动配置的 DashScopeChatModel。
     */
    @Primary
    @Bean("chatClient")
    public ChatClient dashScopeChatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem(systemPrompt)
                .build();

    }

    @Bean("coreChatClient")
    public ChatClient coreChatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem(systemPrompt)
                .defaultOptions(DashScopeChatOptions.builder()
                        .temperature(0.7)
                        .maxToken(1000)
                        .topP(0.8)
                        .build())
                .build();
    }



}
