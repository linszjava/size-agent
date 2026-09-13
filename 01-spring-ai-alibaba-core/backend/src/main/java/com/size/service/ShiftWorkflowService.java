package com.size.service;


import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.size.exception.ToolOperationException;
import com.size.model.graph.ShiftWorkflowRequest;
import com.size.model.graph.ShiftWorkflowResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ShiftWorkflowService {

    private final CompiledGraph shiftSwapWorkflow;

    public ShiftWorkflowService(
            @Qualifier("shiftSwapWorkflow") CompiledGraph shiftSwapWorkflow) {
        this.shiftSwapWorkflow = shiftSwapWorkflow;
    }

    public ShiftWorkflowResponse start(
            String employeeId,
            ShiftWorkflowRequest request) {
        String workflowId = IdUtil.fastSimpleUUID();
        RunnableConfig config = workflowConfig(employeeId, workflowId);

        Map<String, Object> inputs = Map.of(
                "employeeId", employeeId,
                "shiftDate", request.shiftDate(),
                "targetShift", StrUtil.trim(request.targetShift()),
                "reason", StrUtil.trim(request.reason()),
                "approved", false
        );

        OverAllState state = shiftSwapWorkflow.invoke(inputs, config)
                .orElseThrow(() ->
                        new ToolOperationException("换班工作流没有返回状态"));
        return response(workflowId, state);
    }

    public ShiftWorkflowResponse approve(
            String employeeId,
            String workflowId,
            boolean approved) {
        try {
            RunnableConfig config = workflowConfig(employeeId, workflowId);
            RunnableConfig updatedConfig = shiftSwapWorkflow.updateState(
                    config,
                    Map.of("approved", approved),
                    null
            );

            OverAllState state = shiftSwapWorkflow
                    .invoke((Map<String, Object>) null,
                            updatedConfig.withResume())
                    .orElseThrow(() ->
                            new ToolOperationException("换班工作流恢复失败"));
            return response(workflowId, state);
        }
        catch (ToolOperationException exception) {
            throw exception;
        }
        catch (Exception exception) {
            throw new ToolOperationException(
                    "工作流不存在、已经结束或恢复失败：" + exception.getMessage());
        }
    }

    private RunnableConfig workflowConfig(
            String employeeId,
            String workflowId) {
        return RunnableConfig.builder()
                .threadId(DigestUtil.sha256Hex(
                        employeeId + "\0" + workflowId))
                .build();
    }

    private ShiftWorkflowResponse response(
            String workflowId,
            OverAllState state) {
        return new ShiftWorkflowResponse(
                workflowId,
                state.value("status", "UNKNOWN"),
                state.value("currentShift", ""),
                state.value("targetShift", ""),
                state.value("policy", ""),
                state.value("requestId", ""),
                state.value("message", "")
        );
    }
}
