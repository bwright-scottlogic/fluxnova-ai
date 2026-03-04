package com.fluxnova.scottlogic.mcp.tools;

import org.finos.fluxnova.bpm.engine.DecisionService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.List;
import java.util.Map;

public class DecisionTools {

    private final DecisionService decisionService;

    public DecisionTools(DecisionService decisionService) {
        this.decisionService = decisionService;
    }

    @Tool(description = "Evaluate a DMN decision table by its definition key (latest deployed version) and return all matching rule results.")
    public List<Map<String, Object>> evaluateDecisionTableByKey(
            @ToolParam(description = "The decision definition key as defined in the DMN model") String decisionDefinitionKey,
            @ToolParam(description = "Input variables for the decision as a key/value map") Map<String, Object> variables) {

        return decisionService
                .evaluateDecisionTableByKey(decisionDefinitionKey, variables)
                .getResultList();
    }

    @Tool(description = "Evaluate a DMN decision table by its specific definition ID and return all matching rule results.")
    public List<Map<String, Object>> evaluateDecisionTableById(
            @ToolParam(description = "The decision definition ID (not key) to evaluate") String decisionDefinitionId,
            @ToolParam(description = "Input variables for the decision as a key/value map") Map<String, Object> variables) {

        return decisionService
                .evaluateDecisionTableById(decisionDefinitionId, variables)
                .getResultList();
    }

    @Tool(description = "Evaluate a DMN decision table by key and version, and return all matching rule results.")
    public List<Map<String, Object>> evaluateDecisionTableByKeyAndVersion(
            @ToolParam(description = "The decision definition key as defined in the DMN model") String decisionDefinitionKey,
            @ToolParam(description = "The specific version number to use (use null for latest)") Integer version,
            @ToolParam(description = "Input variables for the decision as a key/value map") Map<String, Object> variables) {

        return decisionService
                .evaluateDecisionTableByKeyAndVersion(decisionDefinitionKey, version, variables)
                .getResultList();
    }
}
