package com.fluxnova.scottlogic.mcp.tools;

import org.finos.fluxnova.bpm.engine.ExternalTaskService;
import org.finos.fluxnova.bpm.engine.externaltask.ExternalTask;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ExternalTaskTools {

    private final ExternalTaskService externalTaskService;

    public ExternalTaskTools(ExternalTaskService externalTaskService) {
        this.externalTaskService = externalTaskService;
    }

    @Tool(description = "List open external tasks, optionally filtered by topic name or process instance ID.")
    public List<Map<String, String>> listExternalTasks(
            @ToolParam(description = "Optional topic name to filter by", required = false) String topicName,
            @ToolParam(description = "Optional process instance ID to filter by", required = false) String processInstanceId) {

        var query = externalTaskService.createExternalTaskQuery();
        if (topicName != null && !topicName.isBlank()) {
            query.topicName(topicName);
        }
        if (processInstanceId != null && !processInstanceId.isBlank()) {
            query.processInstanceId(processInstanceId);
        }

        return query.list().stream()
                .map(t -> Map.of(
                        "id", t.getId(),
                        "topicName", t.getTopicName() != null ? t.getTopicName() : "",
                        "workerId", t.getWorkerId() != null ? t.getWorkerId() : "",
                        "processInstanceId", t.getProcessInstanceId() != null ? t.getProcessInstanceId() : "",
                        "processDefinitionKey", t.getProcessDefinitionKey() != null ? t.getProcessDefinitionKey() : "",
                        "retries", t.getRetries() != null ? String.valueOf(t.getRetries()) : "",
                        "errorMessage", t.getErrorMessage() != null ? t.getErrorMessage() : ""))
                .collect(Collectors.toList());
    }

    @Tool(description = "Complete an external task on behalf of a worker, optionally providing output variables.")
    public void completeExternalTask(
            @ToolParam(description = "The external task ID to complete") String externalTaskId,
            @ToolParam(description = "The worker ID that locked and is completing the task") String workerId,
            @ToolParam(description = "Optional variables to set on the process instance", required = false) Map<String, Object> variables) {

        if (variables != null && !variables.isEmpty()) {
            externalTaskService.complete(externalTaskId, workerId, variables);
        } else {
            externalTaskService.complete(externalTaskId, workerId);
        }
    }

    @Tool(description = "Report a failure for an external task. If retries reach 0 an incident is created.")
    public void handleExternalTaskFailure(
            @ToolParam(description = "The external task ID") String externalTaskId,
            @ToolParam(description = "The worker ID that attempted the task") String workerId,
            @ToolParam(description = "Short error message describing the failure") String errorMessage,
            @ToolParam(description = "Number of retries remaining (set to 0 to create an incident)") int retries,
            @ToolParam(description = "Milliseconds to wait before the task can be fetched again") long retryTimeout) {

        externalTaskService.handleFailure(externalTaskId, workerId, errorMessage, retries, retryTimeout);
    }

    @Tool(description = "Raise a BPMN error from an external task, propagating it to the nearest error boundary event.")
    public void handleExternalTaskBpmnError(
            @ToolParam(description = "The external task ID") String externalTaskId,
            @ToolParam(description = "The worker ID that is raising the error") String workerId,
            @ToolParam(description = "The BPMN error code as defined in the process model") String errorCode,
            @ToolParam(description = "Optional human-readable error message", required = false) String errorMessage) {

        if (errorMessage != null && !errorMessage.isBlank()) {
            externalTaskService.handleBpmnError(externalTaskId, workerId, errorCode, errorMessage);
        } else {
            externalTaskService.handleBpmnError(externalTaskId, workerId, errorCode);
        }
    }

    @Tool(description = "Unlock an external task so it can be fetched again by workers.")
    public void unlockExternalTask(
            @ToolParam(description = "The external task ID to unlock") String externalTaskId) {

        externalTaskService.unlock(externalTaskId);
    }

    @Tool(description = "Set the number of retries for an external task. Setting retries to 0 creates an incident; setting above 0 resolves an existing incident.")
    public void setExternalTaskRetries(
            @ToolParam(description = "The external task ID") String externalTaskId,
            @ToolParam(description = "The new number of retries (must be >= 0)") int retries) {

        externalTaskService.setRetries(externalTaskId, retries);
    }

    @Tool(description = "Get the full error details (stack trace) for a failed external task.")
    public String getExternalTaskErrorDetails(
            @ToolParam(description = "The external task ID") String externalTaskId) {

        String details = externalTaskService.getExternalTaskErrorDetails(externalTaskId);
        return details != null ? details : "";
    }
}
