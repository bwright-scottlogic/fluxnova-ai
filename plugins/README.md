# MCP Tool Plugin
A Fluxnova Modeler plugin that adds MCP (Model Context Protocol) Tool Start Event functionality to BPMN diagrams, enabling AI-powered workflow integration.

<p float="left">
  <img src="docs/MCPStartEvent.png" width="322" />
  <img src="docs/MCPStartEventProperties.png" width="600" />
</p>

## Overview
This plugin extends the Fluxnova Modeler with custom MCP Tool Start Events that can be configured with tool names, descriptions, and parameters for LLM integration.

## Architecture
### Component Structure
```mermaid
graph TB
subgraph "Entry Point"
A[index.js] -->|loads| B[client.bundle.js]
A -->|loads| C[styles.css]
end

    subgraph "Main Module"
        B --> D[client.js]
    end
    
    subgraph "Providers"
        D -->|registers| E[McpToolReplaceMenuProvider]
        D -->|registers| F[McpToolPropertiesProvider]
        D -->|registers| G[McpToolOverlayProvider]
    end
    
    subgraph "Utilities"
        E --> H[McpUtil]
        F --> H
        G --> H
        E --> I[templates.js]
        F --> I
        G --> I
    end
    
    subgraph "BPMN.js Integration"
        D -->|registers| J[Moddle Extension]
        J -->|defines| K[MCP Schema]
    end
    
    style A fill:#e1f5ff
    style D fill:#ffe1e1
    style H fill:#e1ffe1
    style I fill:#fff4e1
    style J fill:#f0e1ff
```

## File Organization
```
mcp-tool-plugin/
├── index.js                    # Plugin entry point
├── package.json                # Dependencies & scripts
├── webpack.config.js           # Build configuration
├── client/
│   ├── client.js              # Main registration module
│   ├── styles.css             # UI styling
│   ├── templates.js           # HTML templates
│   ├── provider/
│   │   ├── McpToolReplaceMenuProvider.js    # Context menu integration
│   │   ├── McpToolPropertiesProvider.js     # Properties panel
│   │   └── McpToolOverlayProvider.js        # AI badge overlay
│   └── util/
│       └── McpUtil.js         # Shared utilities
```

## Component Interaction Flow
1. Plugin Initialization
```mermaid
   sequenceDiagram
   participant CM as Fluxnova Modeler
   participant IDX as index.js
   participant CLI as client.js
   participant BPMN as BPMN.js

   CM->>IDX: Load plugin
   IDX->>CM: Register styles.css
   IDX->>CM: Register client.bundle.js
   CM->>CLI: Execute bundle
   CLI->>BPMN: Register Moddle Extension
   CLI->>BPMN: Register Providers
   BPMN-->>CM: Plugin ready

```
2. User Creates MCP Tool Start Event
```mermaid
   sequenceDiagram
   participant U as User
   participant RM as ReplaceMenuProvider
   participant M as Modeling Service
   participant BO as Business Object
   participant OV as OverlayProvider

   U->>RM: Right-click Start Event
   RM->>U: Show "MCP Tool Start Event" option
   U->>RM: Select option
   RM->>M: updateProperties()
   M->>BO: Set mcp:type = 'mcpToolStart'
   M->>BO: Set mcp:toolName = ''
   M->>BO: Set mcp:description = ''
   RM->>OV: addAiOverlay()
   OV->>U: Display AI badge
```
3. Properties Panel Interaction
```mermaid
   sequenceDiagram
   participant U as User
   participant EB as EventBus
   participant PP as PropertiesProvider
   participant T as Templates
   participant M as Modeling
   participant BO as Business Object

   U->>EB: Select MCP Tool Start Event
   EB->>PP: selection.changed event
   PP->>PP: Check if MCP type
   PP->>T: Get panel template
   T-->>PP: HTML structure
   PP->>PP: Create input fields
   PP->>U: Inject custom panel

   U->>PP: Change tool name
   PP->>M: updateProperties()
   M->>BO: Update mcp:toolName

   U->>PP: Add parameter
   PP->>M: Create mcp:Parameter
   M->>BO: Add to extensionElements
   PP->>U: Refresh parameter list
```
4. Data Model Structure
```mermaid
   classDiagram
   class StartEvent {
   +String id
   +String name
   }

   class McpProperties {
   +String type
   +String toolName
   +String description
   +Boolean propagateBusinessKey
   }

   class ExtensionElements {
   +Array~Element~ values
   }

   class Parameters {
   +Array~Parameter~ parameters
   }

   class Parameter {
   +String paramName
   +String paramType
   }

   StartEvent --|> McpProperties : extends
   StartEvent *-- ExtensionElements : contains
   ExtensionElements *-- Parameters : contains
   Parameters *-- Parameter : contains
```

### BPMN XML Structure
When a MCP Tool Start Event is created, it generates the following XML structure:
```xml
<bpmn:startEvent id="StartEvent_1" mcp:type="mcpToolStart"
    mcp:toolName="MyTool"
    mcp:description="Tool description"
    mcp:propagateBusinessKey="true">
        <bpmn:extensionElements>
            <mcp:Parameters>
            <mcp:Parameter paramName="input1" paramType="String"/>
            <mcp:Parameter paramName="count" paramType="Integer"/>
            </mcp:Parameters>
        </bpmn:extensionElements>
</bpmn:startEvent>
```
## Event Flow
```mermaid
sequenceDiagram
participant D as Diagram
participant EB as EventBus
participant OP as OverlayProvider
participant PP as PropertiesProvider
participant RM as ReplaceMenuProvider

    Note over D,RM: Diagram Import
    D->>EB: import.done
    EB->>OP: Scan for MCP elements
    OP->>D: Add AI badges
    
    Note over D,RM: Element Selection
    D->>EB: selection.changed
    EB->>PP: Check element type
    PP->>D: Inject properties panel
    
    Note over D,RM: Element Modification
    D->>EB: element.changed
    EB->>OP: Update overlay
    EB->>PP: Refresh panel if needed
    
    Note over D,RM: Context Menu
    D->>RM: Right-click event
    RM->>D: Show menu options
```

## Parameter Types
Supported parameter types for MCP Tool inputs:

```
String - Text values
Boolean - True/false values
Integer - Whole numbers
Long - Large whole numbers
Double - Decimal numbers
Date - Date/time values
```

# Build
## Install dependencies
npm install

## Build plugin
npm run build

