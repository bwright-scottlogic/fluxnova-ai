package com.fluxnova.scottlogic.mcp.tools;

import org.finos.fluxnova.bpm.engine.RepositoryService;
import org.finos.fluxnova.bpm.engine.repository.Deployment;
import org.finos.fluxnova.bpm.engine.repository.ProcessDefinition;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RepositoryTools {

    private final RepositoryService repositoryService;

    public RepositoryTools(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    @Tool(description = "List all deployed process definitions, optionally filtered by key.")
    public List<Map<String, String>> listProcessDefinitions(
            @ToolParam(description = "Optional process definition key to filter by", required = false) String key) {

        var query = repositoryService.createProcessDefinitionQuery().latestVersion();
        if (key != null && !key.isBlank()) {
            query.processDefinitionKey(key);
        }

        return query.list().stream()
                .map(pd -> Map.of(
                        "id", pd.getId(),
                        "key", pd.getKey(),
                        "name", pd.getName() != null ? pd.getName() : "",
                        "version", String.valueOf(pd.getVersion()),
                        "deploymentId", pd.getDeploymentId()))
                .collect(Collectors.toList());
    }

    @Tool(description = "Get the BPMN XML source of a deployed process definition.")
    public String getProcessDefinitionXml(
            @ToolParam(description = "The process definition ID (not key) to retrieve XML for") String processDefinitionId) {

        ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(processDefinitionId)
                .singleResult();
        if (pd == null) {
            return "Process definition not found: " + processDefinitionId;
        }
        try (var stream = repositoryService.getProcessModel(processDefinitionId)) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "Error reading BPMN: " + e.getMessage();
        }
    }

    @Tool(description = "Deploy a BPMN process from its XML string. Returns the new deployment ID.")
    public String deployProcessFromXml(
            @ToolParam(description = "A descriptive name for this deployment") String deploymentName,
            @ToolParam(description = "The resource file name (must end in .bpmn or .bpmn20.xml)") String resourceName,
            @ToolParam(description = "The full BPMN 2.0 XML content to deploy") String bpmnXml) {

        Deployment deployment = repositoryService.createDeployment()
                .name(deploymentName)
                .addInputStream(resourceName, new ByteArrayInputStream(bpmnXml.getBytes(StandardCharsets.UTF_8)))
                .deploy();

        return deployment.getId();
    }

    @Tool(description = "List all deployments in the repository.")
    public List<Map<String, String>> listDeployments() {

        return repositoryService.createDeploymentQuery().list().stream()
                .map(d -> Map.of(
                        "id", d.getId(),
                        "name", d.getName() != null ? d.getName() : "",
                        "deploymentTime", d.getDeploymentTime() != null ? d.getDeploymentTime().toString() : ""))
                .collect(Collectors.toList());
    }

    @Tool(description = "Suspend a process definition so no new instances can be started.")
    public void suspendProcessDefinition(
            @ToolParam(description = "The process definition key to suspend") String processDefinitionKey) {

        repositoryService.suspendProcessDefinitionByKey(processDefinitionKey);
    }

    @Tool(description = "Activate a previously suspended process definition.")
    public void activateProcessDefinition(
            @ToolParam(description = "The process definition key to activate") String processDefinitionKey) {

        repositoryService.activateProcessDefinitionByKey(processDefinitionKey);
    }
}
