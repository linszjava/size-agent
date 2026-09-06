package com.size.controller;

import cn.hutool.core.util.StrUtil;
import com.size.model.ChatRequest;
import com.size.model.ChatResponseDto;
import com.size.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/v1")
    public ChatResponseDto chat(@RequestBody  @Valid ChatRequest request) {
        return chatService.chat(request.message());
    }
}
