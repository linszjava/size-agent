package com.size.chapter12.mcp.server.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;

@Service
public class EmployeeScheduleTools {

    private static final Map<String, String> DEMO_SHIFTS = Map.of(
            "E1001", "09:00-18:00 白班",
            "E1002", "18:00-次日02:00 晚班",
            "E1003", "今日休息"
    );

    @Tool(description = "查询指定员工在指定日期的排班。仅返回演示排班数据。")
    public ScheduleResult queryEmployeeSchedule(
            @ToolParam(description = "员工编号，例如 E1001") String employeeId,
            @ToolParam(description = "日期，ISO-8601 格式，例如 2026-09-14") String date) {
        LocalDate parsedDate = LocalDate.parse(date);
        String shift = DEMO_SHIFTS.getOrDefault(employeeId, "没有找到该员工的排班");
        return new ScheduleResult(employeeId, parsedDate, shift);
    }

    public record ScheduleResult(String employeeId, LocalDate date, String shift) {
    }
}
