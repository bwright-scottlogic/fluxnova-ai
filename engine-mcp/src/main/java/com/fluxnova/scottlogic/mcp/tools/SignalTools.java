package com.fluxnova.scottlogic.mcp.tools;

import org.finos.fluxnova.bpm.engine.RuntimeService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.Map;

public class SignalTools {

    private final RuntimeService runtimeService;

    public SignalTools(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    @Tool(description = "Broadcast a signal event to all process instances waiting on that signal name.")
    public void broadcastSignal(
            @ToolParam(description = "The signal name as defined in the BPMN model") String signalName,
            @ToolParam(description = "Optional variables to pass with the signal", required = false) Map<String, Object> variables) {

        if (variables != null && !variables.isEmpty()) {
            runtimeService.signalEventReceived(signalName, variables);
        } else {
            runtimeService.signalEventReceived(signalName);
        }
    }

    @Tool(description = "Send a signal event to a specific execution (process instance execution) by its execution ID.")
    public void sendSignalToExecution(
            @ToolParam(description = "The signal name as defined in the BPMN model") String signalName,
            @ToolParam(description = "The execution ID of the specific process instance execution to signal") String executionId,
            @ToolParam(description = "Optional variables to pass with the signal", required = false) Map<String, Object> variables) {

        if (variables != null && !variables.isEmpty()) {
            runtimeService.signalEventReceived(signalName, executionId, variables);
        } else {
            runtimeService.signalEventReceived(signalName, executionId);
        }
    }
}
