package com.size.controller;

import com.size.model.MemoryChatRequest;
import com.size.model.MemoryMessageDto;
import com.size.service.MemoryChatService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@Validated
@RequestMapping("/api/memory")
@RestController
public class MemoryChatController {

    public static final String ID_PATTERN = "[A-Za-z0-9_-]{1,64}";

    private final MemoryChatService memoryChatService;

    public MemoryChatController(MemoryChatService memoryChatService) {
        this.memoryChatService = memoryChatService;
    }

    /* X-User-Id 只是为了让本地教程容易演示两个用户。客户端可以伪造 Header，因此生产环境不能使用这种方式识别用户。
    生产环境应从 Spring Security 登录上下文取得不可伪造的用户 ID。 */

    @PostMapping("/chat")
    public Map<String, String> chat(
            @RequestHeader("X-User-Id") @NotBlank @Pattern(regexp = ID_PATTERN) String userId,
            @Valid @RequestBody MemoryChatRequest request) {
        return Map.of(
                "conversationId", request.conversationId(),
                "content", memoryChatService.chat(userId, request));
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(
            @RequestHeader("X-User-Id") @NotBlank @Pattern(regexp = ID_PATTERN) String userId,
            @Valid @RequestBody MemoryChatRequest request) {
        return memoryChatService.stream(userId, request);
    }

    @GetMapping("/conversations/{conversationId}")
    public List<MemoryMessageDto> history(
            @RequestHeader("X-User-Id") @NotBlank @Pattern(regexp = ID_PATTERN) String userId,
            @PathVariable @Pattern(regexp = ID_PATTERN) String conversationId) {
        return memoryChatService.history(userId, conversationId);
    }

    @DeleteMapping("/conversations/{conversationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clear(
            @RequestHeader("X-User-Id") @NotBlank @Pattern(regexp = ID_PATTERN) String userId,
            @PathVariable @Pattern(regexp = ID_PATTERN) String conversationId) {
        memoryChatService.clear(userId, conversationId);
    }
}
