package com.size.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.size.exception.ModelCallException;
import com.size.model.agent.AgentChatRequest;
import com.size.model.agent.AgentChatResponse;
import com.size.tools.EmployeeAssistantTool;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class EmployeeAgentService {

    private final ReactAgent reactAgent;

    public EmployeeAgentService(@Qualifier("employeeReactAgent")ReactAgent reactAgent) {
        this.reactAgent = reactAgent;
    }

    public AgentChatResponse chat(String employeeId, AgentChatRequest request) {

        /* RunnableConfig 是本次 Agent 运行的服务端配置 */
        RunnableConfig config = RunnableConfig.builder()
                .threadId(this.threadId(employeeId, request.conversationId()))
                .addMetadata(EmployeeAssistantTool.EMPLOYEE_ID, employeeId)
                .build();
        try {
            AssistantMessage response = reactAgent.call(StrUtil.trim(request.message()), config);
//            Flux<NodeOutput> outputFlux = reactAgent.stream(StrUtil.trim(request.message()), config);
            return new AgentChatResponse(request.conversationId(),response.getText());
        } catch (GraphRunnerException e) {
            throw new ModelCallException("gent 执行失败，请稍后重试",e);
        }
    }

    private String threadId(String employeeId, String conversationId) {
        return DigestUtil.sha256Hex(employeeId +"\0" + conversationId);
    }


}
