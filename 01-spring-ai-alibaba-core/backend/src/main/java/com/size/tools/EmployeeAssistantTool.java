package com.size.tools;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.size.exception.ToolOperationException;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class EmployeeAssistantTool {

    public static final String EMPLOYEE_ID = "employeeId";

    private final Map<String, PendingSwap> pendingSwaps = new ConcurrentHashMap<>();

    /* 查询当前登录员工在指定日期的排班 */
    @Tool(description = "查询当前登录员工在指定日期的排班。只能查询当前登录员工本人，不需要也不允许提供员工编号")
    public ShiftResult getMySchedule(
            @ToolParam(description = "查询日期，格式必须为 yyyy-MM-dd")
            String date,
            ToolContext toolContext
    ){
        String employeeId = requiredEmployeeId(toolContext);
        LocalDate shiftDate = LocalDate.parse(date);
        String shift = shiftDate.getDayOfMonth() % 2 == 0
                ? "09:00-18:00 白班"
                : "13:00-22:00 晚班";
        return new ShiftResult(employeeId,date,shift);
    }

    /* 查询企业内部制度 */
    @Tool(description = "查询企业内部制度。适用于询问请假、调休、换班等制度")
//    @Tool(description = "查询企业内部制度。适用于询问请假、调休、换班等制度",returnDirect = true)
    public PolicyResult searchCompanyPolicy(
            @ToolParam(description = "需要查询的制度关键词") String keyword) {
        String normalizedKeyword = StrUtil.trim(keyword);
        String content = switch (normalizedKeyword) {
            case "换班" -> "换班申请必须由本人发起，并由接班员工和直属主管确认。";
            case "请假" -> "请假应提前提交申请；紧急情况应先联系直属主管。";
            case "调休" -> "调休需有可用调休余额，并在排班确认前提交。";
            default -> "示例数据中没有找到该制度，请联系企业人事部门确认。";
        };
        return new PolicyResult(normalizedKeyword, content);
    }

    /* 准备换班申请 */
    @Tool(description = "为当前登录员工准备换班申请，但不会正式提交。返回确认令牌后，必须由员工通过确认接口完成最终提交")
    public SwapPreparation prepareMyShiftSwap(
            @ToolParam(description = "需要换班的日期，格式必须为 yyyy-MM-dd") String shiftDate,
            @ToolParam(description = "希望更换成的班次，例如白班或晚班") String targetShift,
            @ToolParam(description = "换班原因") String reason,
            ToolContext toolContext) {
        String employeeId = requiredEmployeeId(toolContext);
        LocalDate parsedDate = LocalDate.parse(shiftDate);
        String token = IdUtil.fastSimpleUUID();

        PendingSwap pendingSwap = new PendingSwap(
                token,
                employeeId,
                parsedDate.toString(),
                StrUtil.trim(targetShift),
                StrUtil.trim(reason),
                LocalDateTime.now()
        );
        pendingSwaps.put(token, pendingSwap);

        return new SwapPreparation(
                token,
                "待员工确认",
                "请核对日期、目标班次和原因，然后通过确认接口提交"
        );
    }

    public SwapResult confirmSwap(String employeeId, String confirmationToken) {
        PendingSwap pendingSwap = pendingSwaps.get(confirmationToken);
        if (pendingSwap == null) {
            throw new ToolOperationException("确认令牌不存在或已经使用");
        }
        if (!pendingSwap.employeeId().equals(employeeId)) {
            throw new ToolOperationException("无权确认其他员工的换班申请");
        }

        pendingSwaps.remove(confirmationToken);
        return new SwapResult(
                "SWAP-" + IdUtil.fastSimpleUUID(),
                pendingSwap.employeeId(),
                pendingSwap.shiftDate(),
                pendingSwap.targetShift(),
                "已提交"
        );
    }



    private String requiredEmployeeId(ToolContext toolContext) {
        Object employeeId = toolContext.getContext().get(EMPLOYEE_ID);
        if (employeeId == null || StrUtil.isBlank(employeeId.toString())) {
            throw new IllegalStateException("缺少登录员工身份");
        }
        return employeeId.toString();
    }

    public record ShiftResult(String employeeId, String date, String shift) {
    }

    public record PolicyResult(String keyword, String content) {
    }

    public record SwapPreparation(String confirmationToken, String status, String nextAction) {
    }

    public record SwapResult(
            String requestId,
            String employeeId,
            String shiftDate,
            String targetShift,
            String status
    ) {
    }

    private record PendingSwap(
            String token,
            String employeeId,
            String shiftDate,
            String targetShift,
            String reason,
            LocalDateTime createdAt
    ) {
    }
}


