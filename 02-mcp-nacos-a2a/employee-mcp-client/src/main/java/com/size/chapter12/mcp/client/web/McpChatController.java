package com.size.chapter12.mcp.client.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/mcp")
public class McpChatController {

    private final ChatClient chatClient;

    public McpChatController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @PostMapping("/chat")
    public Map<String, String> chat(@Valid @RequestBody ChatRequest request) {
        String content = chatClient.prompt().user(request.message()).call().content();
        return Map.of("content", content == null ? "" : content);
    }

    public record ChatRequest(@NotBlank String message) {
    }
}
