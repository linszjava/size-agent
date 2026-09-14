package com.size.chapter12.mcp.client.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class McpChatConfig {

    @Bean
    ChatClient employeeMcpChatClient(
            ChatClient.Builder builder,
            @Qualifier("distributedAsyncToolCallback") ToolCallbackProvider mcpTools) {
        return builder
                .defaultSystem("你是员工排班助手。涉及排班事实时必须调用 MCP 工具，不得编造。")
                .defaultToolCallbacks(mcpTools.getToolCallbacks())
                .build();
    }
}
