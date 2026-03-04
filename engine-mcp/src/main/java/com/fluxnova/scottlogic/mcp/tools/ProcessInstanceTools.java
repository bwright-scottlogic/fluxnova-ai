package com.fluxnova.scottlogic.mcp.tools;

import org.finos.fluxnova.bpm.engine.RuntimeService;
import org.finos.fluxnova.bpm.engine.runtime.ProcessInstance;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ProcessInstanceTools {

    private final RuntimeService runtimeService;

    public ProcessInstanceTools(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    @Tool(description = "Start a new process instance by its process definition key. Returns the new process instance ID.")
    public String startProcessInstance(
            @ToolParam(description = "The key of the process definition to start (e.g. 'invoice-approval')") String processDefinitionKey,
            @ToolParam(description = "Optional initial process variables as a key/value map", required = false) Map<String, Object> variables) {

        ProcessInstance instance = (variables != null && !variables.isEmpty())
                ? runtimeService.startProcessInstanceByKey(processDefinitionKey, variables)
                : runtimeService.startProcessInstanceByKey(processDefinitionKey);

        return instance.getId();
    }

    @Tool(description = "List active process instance IDs, optionally filtered by process definition key.")
    public List<Map<String, String>> listActiveProcessInstances(
            @ToolParam(description = "Optional process definition key to filter by", required = false) String processDefinitionKey) {

        var query = runtimeService.createProcessInstanceQuery().active();
        if (processDefinitionKey != null && !processDefinitionKey.isBlank()) {
            query.processDefinitionKey(processDefinitionKey);
        }

        return query.list().stream()
                .map(pi -> Map.of(
                        "id", pi.getId(),
                        "processDefinitionId", pi.getProcessDefinitionId(),
                        "businessKey", pi.getBusinessKey() != null ? pi.getBusinessKey() : ""))
                .collect(Collectors.toList());
    }

    @Tool(description = "Get the current variables of a running process instance.")
    public Map<String, Object> getProcessInstanceVariables(
            @ToolParam(description = "The process instance ID") String processInstanceId) {

        return runtimeService.getVariables(processInstanceId);
    }

    @Tool(description = "Set a variable on a running process instance.")
    public void setProcessInstanceVariable(
            @ToolParam(description = "The process instance ID") String processInstanceId,
            @ToolParam(description = "The variable name") String variableName,
            @ToolParam(description = "The variable value") Object value) {

        runtimeService.setVariable(processInstanceId, variableName, value);
    }

    @Tool(description = "Correlate a message to a waiting process instance, resuming it at a message catch event.")
    public void correlateMessage(
            @ToolParam(description = "The message name defined in the BPMN model") String messageName,
            @ToolParam(description = "Optional business key to narrow the correlation target", required = false) String businessKey,
            @ToolParam(description = "Optional variables to pass with the message", required = false) Map<String, Object> variables) {

        var builder = runtimeService.createMessageCorrelation(messageName);
        if (businessKey != null && !businessKey.isBlank()) {
            builder.processInstanceBusinessKey(businessKey);
        }
        if (variables != null && !variables.isEmpty()) {
            builder.setVariables(variables);
        }
        builder.correlate();
    }

    @Tool(description = "Delete a running process instance by ID.")
    public void deleteProcessInstance(
            @ToolParam(description = "The process instance ID to delete") String processInstanceId,
            @ToolParam(description = "Human-readable reason for deletion") String deleteReason) {

        runtimeService.deleteProcessInstance(processInstanceId, deleteReason);
    }
}
