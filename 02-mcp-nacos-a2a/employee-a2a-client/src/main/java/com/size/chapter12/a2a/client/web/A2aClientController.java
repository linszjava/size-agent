package com.size.chapter12.a2a.client.web;

import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/a2a")
public class A2aClientController {

    private final Agent hrRemoteAgent;

    public A2aClientController(Agent hrRemoteAgent) {
        this.hrRemoteAgent = hrRemoteAgent;
    }

    @GetMapping("/hr")
    public Map<String, String> askHr(@RequestParam String question) throws GraphRunnerException {
        RunnableConfig config = RunnableConfig.builder()
                // HTTP 请求彼此隔离，避免框架默认 threadId 累积上一次问题。
                .threadId("a2a-http-" + UUID.randomUUID())
                .build();
        String answer = hrRemoteAgent.invoke(question, config)
                .orElseThrow(() -> new IllegalStateException("远程 Agent 没有返回状态"))
                .value("answer")
                .map(String::valueOf)
                .filter(value -> !value.isBlank())
                .orElseThrow(() -> new IllegalStateException("远程 Agent 没有返回回答"));
        return Map.of("content", extractTextContent(answer));
    }

    private String extractTextContent(String value) {
        // 1.1.2.2 的非流式 A2A 执行器会对 AssistantMessage 调用 String.valueOf；
        // 在适配器修复前，从该稳定边界中取出真正正文。
        String marker = "textContent=";
        int start = value.lastIndexOf(marker);
        if (start < 0) {
            return value;
        }
        start += marker.length();
        int end = value.indexOf(", metadata=", start);
        return end < 0 ? value.substring(start) : value.substring(start, end);
    }
}
