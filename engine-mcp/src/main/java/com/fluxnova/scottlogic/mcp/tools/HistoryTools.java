package com.fluxnova.scottlogic.mcp.tools;

import org.finos.fluxnova.bpm.engine.HistoryService;
import org.finos.fluxnova.bpm.engine.history.HistoricProcessInstance;
import org.finos.fluxnova.bpm.engine.history.HistoricTaskInstance;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HistoryTools {

    private final HistoryService historyService;

    public HistoryTools(HistoryService historyService) {
        this.historyService = historyService;
    }

    @Tool(description = "List completed (historic) process instances, optionally filtered by process definition key.")
    public List<Map<String, String>> listCompletedProcessInstances(
            @ToolParam(description = "Optional process definition key to filter by", required = false) String processDefinitionKey,
            @ToolParam(description = "Maximum number of results to return (defaults to 50)", required = false) Integer maxResults) {

        var query = historyService.createHistoricProcessInstanceQuery().finished();
        if (processDefinitionKey != null && !processDefinitionKey.isBlank()) {
            query.processDefinitionKey(processDefinitionKey);
        }

        int limit = (maxResults != null && maxResults > 0) ? maxResults : 50;

        return query.orderByProcessInstanceEndTime().desc().listPage(0, limit).stream()
                .map(pi -> Map.of(
                        "id", pi.getId(),
                        "processDefinitionKey", pi.getProcessDefinitionKey() != null ? pi.getProcessDefinitionKey() : "",
                        "startTime", pi.getStartTime() != null ? pi.getStartTime().toString() : "",
                        "endTime", pi.getEndTime() != null ? pi.getEndTime().toString() : "",
                        "deleteReason", pi.getDeleteReason() != null ? pi.getDeleteReason() : ""))
                .collect(Collectors.toList());
    }

    @Tool(description = "List all historic tasks for a specific process instance.")
    public List<Map<String, String>> listHistoricTasksForInstance(
            @ToolParam(description = "The process instance ID") String processInstanceId) {

        return historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricTaskInstanceEndTime().asc()
                .list().stream()
                .map(t -> Map.of(
                        "id", t.getId(),
                        "name", t.getName() != null ? t.getName() : "",
                        "assignee", t.getAssignee() != null ? t.getAssignee() : "",
                        "startTime", t.getStartTime() != null ? t.getStartTime().toString() : "",
                        "endTime", t.getEndTime() != null ? t.getEndTime().toString() : ""))
                .collect(Collectors.toList());
    }

    @Tool(description = "Look up a single historic process instance by ID, including its state and duration.")
    public Map<String, String> getHistoricProcessInstance(
            @ToolParam(description = "The process instance ID") String processInstanceId) {

        HistoricProcessInstance pi = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        if (pi == null) {
            return Map.of("error", "No historic process instance found for ID: " + processInstanceId);
        }
        return Map.of(
                "id", pi.getId(),
                "processDefinitionKey", pi.getProcessDefinitionKey() != null ? pi.getProcessDefinitionKey() : "",
                "state", pi.getState() != null ? pi.getState() : "",
                "startTime", pi.getStartTime() != null ? pi.getStartTime().toString() : "",
                "endTime", pi.getEndTime() != null ? pi.getEndTime().toString() : "",
                "durationInMillis", pi.getDurationInMillis() != null ? String.valueOf(pi.getDurationInMillis()) : "");
    }

    @Tool(description = "Get the historic variable values for a completed or running process instance.")
    public Map<String, Object> getHistoricVariables(
            @ToolParam(description = "The process instance ID") String processInstanceId) {

        return historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .list().stream()
                .collect(Collectors.toMap(
                        v -> v.getName(),
                        v -> v.getValue() != null ? v.getValue() : "null"));
    }
}
