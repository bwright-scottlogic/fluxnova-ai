package org.finos.fluxnova.ai.mcp.engine;

import org.finos.fluxnova.bpm.engine.ProcessEngine;
import org.finos.fluxnova.bpm.engine.RuntimeService;
import org.finos.fluxnova.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.finos.fluxnova.bpm.engine.impl.cfg.ProcessEnginePlugin;
import org.finos.fluxnova.ai.mcp.registry.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class McpEnginePlugin implements ProcessEnginePlugin {
    private static final Logger LOG = LoggerFactory.getLogger(McpEnginePlugin.class);

    private final RuntimeService runtimeService;
    private final ToolRegistry toolRegistry;

    public McpEnginePlugin(RuntimeService runtimeService, ToolRegistry toolRegistry) {
        this.runtimeService = runtimeService;
        this.toolRegistry = toolRegistry;
    }

    @Override
    public void preInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
        LOG.info("MCP - Initializing MCP Engine Plugin");

        BpmnToolExtractor extractor = new BpmnToolExtractor();
        ProcessStarter processStarter = new ProcessStarter(runtimeService);
        ToolFactory factory = new ToolFactory(processStarter, toolRegistry);
        McpParseListener listener = new McpParseListener(extractor, factory);

        LOG.debug("MCP - Parse listeners BEFORE adding: {}",
                processEngineConfiguration.getCustomPostBPMNParseListeners().size());
        processEngineConfiguration.getCustomPostBPMNParseListeners().add(listener);
        LOG.debug("MCP - Parse listeners AFTER adding: {}",
                processEngineConfiguration.getCustomPostBPMNParseListeners().size());
        LOG.debug("MCP - Listener instance: {}", listener);
        LOG.info("MCP Engine Plugin initialized - BPMN parse listener registered");
    }

    @Override
    public void postInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
        LOG.debug("MCP Engine Plugin post-initialization complete");
    }

    @Override
    public void postProcessEngineBuild(ProcessEngine processEngine) {
        LOG.info("MCP Engine Plugin ready - Process engine built");
    }
}
