package org.finos.fluxnova.ai.mcp.engine;

import org.finos.fluxnova.ai.mcp.model.ToolDefinition;
import org.finos.fluxnova.bpm.engine.RepositoryService;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Element;
import org.finos.fluxnova.bpm.engine.repository.ProcessDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

public class McpStartupScanner {
    private static final Logger LOG = LoggerFactory.getLogger(McpStartupScanner.class);

    private final RepositoryService repositoryService;
    private final BpmnToolExtractor extractor;
    private final ToolFactory factory;

    public McpStartupScanner(RepositoryService repositoryService,
                             BpmnToolExtractor extractor,
                             ToolFactory factory) {
        this.repositoryService = repositoryService;
        this.extractor = extractor;
        this.factory = factory;
    }

    public void scanAndRegisterExistingProcesses() {
        LOG.info("MCP - Scanning existing process definitions for MCP tools");

        List<ProcessDefinition> definitions = repositoryService
                .createProcessDefinitionQuery()
                .latestVersion()
                .list();

        int registered = 0;
        for (ProcessDefinition definition : definitions) {
            try {
                registered += scanProcessDefinition(definition);
            } catch (Exception e) {
                LOG.error("MCP - Failed to scan process: {}", definition.getKey(), e);
            }
        }

        LOG.info("MCP - Startup scan complete. Registered {} tools from {} processes",
                registered, definitions.size());
    }

    private int scanProcessDefinition(ProcessDefinition definition) {
        try (InputStream bpmnStream = repositoryService.getProcessModel(definition.getId())) {
            // Parse as XML Document
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            docFactory.setNamespaceAware(true);
            Document doc = docFactory.newDocumentBuilder().parse(bpmnStream);

            NodeList startEvents = doc.getElementsByTagName("startEvent");
            int count = 0;

            for (int i = 0; i < startEvents.getLength(); i++) {
                Element startEvent = (Element) startEvents.item(i);
                ToolDefinition toolDef = extractor.extract(startEvent, definition.getKey());

                if (toolDef != null) {
                    factory.createAndRegister(toolDef);  // Now uses ToolFactory from class field
                    count++;
                }
            }

            return count;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse BPMN for process: " + definition.getKey(), e);
        }
    }

}
