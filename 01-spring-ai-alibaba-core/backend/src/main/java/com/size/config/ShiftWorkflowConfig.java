package com.size.config;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

@Configuration
public class ShiftWorkflowConfig {

    @Bean("shiftSwapWorkflow")
    public CompiledGraph ShiftSwapWorkflow() throws GraphStateException {

        KeyStrategyFactory keyStrategies = () -> {
            Map<String, KeyStrategy> strategies = new HashMap<>();
            strategies.put("employeeId", KeyStrategy.REPLACE);
            strategies.put("shiftDate", KeyStrategy.REPLACE);
            strategies.put("targetShift", KeyStrategy.REPLACE);
            strategies.put("reason", KeyStrategy.REPLACE);
            strategies.put("currentShift", KeyStrategy.REPLACE);
            strategies.put("policy", KeyStrategy.REPLACE);
            strategies.put("approved", KeyStrategy.REPLACE);
            strategies.put("requestId", KeyStrategy.REPLACE);
            strategies.put("status", KeyStrategy.REPLACE);
            strategies.put("message", KeyStrategy.REPLACE);
            return strategies;
        };


        StateGraph graph = new StateGraph("shift-swap-workflow", keyStrategies)
                .addNode("query_schedule", node_async(state -> {
                    String shiftDate = state.value("shiftDate", "");
                    LocalDate date = LocalDate.parse(shiftDate);
                    String currentShift = date.getDayOfMonth() % 2 == 0 ? "白班" : "晚班";
                    return Map.of(
                            "currentShift", currentShift,
                            "status", "SCHEDULE_QUERIED"
                    );
                }))
                .addNode("check_policy", node_async(state -> Map.of(
                        "policy", "换班申请须由员工本人确认，并由直属主管审批后才能生效。",
                        "status", "POLICY_CHECKED"
                )))
                .addNode("prepare_request", node_async(state -> Map.of(
                        "status", "WAITING_EMPLOYEE_CONFIRMATION",
                        "message", "换班信息已准备，请员工确认是否提交"
                )))
                .addNode("human_approval", node_async(state -> {
                    boolean approved = state.value("approved", false);
                    return Map.of(
                            "status", approved ? "EMPLOYEE_CONFIRMED" : "REJECTED",
                            "message", approved ? "员工已确认，继续提交" : "员工已拒绝，本次流程结束"
                    );
                }))
                .addNode("update_schedule", node_async(state -> Map.of(
                        "requestId", "SWAP-" + UUID.randomUUID(),
                        "status", "SCHEDULE_UPDATED",
                        "message", "换班申请已提交，排班已更新"
                )))
                .addNode("send_notification", node_async(state -> Map.of(
                        "status", "COMPLETED",
                        "message", "排班更新完成，通知已发送"
                )))
                .addNode("rejected", node_async(state -> Map.of(
                        "status", "REJECTED",
                        "message", "员工未确认，未修改排班"
                )))
                .addEdge(StateGraph.START, "query_schedule")
                .addEdge("query_schedule", "check_policy")
                .addEdge("check_policy", "prepare_request")
                .addEdge("prepare_request", "human_approval")
                .addConditionalEdges(
                        "human_approval",
                        edge_async(state -> state.value("approved", false)
                                ? "approved"
                                : "rejected"),
                        Map.of(
                                "approved", "update_schedule",
                                "rejected", "rejected"
                        )
                )
                .addEdge("update_schedule", "send_notification")
                .addEdge("send_notification", END)
                .addEdge("rejected", END);

        CompileConfig config = CompileConfig.builder()
                .saverConfig(SaverConfig.builder()
                        .register(MemorySaver.builder().build()).build())
                .interruptBefore("human_approval")
                .recursionLimit(20)
                .build();

        return graph.compile(config);
    }
}
