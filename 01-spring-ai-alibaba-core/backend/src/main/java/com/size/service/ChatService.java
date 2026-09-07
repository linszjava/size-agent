package com.size.service;

import cn.hutool.core.util.StrUtil;
import com.size.exception.ModelCallException;
import com.size.model.ChatResponseDto;
import com.size.tools.ChatClientTool;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private final ChatClient chatClient;

    public ChatService( ChatClient chatClient) {
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

            System.out.println("===================="+chatResponse);
            ChatResponseDto chatResponseDto = ChatClientTool.toChatResponseDto(chatResponse);
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



}
