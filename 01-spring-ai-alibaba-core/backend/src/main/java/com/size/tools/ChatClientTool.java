package com.size.tools;

import cn.hutool.crypto.digest.DigestUtil;
import com.size.model.ChatRequest;
import com.size.model.ChatResponseDto;
import org.springframework.ai.chat.model.ChatResponse;

public class ChatClientTool {

    public static ChatResponseDto toChatResponseDto(ChatResponse chatResponse) {
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
