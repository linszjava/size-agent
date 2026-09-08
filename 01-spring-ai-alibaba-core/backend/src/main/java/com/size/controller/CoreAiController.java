package com.size.controller;

import com.size.model.ChatResponseDto;
import com.size.model.CoreChatRequest;
import com.size.service.CoreAiService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/core")
public class CoreAiController {

    private final CoreAiService coreAiService;

    public CoreAiController(CoreAiService coreAiService) {
        this.coreAiService = coreAiService;
    }

    @PostMapping("/chat-model")
    public ChatResponseDto chatModel(@Valid @RequestBody CoreChatRequest request) {
        return coreAiService.callWithChatModel(request);
    }

    @PostMapping("/chat-client")
    public ChatResponseDto chatClient(@Valid @RequestBody CoreChatRequest request) {
        return coreAiService.callWithChatClient(request);
    }

    @PostMapping("/messages")
    public ChatResponseDto messages(@Valid @RequestBody CoreChatRequest request) {
        return coreAiService.callWithMessageHistory(request);
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@Valid @RequestBody CoreChatRequest request) {
        return coreAiService.stream(request);
    }
}
