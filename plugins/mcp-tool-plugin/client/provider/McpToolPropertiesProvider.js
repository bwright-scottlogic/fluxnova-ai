'use strict';

var domify = require('domify');
var McpUtil = require('../util/McpUtil');
var TEMPLATES = require('../templates');

var PARAMETER_TYPES = ['String', 'Boolean', 'Integer', 'Long', 'Double', 'Date'];

var moddle = null;

/**
 * Provider for MCP Tool custom properties panel
 */
function McpToolPropertiesProvider(eventBus, modeling, bpmnFactory) {
    moddle = bpmnFactory;
    var currentElement = null;

    eventBus.on('selection.changed', function(e) {
        removeCustomPanel();
        var newSelection = e.newSelection;

        if (newSelection && newSelection.length === 1) {
            currentElement = newSelection[0];
            if (currentElement.type === 'bpmn:StartEvent' && McpUtil.hasMcpType(currentElement.businessObject)) {
                setTimeout(function() { injectCustomPanel(currentElement, modeling); }, 200);
            }
        } else {
            currentElement = null;
        }
    });

    eventBus.on('elements.changed', function(e) {
        if (currentElement && e.elements.some(function(el) { return el.id === currentElement.id; })) {
            if (McpUtil.hasMcpType(currentElement.businessObject) && !document.getElementById('mcp-custom-properties')) {
                setTimeout(function() { injectCustomPanel(currentElement, modeling); }, 200);
            }
        }
    });
}

McpToolPropertiesProvider.$inject = ['eventBus', 'modeling', 'bpmnFactory'];

function removeCustomPanel() {
    var existing = document.getElementById('mcp-custom-properties');
    if (existing) existing.remove();
}

function injectCustomPanel(element, modeling) {
    removeCustomPanel();

    var container = document.querySelector('.bio-properties-panel-scroll-container') ||
        document.querySelector('[class*="properties-panel"]');

    if (!container) return;

    var bo = element.businessObject;
    var panel = domify(TEMPLATES.panel);
    var fieldsContainer = panel.querySelector('.mcp-fields-container');

    fieldsContainer.appendChild(createInputField(
        { prop: 'mcp:toolName', label: 'MCP Tool Name', placeholder: 'Provide a descriptive Name for your MCP Tool' },
        element, bo, modeling
    ));

    fieldsContainer.appendChild(createInputField(
        { prop: 'mcp:description', label: 'MCP Tool Description', placeholder: 'Provide a description to clearly identify the purpose of your MCP tool to a calling LLM', isTextArea: true },
        element, bo, modeling
    ));

    fieldsContainer.appendChild(createCheckbox(
        { prop: 'mcp:propagateBusinessKey', label: 'Propagate Business Key' },
        element, bo, modeling
    ));

    fieldsContainer.appendChild(createParametersList(element, bo, modeling));

    container.insertBefore(panel, container.firstChild);
}

function createInputField(field, element, bo, modeling) {
    var row = domify(field.isTextArea ? TEMPLATES.textareaField : TEMPLATES.inputField);
    var label = row.querySelector('label');
    var input = row.querySelector(field.isTextArea ? 'textarea' : 'input');

    label.textContent = field.label;
    input.value = McpUtil.getMcpProperty(bo, field.prop) || '';
    input.placeholder = field.placeholder || '';

    input.addEventListener('change', function(e) {
        var update = {};
        update[field.prop] = e.target.value;
        modeling.updateProperties(element, update);
    });

    return row;
}

function createCheckbox(field, element, bo, modeling) {
    var row = domify(TEMPLATES.checkboxField);
    var label = row.querySelector('label');
    var checkbox = row.querySelector('input');

    label.textContent = field.label;
    checkbox.checked = McpUtil.getMcpProperty(bo, field.prop) !== false;

    checkbox.addEventListener('change', function(e) {
        var update = {};
        update[field.prop] = e.target.checked;
        modeling.updateProperties(element, update);
    });

    return row;
}

function createParametersList(element, bo, modeling) {
    var container = domify(TEMPLATES.parametersContainer);
    var addBtn = container.querySelector('.mcp-btn-add');
    var listContainer = container.querySelector('.mcp-parameters-list');

    function render() {
        listContainer.innerHTML = '';
        var parameters = McpUtil.getParameters(bo);

        if (parameters.length === 0) {
            listContainer.appendChild(domify(TEMPLATES.emptyParameters));
            return;
        }

        parameters.forEach(function(param, index) {
            var row = domify(TEMPLATES.parameterRow);
            var nameInput = row.querySelector('.mcp-parameter-name');
            var typeSelect = row.querySelector('.mcp-parameter-type');
            var removeBtn = row.querySelector('.mcp-btn-remove');

            nameInput.value = param.paramName || '';
            nameInput.addEventListener('change', function(e) {
                param.paramName = e.target.value;
                McpUtil.updateModdle(element, bo, modeling);
            });

            PARAMETER_TYPES.forEach(function(type) {
                var option = document.createElement('option');
                option.value = type;
                option.textContent = type;
                option.selected = type === param.paramType;
                typeSelect.appendChild(option);
            });

            typeSelect.addEventListener('change', function(e) {
                param.paramType = e.target.value;
                McpUtil.updateModdle(element, bo, modeling);
            });

            removeBtn.addEventListener('click', function() {
                parameters.splice(index, 1);
                McpUtil.updateModdle(element, bo, modeling);
                render();
            });

            listContainer.appendChild(row);
        });
    }

    addBtn.addEventListener('click', function() {
        var extensionElements = McpUtil.getExtensionElements(bo);
        if (!extensionElements) {
            extensionElements = moddle.create('bpmn:ExtensionElements', { values: [] });
            modeling.updateModdleProperties(element, bo, { extensionElements: extensionElements });
        }

        var mcpParams = McpUtil.getMcpParameters(bo);
        if (!mcpParams) {
            mcpParams = moddle.create('mcp:Parameters', { parameters: [] });
            extensionElements.get('values').push(mcpParams);
            mcpParams.$parent = extensionElements;
        }

        var newParam = moddle.create('mcp:Parameter', { paramName: '', paramType: 'String' });
        newParam.$parent = mcpParams;
        mcpParams.get('parameters').push(newParam);

        McpUtil.updateModdle(element, bo, modeling);
        render();
    });

    render();
    return container;
}

module.exports = McpToolPropertiesProvider;
