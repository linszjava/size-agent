package com.size.service;

import cn.hutool.core.util.StrUtil;
import com.size.exception.ModelCallException;
import com.size.model.ChatResponseDto;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private final ChatClient chatClient;

    public ChatService(@Qualifier("chatClientAli") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public ChatResponseDto chat(String message) {

        try {
            ChatResponse chatResponse = chatClient.prompt()
                    .user(StrUtil.trim(message))
                    .call()
//                .chatClientResponse()
                    .chatResponse();
            /* ChatClientResponse & ChatResponse 的区别 */
        /*ChatResponse：包含模型回答、Token 使用量、模型名称等。
          ChatClientResponse：除了 ChatResponse，还包含 Advisor 执行上下文，例如后续 RAG 检索到的文档。*/

            /* 加点判断 */
            if (chatResponse == null || chatResponse.getResult() == null) {
                throw new ModelCallException("模型没有返回有效结果",null);
            }

            ChatResponseDto chatResponseDto = toChatResponseDto(chatResponse);
            return chatResponseDto;

            /* 健壮性 */
        }catch (ModelCallException exception) {
            throw exception;
        }
    catch (RuntimeException exception) {
            throw new ModelCallException(
                    "模型服务暂时不可用，请稍后重试",
                    exception
            );
        }
    }

    private static ChatResponseDto toChatResponseDto(ChatResponse chatResponse) {
        String content = chatResponse.getResult().getOutput().getText();

        String model = chatResponse.getMetadata().getModel();

        long promptTokens = toLong(chatResponse.getMetadata().getUsage().getPromptTokens());

        long completionTokens = toLong(chatResponse.getMetadata().getUsage().getCompletionTokens());
        long totalTokens = toLong(chatResponse.getMetadata().getUsage().getTotalTokens());

        String finishReason = chatResponse.getResult().getMetadata().getFinishReason();


        ChatResponseDto chatResponseDto = new ChatResponseDto(content,model,promptTokens,
                completionTokens,totalTokens,finishReason);
        return chatResponseDto;
    }

    private static Long toLong(Integer value) {
        return value == null ? null : value.longValue();
    }


}
