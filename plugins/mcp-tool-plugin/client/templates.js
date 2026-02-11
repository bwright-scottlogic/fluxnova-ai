'use strict';

module.exports = {
    panel:
        '<div id="mcp-custom-properties">' +
        '<div class="mcp-panel-title">MCP Tool Properties</div>' +
        '<div class="mcp-fields-container"></div>' +
        '</div>',

    inputField:
        '<div class="mcp-form-row">' +
        '<label class="mcp-form-label"></label>' +
        '<input type="text" class="mcp-form-input" />' +
        '</div>',

    textareaField:
        '<div class="mcp-form-row">' +
        '<label class="mcp-form-label"></label>' +
        '<textarea class="mcp-form-textarea" rows="5"></textarea>' +
        '</div>',

    checkboxField:
        '<div class="mcp-checkbox-row">' +
        '<label class="mcp-checkbox-label"></label>' +
        '<input type="checkbox" class="mcp-checkbox" />' +
        '</div>',

    parametersContainer:
        '<div class="mcp-parameters-container">' +
        '<div class="mcp-parameters-header">' +
        '<div class="mcp-parameters-title">MCP Tool Parameters</div>' +
        '<button class="mcp-btn-add">+ Add</button>' +
        '</div>' +
        '<div class="mcp-parameters-list"></div>' +
        '</div>',

    parameterRow:
        '<div class="mcp-parameter-row">' +
        '<input type="text" class="mcp-parameter-name" placeholder="Name" />' +
        '<select class="mcp-parameter-type"></select>' +
        '<button class="mcp-btn-remove">X</button>' +
        '</div>',

    emptyParameters:
        '<div class="mcp-parameters-empty">No parameters defined</div>',

    aiBadge:
        '<div class="mcp-ai-badge">' +
        '<svg width="36" height="36" viewBox="0 0 36 36">' +
        '<text x="18" y="23" font-family="Arial" font-size="12" font-weight="bold" fill="#52b415" text-anchor="middle">AI</text>' +
        '</svg>' +
        '</div>'
};
