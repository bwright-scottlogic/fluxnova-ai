package org.finos.fluxnova.ai.mcp.engine;

import org.finos.fluxnova.bpm.engine.impl.bpmn.parser.AbstractBpmnParseListener;
import org.finos.fluxnova.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.finos.fluxnova.bpm.engine.impl.pvm.process.ActivityImpl;
import org.finos.fluxnova.bpm.engine.impl.pvm.process.ScopeImpl;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Element;
import org.finos.fluxnova.ai.mcp.model.ToolDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class McpParseListener extends AbstractBpmnParseListener {
    private static final Logger LOG = LoggerFactory.getLogger(McpParseListener.class);

    private final BpmnToolExtractor extractor;
    private final ToolFactory factory;

    public McpParseListener(BpmnToolExtractor extractor, ToolFactory factory) {
        this.extractor = extractor;
        this.factory = factory;
        LOG.debug("MCP - McpParseListener instance created: {}", this);
    }

    @Override
    public void parseStartEvent(Element startEventElement, ScopeImpl scope, ActivityImpl activity) {
        try {
            String processId = ((ProcessDefinitionEntity) scope.getProcessDefinition()).getKey();
            String startEventId = activity.getId();

            LOG.debug("MCP - Parsing start event '{}' in process '{}'", startEventId, processId);

            ToolDefinition definition = extractor.extract(startEventElement, processId);

            if (definition != null) {
                LOG.info("MCP - Found MCP tool definition, registering: '{}'", definition.toolName());
                factory.createAndRegister(definition);
            } else {
                LOG.debug("MCP - No MCP tool definition found for start event '{}' in process '{}'",
                        startEventId, processId);
            }

        } catch (Exception e) {
            LOG.error("MCP - Error processing MCP start event in process: {}",
                    ((ProcessDefinitionEntity) scope.getProcessDefinition()).getKey(), e);
        }
    }
}
