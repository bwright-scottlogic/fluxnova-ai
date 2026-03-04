package com.fluxnova.scottlogic.mcp.tools;

import org.finos.fluxnova.bpm.engine.FormService;
import org.finos.fluxnova.bpm.engine.form.StartFormData;
import org.finos.fluxnova.bpm.engine.form.TaskFormData;
import org.finos.fluxnova.bpm.engine.runtime.ProcessInstance;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.Map;
import java.util.stream.Collectors;

public class FormTools {

    private final FormService formService;

    public FormTools(FormService formService) {
        this.formService = formService;
    }

    @Tool(description = "Get the form key for the start event of a process definition.")
    public String getStartFormKey(
            @ToolParam(description = "The process definition ID") String processDefinitionId) {

        String key = formService.getStartFormKey(processDefinitionId);
        return key != null ? key : "";
    }

    @Tool(description = "Get the form key for a specific user task within a process definition.")
    public String getTaskFormKey(
            @ToolParam(description = "The process definition ID") String processDefinitionId,
            @ToolParam(description = "The task definition key (BPMN element ID of the task)") String taskDefinitionKey) {

        String key = formService.getTaskFormKey(processDefinitionId, taskDefinitionKey);
        return key != null ? key : "";
    }

    @Tool(description = "Get the form field definitions for a process start event.")
    public Map<String, Object> getStartFormData(
            @ToolParam(description = "The process definition ID") String processDefinitionId) {

        StartFormData data = formService.getStartFormData(processDefinitionId);
        if (data == null) {
            return Map.of();
        }
        return Map.of(
                "formKey", data.getFormKey() != null ? data.getFormKey() : "",
                "fields", data.getFormFields().stream()
                        .map(f -> Map.of(
                                "id", f.getId() != null ? f.getId() : "",
                                "label", f.getLabel() != null ? f.getLabel() : "",
                                "typeName", f.getType() != null ? f.getType().getName() : "",
                                "defaultValue", f.getDefaultValue() != null ? String.valueOf(f.getDefaultValue()) : ""))
                        .collect(Collectors.toList()));
    }

    @Tool(description = "Get the form field definitions for a user task.")
    public Map<String, Object> getTaskFormData(
            @ToolParam(description = "The task ID") String taskId) {

        TaskFormData data = formService.getTaskFormData(taskId);
        if (data == null) {
            return Map.of();
        }
        return Map.of(
                "formKey", data.getFormKey() != null ? data.getFormKey() : "",
                "fields", data.getFormFields().stream()
                        .map(f -> Map.of(
                                "id", f.getId() != null ? f.getId() : "",
                                "label", f.getLabel() != null ? f.getLabel() : "",
                                "typeName", f.getType() != null ? f.getType().getName() : "",
                                "defaultValue", f.getDefaultValue() != null ? String.valueOf(f.getDefaultValue()) : ""))
                        .collect(Collectors.toList()));
    }

    @Tool(description = "Get the pre-populated variable values for rendering a process start form.")
    public Map<String, Object> getStartFormVariables(
            @ToolParam(description = "The process definition ID") String processDefinitionId) {

        return formService.getStartFormVariables(processDefinitionId).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue() != null ? e.getValue() : ""));
    }

    @Tool(description = "Get the pre-populated variable values for rendering a task form.")
    public Map<String, Object> getTaskFormVariables(
            @ToolParam(description = "The task ID") String taskId) {

        return formService.getTaskFormVariables(taskId).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue() != null ? e.getValue() : ""));
    }

    @Tool(description = "Start a new process instance by submitting a start form with provided field values. Returns the new process instance ID.")
    public String submitStartForm(
            @ToolParam(description = "The process definition ID to start") String processDefinitionId,
            @ToolParam(description = "Optional business key for the new process instance", required = false) String businessKey,
            @ToolParam(description = "Form field values as a key/value map") Map<String, Object> formProperties) {

        ProcessInstance instance;
        if (businessKey != null && !businessKey.isBlank()) {
            instance = formService.submitStartForm(processDefinitionId, businessKey, formProperties);
        } else {
            instance = formService.submitStartForm(processDefinitionId, formProperties);
        }
        return instance.getId();
    }

    @Tool(description = "Complete a user task by submitting its form with the provided field values.")
    public void submitTaskForm(
            @ToolParam(description = "The task ID to complete") String taskId,
            @ToolParam(description = "Form field values as a key/value map") Map<String, Object> formProperties) {

        formService.submitTaskForm(taskId, formProperties);
    }
}
