package com.fluxnova.scottlogic.mcp.tools;

import org.finos.fluxnova.bpm.engine.TaskService;
import org.finos.fluxnova.bpm.engine.task.Task;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TaskTools {

    private final TaskService taskService;

    public TaskTools(TaskService taskService) {
        this.taskService = taskService;
    }

    @Tool(description = "List open (uncompleted) user tasks. Optionally filter by assignee or process instance ID.")
    public List<Map<String, String>> listTasks(
            @ToolParam(description = "Filter by assignee username", required = false) String assignee,
            @ToolParam(description = "Filter by process instance ID", required = false) String processInstanceId) {

        var query = taskService.createTaskQuery();
        if (assignee != null && !assignee.isBlank()) {
            query.taskAssignee(assignee);
        }
        if (processInstanceId != null && !processInstanceId.isBlank()) {
            query.processInstanceId(processInstanceId);
        }

        return query.list().stream()
                .map(t -> Map.of(
                        "id", t.getId(),
                        "name", t.getName() != null ? t.getName() : "",
                        "assignee", t.getAssignee() != null ? t.getAssignee() : "",
                        "processInstanceId", t.getProcessInstanceId() != null ? t.getProcessInstanceId() : "",
                        "taskDefinitionKey", t.getTaskDefinitionKey() != null ? t.getTaskDefinitionKey() : ""))
                .collect(Collectors.toList());
    }

    @Tool(description = "Get a single task by its ID.")
    public Map<String, String> getTask(
            @ToolParam(description = "The task ID") String taskId) {

        Task t = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (t == null) {
            return Map.of("error", "Task not found: " + taskId);
        }
        return Map.of(
                "id", t.getId(),
                "name", t.getName() != null ? t.getName() : "",
                "description", t.getDescription() != null ? t.getDescription() : "",
                "assignee", t.getAssignee() != null ? t.getAssignee() : "",
                "processInstanceId", t.getProcessInstanceId() != null ? t.getProcessInstanceId() : "",
                "taskDefinitionKey", t.getTaskDefinitionKey() != null ? t.getTaskDefinitionKey() : "");
    }

    @Tool(description = "Claim a task, assigning it to the specified user.")
    public void claimTask(
            @ToolParam(description = "The task ID to claim") String taskId,
            @ToolParam(description = "The username to assign the task to") String userId) {

        taskService.claim(taskId, userId);
    }

    @Tool(description = "Complete a task, optionally providing output variables.")
    public void completeTask(
            @ToolParam(description = "The task ID to complete") String taskId,
            @ToolParam(description = "Optional variables to set on completion", required = false) Map<String, Object> variables) {

        if (variables != null && !variables.isEmpty()) {
            taskService.complete(taskId, variables);
        } else {
            taskService.complete(taskId);
        }
    }

    @Tool(description = "Set the assignee of a task to a different user.")
    public void reassignTask(
            @ToolParam(description = "The task ID") String taskId,
            @ToolParam(description = "The new assignee's username") String userId) {

        taskService.setAssignee(taskId, userId);
    }

    @Tool(description = "Get the form variables for a task (the data needed to render the task form).")
    public Map<String, Object> getTaskVariables(
            @ToolParam(description = "The task ID") String taskId) {

        return taskService.getVariables(taskId);
    }

    @Tool(description = "Add a comment to a task.")
    public String addTaskComment(
            @ToolParam(description = "The task ID") String taskId,
            @ToolParam(description = "The process instance ID the task belongs to") String processInstanceId,
            @ToolParam(description = "The comment text") String message) {

        var comment = taskService.createComment(taskId, processInstanceId, message);
        return comment.getId();
    }
}
