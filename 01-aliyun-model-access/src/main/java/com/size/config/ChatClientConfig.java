package com.size.config;

import com.alibaba.cloud.ai.dashscope.agent.DashScopeAgent;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
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

    /**
     * 方式二：直接依赖 Spring AI Alibaba 的 DashScopeChatModel 实现。
     */
    @Bean("chatClientAli")
    public ChatClient dashScopeChatClientAli(DashScopeChatModel model) {
        return ChatClient.builder(model)
                .defaultSystem(systemPrompt)
                .build();
    }

    /**
     * 方式三：依赖 Spring AI 通用的 ChatModel 接口。
     * 当前注入的实际实现仍然是 DashScopeChatModel。
     */
    @Bean("chatClientGeneral")
    public ChatClient dashScopeChatClientGeneral(ChatModel model) {
        return ChatClient.builder(model)
                .defaultSystem(systemPrompt)
                .build();
    }

}
