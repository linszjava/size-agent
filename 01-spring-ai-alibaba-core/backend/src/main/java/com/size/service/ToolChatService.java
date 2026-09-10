package com.size.service;

import cn.hutool.core.util.StrUtil;
import com.size.exception.ModelCallException;
import com.size.tools.EmployeeAssistantTool;
import com.size.tools.WeatherTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ToolChatService {

    private final ChatClient chatClient;
    private final EmployeeAssistantTool employeeAssistantTool;
    private final WeatherTool weatherTool;
    private final ToolCallback currentTimeTool;

    public ToolChatService(@Qualifier("chatClient") ChatClient chatClient,
                           EmployeeAssistantTool employeeAssistantTool,
                           WeatherTool weatherTool,
                           @Qualifier("currentTimeTool") ToolCallback currentTimeTool) {
        this.chatClient = chatClient;
        this.employeeAssistantTool = employeeAssistantTool;
        this.weatherTool = weatherTool;
        this.currentTimeTool = currentTimeTool;
    }

    public String chat(String employeeId, String message) {
        try {
            return chatClient.prompt()
                    .system("""
                            你是企业员工助手。需要实时数据时必须调用工具，不得编造。
                            当前员工身份只能来自 Tool Context，不得要求用户提供或修改 employeeId。
                            换班工具只能生成待确认申请，必须提醒员工使用确认接口完成最终提交。
                            """)
                    .user(StrUtil.trim(message))
                    .tools(employeeAssistantTool, weatherTool)
                    .toolCallbacks(currentTimeTool)
                    .toolContext(Map.of(
                            EmployeeAssistantTool.EMPLOYEE_ID,
                            employeeId
                    ))
                    .call()
                    .content();
        }
        catch (RuntimeException exception) {
            System.out.println("exception:" + exception);
            throw new ModelCallException("工具对话执行失败，请稍后重试", exception);
        }

    }

    public EmployeeAssistantTool.SwapResult confirmSwap(
            String employeeId,
            String confirmationToken) {
        return employeeAssistantTool.confirmSwap(employeeId, confirmationToken);
    }


}
