# MCP Tool Integration for Fluxnova BPM
Automatically registers Fluxnova BPMN processes as MCP (Model Context Protocol) tools, allowing AI assistants to start workflow processes.

## Business Key Propagation
Business Key propagation from parent (Parent process starting another process via MCP) calling process has limited support. You are required to inject the business key into your prompt, e.g " - BusinessKey is ABC-123".

This approach can be tidied up once strategic support for LLM Invocation is imported into Fluxnova

## How It Works

### Architecture
BPMN File → Parse Listener → Tool Extractor → Tool Factory → Tool Registry → MCP Server ↓ Process Starter

1. **BPMN Parsing**: When a BPMN file is deployed, `McpParseListener` intercepts start events
2. **Tool Extraction**: `BpmnToolExtractor` reads MCP attributes from start events
3. **Tool Creation**: `ToolFactory` creates a tool configuration and handler
4. **Registration**: `ToolRegistry` registers the tool with the MCP server
5. **Invocation**: When an AI calls the tool, `ProcessStarter` starts a Fluxnova process instance

### Package Structure
```
org.finos.fluxnova.ai.mcp/ 
├── model/ # Domain objects (ToolDefinition, ToolParameter, ToolHandler) 
├── engine/ # Fluxnova integration (parsing, extraction, execution) 
│ ├── McpEnginePlugin 
│ ├── McpParseListener 
│ ├── BpmnToolExtractor 
│ ├── ToolFactory 
│ └── ProcessStarter 
├── registry/ # MCP server integration (tool registration) 
│ ├── ToolRegistry 
│ └── ToolConfig 
└── autoconfigure/ # Spring Boot auto-configuration
```
## Usage

### Spring AI MCP Dependencies

### Maven Dependencies

Add the following dependencies to your `pom.xml`:

```xml
<dependencies>
    <!-- Spring AI MCP Server -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-mcp-server-webmvc</artifactId>
    </dependency>

    <!-- MCP Fluxnova Extension -->
    <dependency>
        <groupId>io.mcp.tools</groupId>
        <artifactId>mcp-fluxnova-extension</artifactId>
    </dependency>
</dependencies>
```
### Spring MCP Properties
Spring AI needs to be configured. Below is an example, **but please refer to Spring AI documentation**.
```
# MCP Server Configuration
spring.ai.mcp.server.name=fluxnova-mcp-server
spring.ai.mcp.server.version=1.0.0
spring.ai.mcp.server.protocol=streamable
spring.ai.mcp.server.stdio=false
spring.ai.mcp.server.type=sync
```

### 1. Add MCP Attributes to BPMN Start Events

TODO See associated MCP Plugin - Link TBC 
```xml
<bpmn:definitions 
    xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
    xmlns:mcp="http://fluxnova.finos.org/schema/1.0/ai/mcp">
  
  <bpmn:process id="kycProcess" isExecutable="true">
    <bpmn:startEvent id="start" 
                     mcp:type="mcpToolStart"
                     mcp:toolName="KYC Review Tool"
                     mcp:description="Initiates KYC check on customer">
      <bpmn:extensionElements>
        <mcp:parameters>
          <mcp:parameter paramName="customerName" paramType="string" />
          <mcp:parameter paramName="customerId" paramType="string" />
        </mcp:parameters>
      </bpmn:extensionElements>
      <bpmn:outgoing>flow1</bpmn:outgoing>
    </bpmn:startEvent>
    <!-- rest of process -->
  </bpmn:process>
</bpmn:definitions>
```
### 2. Deploy BPMN File
Deploy the BPMN file to Fluxnova. The tool is automatically registered.

### 3. AI Invokes Tool
When an AI assistant calls the tool:
```

{
  "toolName": "KYC Review Tool",
  "arguments": {
    "customerName": "John Doe",
    "customerId": "12345"
  }
}
```
A Fluxnova process instance starts with these variables.

## MCP Attributes
| Attribute | Required | Description |
|-----------|----------|-------------|
| `mcp:type` | No | Set to `"mcpToolStart"` to mark as MCP tool |
| `mcp:toolName` | Yes | Name of the tool (shown to AI) |
| `mcp:description` | Yes | Description of what the tool does |
| `mcp:propagateBusinessKey` | No | Whether to use `businessKey` argument (default: `true`) |


#### Parameters
Define parameters in <mcp:parameters>:
```
<mcp:parameter paramName="name" paramType="type" />
```
Supported types: string, number, boolean, object, array

### Configuration
Auto-configured via Spring Boot. No manual configuration needed.

Optional: Enable Debug Logging
```
logging:
  level:
    org.finos.fluxnova.ai.mcp: DEBUG
```

## Architecture diagrams
### MCP Tool Registration and Execution Flow

