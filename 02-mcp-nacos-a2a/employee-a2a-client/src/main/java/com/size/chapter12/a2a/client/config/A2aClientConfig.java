package com.size.chapter12.a2a.client.config;

import com.alibaba.cloud.ai.a2a.registry.nacos.discovery.NacosAgentCardProvider;
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.agent.a2a.AgentCardWrapper;
import com.alibaba.cloud.ai.graph.agent.a2a.A2aRemoteAgent;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class A2aClientConfig {

    @Bean
    Agent hrRemoteAgent(NacosAgentCardProvider agentCardProvider) throws GraphStateException {
        AgentCardWrapper discovered = agentCardProvider.getAgentCard("hr-specialist");
        AgentCard nonStreamingCard = new AgentCard.Builder()
                .name(discovered.name())
                .description(discovered.description())
                .url(discovered.url())
                .provider(discovered.provider())
                .version(discovered.version())
                .documentationUrl(discovered.documentationUrl())
                .capabilities(new AgentCapabilities.Builder().streaming(false).build())
                .defaultInputModes(discovered.defaultInputModes())
                .defaultOutputModes(discovered.defaultOutputModes())
                .skills(discovered.skills())
                .supportsAuthenticatedExtendedCard(discovered.supportsAuthenticatedExtendedCard())
                .securitySchemes(discovered.securitySchemes())
                .security(discovered.security())
                .iconUrl(discovered.iconUrl())
                .additionalInterfaces(discovered.additionalInterfaces())
                .preferredTransport(discovered.preferredTransport())
                .protocolVersion(discovered.protocolVersion())
                .build();

        return A2aRemoteAgent.builder()
                // Card 仍来自 Nacos；这里固定使用非流式模式，规避 1.1.2.2
                // 在流式 completed 事件上丢失最终文本的问题。
                .agentCard(nonStreamingCard)
                .name("hr-specialist")
                .description("通过 A2A 调用远程人事专家")
                .instruction("请用中文回答下面的用户问题：\n{messages}")
                // 远程答案使用独立字段，避免与本地 messages 的追加型状态混在一起。
                .outputKey("answer")
                .shareState(false)
                .build();
    }
}
