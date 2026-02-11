package org.finos.fluxnova.ai.mcp.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpSyncServer;
import org.finos.fluxnova.ai.mcp.engine.*;
import org.finos.fluxnova.bpm.engine.RepositoryService;
import org.finos.fluxnova.bpm.engine.RuntimeService;
import org.finos.fluxnova.ai.mcp.registry.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;

@AutoConfiguration
@ConditionalOnClass({McpSyncServer.class, RuntimeService.class})
public class McpAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(McpAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public ToolRegistry toolRegistry(McpSyncServer mcpServer, ObjectMapper objectMapper) {
        log.info("Auto-configuring ToolRegistry bean");
        return new ToolRegistry(mcpServer, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public McpEnginePlugin mcpEnginePlugin(
            @Lazy RuntimeService runtimeService,
            ToolRegistry toolRegistry) {
        log.info("Auto-configuring McpEnginePlugin with ToolRegistry");
        return new McpEnginePlugin(runtimeService, toolRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public McpStartupScanner mcpStartupScanner(
            RepositoryService repositoryService,
            BpmnToolExtractor extractor,
            ToolFactory factory) {
        return new McpStartupScanner(repositoryService, extractor, factory);
    }

    @Bean
    public ApplicationRunner mcpToolRegistrationRunner(McpStartupScanner scanner) {
        return args -> {
            log.info("MCP - Starting tool registration from existing processes");
            scanner.scanAndRegisterExistingProcesses();
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public BpmnToolExtractor bpmnToolExtractor() {
        return new BpmnToolExtractor();
    }

    @Bean
    @ConditionalOnMissingBean
    public ProcessStarter processStarter(RuntimeService runtimeService) {
        return new ProcessStarter(runtimeService);
    }

    @Bean
    @ConditionalOnMissingBean
    public ToolFactory toolFactory(ProcessStarter processStarter, ToolRegistry toolRegistry) {
        return new ToolFactory(processStarter, toolRegistry);
    }
}
