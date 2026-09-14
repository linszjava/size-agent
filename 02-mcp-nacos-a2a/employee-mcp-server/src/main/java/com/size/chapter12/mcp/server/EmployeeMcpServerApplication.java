package com.size.chapter12.mcp.server;

import com.size.chapter12.mcp.server.tool.EmployeeScheduleTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class EmployeeMcpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmployeeMcpServerApplication.class, args);
    }

    @Bean
    ToolCallbackProvider employeeTools(EmployeeScheduleTools tools) {
        return MethodToolCallbackProvider.builder().toolObjects(tools).build();
    }
}
