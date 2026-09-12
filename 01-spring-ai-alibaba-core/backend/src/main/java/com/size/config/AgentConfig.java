package com.size.config;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.modelcalllimit.ModelCallLimitHook;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.size.tools.EmployeeAssistantTool;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class AgentConfig {

    @Bean
    public ReactAgent employeeReactAgent(
            ChatModel chatModel,
            EmployeeAssistantTool employeeAssistantTool
    ) {
        return ReactAgent.builder()
                .name("employee_assistant")
                .model(chatModel)
                .systemPrompt("""
                        你是企业员工助理，可以查询本人排班、解释企业制度、准备换班申请。
                        遇到需要实时数据或业务操作的问题时，必须调用工具，不得编造。
                        员工身份只能来自服务端运行上下文，不得要求用户提供或修改 employeeId。
                        换班工具只能准备待确认申请，不能代替用户最终提交。
                        任务完成后直接给出简洁结果，不向用户展示内部思考过程。
                        """)
                .methodTools(employeeAssistantTool)
                .hooks(ModelCallLimitHook.builder().runLimit(5).build())  //  在Agent/模型/工具执行前后插入横切逻辑
                .saver(new MemorySaver())
                .enableLogging(true)
                .build();



    }
}
