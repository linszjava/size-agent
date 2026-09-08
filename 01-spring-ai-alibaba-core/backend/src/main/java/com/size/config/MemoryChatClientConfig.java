package com.size.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MemoryChatClientConfig {

    @Value("${app.ai.system-prompt}")
    private String defaultSystem;

    @Bean
    public ChatMemory chatMemory(){
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20) /* maxMessages(20) 表示记忆窗口最多保留 20 条消息，而不是 20 轮对话。
                                    一轮通常至少包含一条 User Message 和一条 Assistant Message*/
                .build();
    }

    @Bean("memoryChatClient")
    public ChatClient memoryChatClient(ChatClient.Builder builder,ChatMemory chatMemory) {
        return builder
                .defaultSystem(defaultSystem)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();


    }
}
