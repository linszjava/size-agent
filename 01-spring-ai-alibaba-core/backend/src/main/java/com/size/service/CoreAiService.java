package com.size.service;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.size.exception.ModelCallException;
import com.size.model.ChatResponseDto;
import com.size.model.CoreChatRequest;
import com.size.model.CoreChatResponse;
import com.size.tools.ChatClientTool;
import jakarta.validation.constraints.NotBlank;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@Service
public class CoreAiService {

    private final ChatModel chatModel;

    private final ChatClient chatClient;


    private final String defaultSystemPrompt;

    public CoreAiService(ChatModel chatModel, ChatClient chatClient,
                         @Value("${app.ai.system-prompt}") String defaultSystemPrompt) {
        this.chatModel = chatModel;
        this.chatClient = chatClient;
        this.defaultSystemPrompt = defaultSystemPrompt;
    }


    public ChatResponseDto callWithChatModel(CoreChatRequest coreChatRequest) {

        List<Message> message = List.of(
                new SystemMessage(resolveSystemPrompt(coreChatRequest.systemPrompt())),
                new UserMessage(StrUtil.trim(coreChatRequest.message()))
        );
        Prompt prompt = new Prompt(message,createOptions(coreChatRequest));
        return toResponse(chatModel.call(prompt));
    }

    public ChatResponseDto callWithChatClient(CoreChatRequest request) {
        ChatResponse response = chatClient.prompt()
                .system(resolveSystemPrompt(request.systemPrompt()))
                .user(StrUtil.trim(request.message()))
                .options(createOptions(request))
                .call()
                .chatResponse();
        return toResponse(response);
    }

    public ChatResponseDto callWithMessageHistory(CoreChatRequest request) {
        List<Message> messages = List.of(
                new SystemMessage(resolveSystemPrompt(request.systemPrompt())),
                new UserMessage("我正在学习 Spring AI Alibaba。"),
                new AssistantMessage("好的，我会结合 Spring AI Alibaba 进行讲解。"),
                new UserMessage(StrUtil.trim(request.message()))
        );
        return toResponse(chatModel.call(new Prompt(messages, createOptions(request))));
    }

    public Flux<String> stream(CoreChatRequest request) {
        return chatClient.prompt()
                .system(resolveSystemPrompt(request.systemPrompt()))
                .user(StrUtil.trim(request.message()))
                .options(createOptions(request))
                .stream()
                .content();
    }


    private String resolveSystemPrompt(String systemPrompt) {
        return StrUtil.blankToDefault(systemPrompt, defaultSystemPrompt);
    }

    /* 塞一些 chat Options 参数 */
    private ChatOptions createOptions(CoreChatRequest coreChatRequest) {

        DashScopeChatOptions.DashScopeChatOptionsBuilder builder = DashScopeChatOptions.builder()
                .multiModel(true)
                .temperature(ObjectUtil.defaultIfNull(coreChatRequest.temperature(), 0.7))
                .topP(ObjectUtil.defaultIfNull(coreChatRequest.topP(), 0.8))
                .maxToken(ObjectUtil.defaultIfNull(coreChatRequest.maxTokens(), 1000));

        if (ObjectUtil.isNotEmpty(coreChatRequest.stopSequences())) {
            List<Object> stopList = new ArrayList<>(coreChatRequest.stopSequences());
            builder.stop(stopList);
        }
        return builder.build();
    }
    private ChatResponseDto toResponse(ChatResponse  response) {

        if (ObjectUtil.isNull(response)|| ObjectUtil.isNull(response.getResult())) {
            throw new IllegalStateException("模型没有返回有效结果");
        }

        return ChatClientTool.toChatResponseDto(response);
    }
}
