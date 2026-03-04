package com.fluxnova.scottlogic.mcp;

import com.fluxnova.scottlogic.mcp.tools.AuthorizationTools;
import com.fluxnova.scottlogic.mcp.tools.DecisionTools;
import com.fluxnova.scottlogic.mcp.tools.ExternalTaskTools;
import com.fluxnova.scottlogic.mcp.tools.FormTools;
import com.fluxnova.scottlogic.mcp.tools.HistoryTools;
import com.fluxnova.scottlogic.mcp.tools.IdentityTools;
import com.fluxnova.scottlogic.mcp.tools.ManagementTools;
import com.fluxnova.scottlogic.mcp.tools.ProcessInstanceTools;
import com.fluxnova.scottlogic.mcp.tools.RepositoryTools;
import com.fluxnova.scottlogic.mcp.tools.SignalTools;
import com.fluxnova.scottlogic.mcp.tools.TaskTools;
import org.finos.fluxnova.bpm.engine.AuthorizationService;
import org.finos.fluxnova.bpm.engine.DecisionService;
import org.finos.fluxnova.bpm.engine.ExternalTaskService;
import org.finos.fluxnova.bpm.engine.FormService;
import org.finos.fluxnova.bpm.engine.HistoryService;
import org.finos.fluxnova.bpm.engine.IdentityService;
import org.finos.fluxnova.bpm.engine.ManagementService;
import org.finos.fluxnova.bpm.engine.RepositoryService;
import org.finos.fluxnova.bpm.engine.RuntimeService;
import org.finos.fluxnova.bpm.engine.TaskService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpToolsConfiguration {

    // ---- tool beans ----

    @Bean
    public ProcessInstanceTools processInstanceTools(RuntimeService runtimeService) {
        return new ProcessInstanceTools(runtimeService);
    }

    @Bean
    public TaskTools taskTools(TaskService taskService) {
        return new TaskTools(taskService);
    }

    @Bean
    public RepositoryTools repositoryTools(RepositoryService repositoryService) {
        return new RepositoryTools(repositoryService);
    }

    @Bean
    public HistoryTools historyTools(HistoryService historyService) {
        return new HistoryTools(historyService);
    }

    @Bean
    public SignalTools signalTools(RuntimeService runtimeService) {
        return new SignalTools(runtimeService);
    }

    @Bean
    public ExternalTaskTools externalTaskTools(ExternalTaskService externalTaskService) {
        return new ExternalTaskTools(externalTaskService);
    }

    @Bean
    public IdentityTools identityTools(IdentityService identityService) {
        return new IdentityTools(identityService);
    }

    @Bean
    public ManagementTools managementTools(ManagementService managementService) {
        return new ManagementTools(managementService);
    }

    @Bean
    public FormTools formTools(FormService formService) {
        return new FormTools(formService);
    }

    @Bean
    public DecisionTools decisionTools(DecisionService decisionService) {
        return new DecisionTools(decisionService);
    }

    @Bean
    public AuthorizationTools authorizationTools(AuthorizationService authorizationService) {
        return new AuthorizationTools(authorizationService);
    }

    // ---- MCP tool callback provider ----

    /**
     * Registers all BPM tool objects with Spring AI's MCP server.
     * The MCP auto-configuration discovers this bean and exposes every
     * {@code @Tool}-annotated method as an MCP tool.
     */
    @Bean
    public ToolCallbackProvider bpmToolCallbackProvider(
            ProcessInstanceTools processInstanceTools,
            TaskTools taskTools,
            RepositoryTools repositoryTools,
            HistoryTools historyTools,
            SignalTools signalTools,
            ExternalTaskTools externalTaskTools,
            IdentityTools identityTools,
            ManagementTools managementTools,
            FormTools formTools,
            DecisionTools decisionTools,
            AuthorizationTools authorizationTools) {

        return MethodToolCallbackProvider.builder()
                .toolObjects(
                        processInstanceTools,
                        taskTools,
                        repositoryTools,
                        historyTools,
                        signalTools,
                        externalTaskTools,
                        identityTools,
                        managementTools,
                        formTools,
                        decisionTools,
                        authorizationTools)
                .build();
    }
}
