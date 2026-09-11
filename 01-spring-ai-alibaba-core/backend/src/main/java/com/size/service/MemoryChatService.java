package com.size.service;

import cn.hutool.core.util.StrUtil;
import com.size.model.memory.MemoryChatRequest;
import com.size.model.memory.MemoryMessageDto;
import com.size.tools.MemoryChatClientTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@Service
public class MemoryChatService {

    private final ChatMemory chatMemory;

    private final ChatClient memoryChatClient;

    public MemoryChatService(ChatMemory chatMemory, @Qualifier("memoryChatClient") ChatClient memoryChatClient) {
        this.chatMemory = chatMemory;
        this.memoryChatClient = memoryChatClient;
    }

    public String chat (String userId, MemoryChatRequest request) {
        return memoryChatClient.prompt()
                .user(StrUtil.trim(request.message()))
                .advisors(advisorSpec ->
                        advisorSpec.param(ChatMemory.CONVERSATION_ID,
                                MemoryChatClientTool.memoryKey(userId, request.conversationId())))
                .call()
                .content();
    }


    public Flux<String> stream(String userId, MemoryChatRequest request) {
        return memoryChatClient.prompt()
                .user(StrUtil.trim(request.message()))
                .advisors(advisor -> advisor.param(
                        ChatMemory.CONVERSATION_ID,
                        MemoryChatClientTool.memoryKey(userId, request.conversationId())))
                .stream()
                .content();
    }

    public List<MemoryMessageDto> history(String userId, String conversationId) {
        return chatMemory.get(MemoryChatClientTool.memoryKey(userId, conversationId)).stream()
                .map(message -> new MemoryMessageDto(
                        message.getMessageType().name(),
                        message.getText()))
                .toList();
    }

    public void clear(String userId, String conversationId) {
        chatMemory.clear(MemoryChatClientTool.memoryKey(userId, conversationId));
    }


}