```mermaid
sequenceDiagram
    participant BPMN as BPMN Deployment
    participant Plugin as McpEnginePlugin
    participant Listener as McpParseListener
    participant Extractor as BpmnToolExtractor
    participant Factory as ToolFactory
    participant Registry as ToolRegistry
    participant MCP as McpSyncServer
    participant LLM as LLM Client
    participant Starter as ProcessStarter
    participant Runtime as RuntimeService

    Note over BPMN,Plugin: 1. Plugin Initialization
    BPMN->>Plugin: Deploy BPMN Process
    Plugin->>Plugin: preInit()
    Plugin->>Listener: Register Parse Listener

    Note over Listener,Factory: 2. BPMN Parsing & Tool Registration
    Listener->>Listener: parseStartEvent()
    Listener->>Extractor: extract(startEventElement, processId)
    Extractor->>Extractor: Read mcp:toolName, mcp:description
    Extractor->>Extractor: extractParameters()
    Extractor->>Extractor: Add businessKey if propagateBusinessKey=true
    Extractor-->>Listener: ToolDefinition

    Listener->>Factory: createAndRegister(definition)
    Factory->>Factory: buildParameterSpecs()
    Factory->>Factory: Create ToolConfig
    Factory->>Registry: register(config)
    Registry->>Registry: buildTool()
    Registry->>Registry: buildJsonSchema()
    Registry->>MCP: addTool(spec)
    Registry->>MCP: notifyToolsListChanged()
    MCP-->>Registry: Tool Registered
    Registry-->>Factory: Success

    Note over LLM,Runtime: 3. Tool Invocation by LLM
    LLM->>MCP: list_tools()
    MCP-->>LLM: Available Tools (JSON Schema)

    LLM->>MCP: call_tool(toolName, arguments)
    MCP->>Registry: Execute Tool Handler
    Registry->>Starter: startProcess(definition, arguments)
    Starter->>Runtime: startProcessInstanceByKey(processId, businessKey, args)
    Runtime-->>Starter: ProcessInstance
    Starter-->>Registry: Map{processInstanceId, businessKey, message}
    Registry->>Registry: formatResult()
    Registry-->>MCP: CallToolResult
    MCP-->>LLM: Tool Execution Result
```
### Component Responsibilities
```mermaid

graph TB
subgraph "Spring Boot Auto-Configuration"
AutoConfig[McpAutoConfiguration]
AutoConfig -->|Creates| Registry[ToolRegistry]
AutoConfig -->|Creates| Plugin[McpEnginePlugin]
end

    subgraph "BPMN Processing Layer"
        Plugin -->|Registers| Listener[McpParseListener]
        Listener -->|Uses| Extractor[BpmnToolExtractor]
        Listener -->|Uses| Factory[ToolFactory]
        Extractor -->|Produces| ToolDef[ToolDefinition]
        Factory -->|Consumes| ToolDef
    end
    
    subgraph "MCP Integration Layer"
        Factory -->|Registers Tools| Registry
        Registry -->|Manages| MCP[McpSyncServer]
        Registry -->|Stores| ToolConfig[ToolConfig]
        ToolConfig -->|Contains| Handler[ToolHandler]
    end
    
    subgraph "Process Execution Layer"
        Handler -->|Delegates to| Starter[ProcessStarter]
        Starter -->|Starts Process| Runtime[RuntimeService]
    end
    
    subgraph "Domain Models"
        ToolDef -->|Contains| Params[ToolParameter]
        ToolConfig -->|Contains| ParamSpec[ParameterSpec]
    end
    
    style AutoConfig fill:#e1f5ff
    style Registry fill:#fff4e1
    style MCP fill:#e8f5e9
    style Runtime fill:#fce4ec
```
| Component | Purpose                                                                                            |
|-----------|----------------------------------------------------------------------------------------------------|
| **McpEnginePlugin** | Fluxnova process engine plugin that registers the BPMN parse listener during engine initialization |
| **McpParseListener** | Listens for start events during BPMN parsing and triggers tool extraction                          |
| **BpmnToolExtractor** | Reads MCP-specific XML attributes and elements to create ToolDefinition objects                    |
| **ToolFactory** | Converts ToolDefinitions into ToolConfigs and registers them with the registry                     |
| **ToolRegistry** | Manages tool lifecycle, builds JSON schemas, and integrates with McpSyncServer                     |
| **ProcessStarter** | Executes the actual process start when an LLM invokes a tool                                       |
| **ToolDefinition** | Domain model representing extracted BPMN metadata                                                  |
| **ToolParameter** | Represents a single parameter with name, type, and optional flag                                   |
| **ToolConfig** | Configuration object containing tool metadata and execution handler                                |

# Testing
Directly testable via `npx @modelcontextprotocol/inspector`. After connecting to your Fluxnova server, you can invoke the process via the Tools menu.

# Troubleshooting
## Tool not registering?

- Enable DEBUG logging
- Check BPMN has mcp:toolName attribute
- Verify namespace: xmlns:mcp="http://fluxnova.finos.org/schema/1.0/ai/mcp"`
- Check logs for "Registered MCP tool" message

## Parameters not working?
- Use paramName and paramType (not mcp:paramName)
- Supported types: string, number, boolean, object, array
