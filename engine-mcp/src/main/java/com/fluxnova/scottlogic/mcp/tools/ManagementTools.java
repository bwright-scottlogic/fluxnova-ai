package com.fluxnova.scottlogic.mcp.tools;

import org.finos.fluxnova.bpm.engine.ManagementService;
import org.finos.fluxnova.bpm.engine.runtime.Job;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ManagementTools {

    private final ManagementService managementService;

    public ManagementTools(ManagementService managementService) {
        this.managementService = managementService;
    }

    @Tool(description = "List scheduled/failed jobs in the engine, optionally filtering to only failed jobs (retries = 0).")
    public List<Map<String, String>> listJobs(
            @ToolParam(description = "If true, return only failed jobs with 0 retries remaining", required = false) Boolean failedOnly,
            @ToolParam(description = "Optional process instance ID to filter by", required = false) String processInstanceId) {

        var query = managementService.createJobQuery();
        if (Boolean.TRUE.equals(failedOnly)) {
            query.noRetriesLeft();
        }
        if (processInstanceId != null && !processInstanceId.isBlank()) {
            query.processInstanceId(processInstanceId);
        }

        return query.list().stream()
                .map(j -> Map.of(
                        "id", j.getId(),
                        "processInstanceId", j.getProcessInstanceId() != null ? j.getProcessInstanceId() : "",
                        "processDefinitionKey", j.getProcessDefinitionKey() != null ? j.getProcessDefinitionKey() : "",
                        "dueDate", j.getDuedate() != null ? j.getDuedate().toString() : "",
                        "retries", String.valueOf(j.getRetries()),
                        "exceptionMessage", j.getExceptionMessage() != null ? j.getExceptionMessage() : ""))
                .collect(Collectors.toList());
    }

    @Tool(description = "Force immediate execution of a specific job by its ID, ignoring suspension state.")
    public void executeJob(
            @ToolParam(description = "The job ID to execute") String jobId) {

        managementService.executeJob(jobId);
    }

    @Tool(description = "Delete a job by its ID.")
    public void deleteJob(
            @ToolParam(description = "The job ID to delete") String jobId) {

        managementService.deleteJob(jobId);
    }

    @Tool(description = "Set the number of retries for a job. Use a value > 0 to resolve an incident caused by a failed job.")
    public void setJobRetries(
            @ToolParam(description = "The job ID to update") String jobId,
            @ToolParam(description = "The new retry count (must be >= 0)") int retries) {

        managementService.setJobRetries(jobId, retries);
    }

    @Tool(description = "Get the full exception stack trace for a failed job.")
    public String getJobExceptionStacktrace(
            @ToolParam(description = "The job ID to retrieve the stack trace for") String jobId) {

        String trace = managementService.getJobExceptionStacktrace(jobId);
        return trace != null ? trace : "";
    }

    @Tool(description = "Get the current database table row counts for all process engine tables.")
    public Map<String, Long> getDatabaseTableCounts() {
        return managementService.getTableCount();
    }

    @Tool(description = "Get properties (key-value pairs) stored in the process engine's property table.")
    public Map<String, String> getEngineProperties() {
        return managementService.getProperties();
    }
}
