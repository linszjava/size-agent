package com.size.chapter12.a2a.server;

import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@SpringBootApplication
public class HrA2aServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(HrA2aServerApplication.class, args);
    }

    @Bean
    @Primary
    Agent rootAgent(ChatModel chatModel) throws GraphStateException {
        return ReactAgent.builder()
                .name("hr-specialist")
                .description("回答企业人事制度问题的远程专家 Agent")
                .model(chatModel)
                .instruction("""
                        你是企业人事制度专家。请用中文准确回答。
                        当前演示规则：正式员工每年享有 10 天年假；最多结转 5 天到下一年度。
                        超出演示规则的问题必须明确说明资料不足，不得编造。
                        """)
                // 使用独立输出键，使 A2A artifact 只携带最终 AssistantMessage，
                // 而不是把整段 messages 历史序列化后返回。
                .outputKey("answer")
                .build();
    }
}
