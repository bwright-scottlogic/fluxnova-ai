/******/ (() => { // webpackBootstrap
/******/ 	var __webpack_modules__ = ({

/***/ "./client/provider/McpToolOverlayProvider.js"
/*!***************************************************!*\
  !*** ./client/provider/McpToolOverlayProvider.js ***!
  \***************************************************/
(module, __unused_webpack_exports, __webpack_require__) {

"use strict";


var domify = __webpack_require__(/*! domify */ "./node_modules/domify/index.js");
var McpUtil = __webpack_require__(/*! ../util/McpUtil */ "./client/util/McpUtil.js");
var TEMPLATES = __webpack_require__(/*! ../templates */ "./client/templates.js");

/**
 * Provider for MCP Tool AI badge overlays
 */
function McpToolOverlayProvider(eventBus, overlays, elementRegistry) {
    this._overlays = overlays;

    eventBus.on(['import.done', 'element.changed'], function(e) {
        var element = e.element || null;
        if (!element || element.type !== 'bpmn:StartEvent') return;

        var bo = element.businessObject;
        if (McpUtil.hasMcpType(bo)) {
            removeAiOverlay(element, overlays);
            setTimeout(function() { addAiOverlay(element, overlays); }, 50);
        }
    });

    eventBus.on('import.done', function() {
        setTimeout(function() {
            elementRegistry.getAll().forEach(function(el) {
                if (el.type === 'bpmn:StartEvent' && McpUtil.hasMcpType(el.businessObject)) {
                    addAiOverlay(el, overlays);
                }
            });
        }, 200);
    });
}

McpToolOverlayProvider.$inject = ['eventBus', 'overlays', 'elementRegistry'];

function addAiOverlay(element, overlays) {
    if (!overlays) return;

    try {
        removeAiOverlay(element, overlays);
        var badge = domify(TEMPLATES.aiBadge);

        overlays.add(element, 'mcp-ai-badge', {
            position: { top: 0, left: 0 },
            html: badge
        });
    } catch (err) {
        console.log('[MCP Tool Plugin] Error adding overlay:', err.message);
    }
}

function removeAiOverlay(element, overlays) {
    if (!overlays) return;
    try {
        overlays.remove({ element: element, type: 'mcp-ai-badge' });
    } catch (err) {}
}

module.exports = McpToolOverlayProvider;


/***/ },

/***/ "./client/provider/McpToolPropertiesProvider.js"
/*!******************************************************!*\
  !*** ./client/provider/McpToolPropertiesProvider.js ***!
  \******************************************************/
(module, __unused_webpack_exports, __webpack_require__) {

"use strict";


var domify = __webpack_require__(/*! domify */ "./node_modules/domify/index.js");
var McpUtil = __webpack_require__(/*! ../util/McpUtil */ "./client/util/McpUtil.js");
var TEMPLATES = __webpack_require__(/*! ../templates */ "./client/templates.js");

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


/***/ },

/***/ "./client/provider/McpToolReplaceMenuProvider.js"
/*!*******************************************************!*\
  !*** ./client/provider/McpToolReplaceMenuProvider.js ***!
  \*******************************************************/
(module, __unused_webpack_exports, __webpack_require__) {

"use strict";


var McpUtil = __webpack_require__(/*! ../util/McpUtil */ "./client/util/McpUtil.js");

/**
 * Provider for MCP Tool replacement menu entries
 */
function McpToolReplaceMenuProvider(popupMenu, modeling, translate, selection, overlays) {
    this._modeling = modeling;
    this._translate = translate;
    this._selection = selection;
    this._overlays = overlays;

    popupMenu.registerProvider('bpmn-replace', this);
}

McpToolReplaceMenuProvider.$inject = ['popupMenu', 'modeling', 'translate', 'selection', 'overlays'];

McpToolReplaceMenuProvider.prototype.getPopupMenuEntries = function(element) {
    if (element.type !== 'bpmn:StartEvent') return {};

    var modeling = this._modeling;
    var selection = this._selection;
    var translate = this._translate;
    var overlays = this._overlays;

    return {
        'replace-with-mcp-tool-start': {
            label: translate('MCP Tool Start Event'),
            className: 'bpmn-icon-mcp-tool-start',
            action: function() {
                modeling.updateProperties(element, {
                    'mcp:type': 'mcpToolStart',
                    'mcp:toolName': '',
                    'mcp:description': ''
                });
                selection.select(element);
                setTimeout(function() {
                    addAiOverlay(element, overlays);
                }, 100);
            }
        }
    };
};

function addAiOverlay(element, overlays) {
    var domify = __webpack_require__(/*! domify */ "./node_modules/domify/index.js");
    var TEMPLATES = __webpack_require__(/*! ../templates */ "./client/templates.js");

    if (!overlays) return;

    try {
        removeAiOverlay(element, overlays);
        var badge = domify(TEMPLATES.aiBadge);

        overlays.add(element, 'mcp-ai-badge', {
            position: { top: 0, left: 0 },
            html: badge
        });
    } catch (err) {
        console.log('[MCP Tool Plugin] Error adding overlay:', err.message);
    }
}

function removeAiOverlay(element, overlays) {
    if (!overlays) return;
    try {
        overlays.remove({ element: element, type: 'mcp-ai-badge' });
    } catch (err) {}
}

module.exports = McpToolReplaceMenuProvider;


/***/ },

/***/ "./client/templates.js"
/*!*****************************!*\
  !*** ./client/templates.js ***!
  \*****************************/
(module) {

"use strict";


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


/***/ },

/***/ "./client/util/McpUtil.js"
/*!********************************!*\
  !*** ./client/util/McpUtil.js ***!
  \********************************/
(module) {

"use strict";


/**
 * Get MCP property from business object
 */
function getMcpProperty(bo, prop) {
    return bo.get(prop);
}

/**
 * Check if element has MCP type
 */
function hasMcpType(bo) {
    return getMcpProperty(bo, 'mcp:type') === 'mcpToolStart';
}

/**
 * Get extension elements from business object
 */
function getExtensionElements(bo) {
    return bo.get('extensionElements');
}

/**
 * Get MCP parameters from business object
 */
function getMcpParameters(bo) {
    var extensionElements = getExtensionElements(bo);
    if (!extensionElements) return null;

    var values = extensionElements.get('values');
    return values ? values.find(function(v) { return v.$type === 'mcp:Parameters'; }) : null;
}

/**
 * Get parameters array from business object
 */
function getParameters(bo) {
    var mcpParams = getMcpParameters(bo);
    return mcpParams ? (mcpParams.get('parameters') || []) : [];
}

/**
 * Update moddle properties
 */
function updateModdle(element, bo, modeling) {
    modeling.updateModdleProperties(element, bo, {
        extensionElements: getExtensionElements(bo)
    });
}

module.exports = {
    getMcpProperty: getMcpProperty,
    hasMcpType: hasMcpType,
    getExtensionElements: getExtensionElements,
    getMcpParameters: getMcpParameters,
    getParameters: getParameters,
    updateModdle: updateModdle
};


/***/ },

/***/ "./node_modules/camunda-modeler-plugin-helpers/index.js"
/*!**************************************************************!*\
  !*** ./node_modules/camunda-modeler-plugin-helpers/index.js ***!
  \**************************************************************/
(__unused_webpack_module, __webpack_exports__, __webpack_require__) {

"use strict";
__webpack_require__.r(__webpack_exports__);
/* harmony export */ __webpack_require__.d(__webpack_exports__, {
/* harmony export */   getModelerDirectory: () => (/* binding */ getModelerDirectory),
/* harmony export */   getPluginsDirectory: () => (/* binding */ getPluginsDirectory),
/* harmony export */   registerBpmnJSModdleExtension: () => (/* binding */ registerBpmnJSModdleExtension),
/* harmony export */   registerBpmnJSPlugin: () => (/* binding */ registerBpmnJSPlugin),
/* harmony export */   registerClientPlugin: () => (/* binding */ registerClientPlugin)
/* harmony export */ });
/**
 * Validate and register a client plugin.
 *
 * @param {Object} plugin
 * @param {String} type
 */
function registerClientPlugin(plugin, type) {
  var plugins = window.plugins || [];
  window.plugins = plugins;

  if (!plugin) {
    throw new Error('plugin not specified');
  }

  if (!type) {
    throw new Error('type not specified');
  }

  plugins.push({
    plugin: plugin,
    type: type
  });
}

/**
 * Validate and register a bpmn-js plugin.
 *
 * @param {Object} module
 *
 * @example
 *
 * import {
 *   registerBpmnJSPlugin
 * } from 'camunda-modeler-plugin-helpers';
 *
 * const BpmnJSModule = {
 *   __init__: [ 'myService' ],
 *   myService: [ 'type', ... ]
 * };
 *
 * registerBpmnJSPlugin(BpmnJSModule);
 */
function registerBpmnJSPlugin(module) {
  registerClientPlugin(module, 'bpmn.modeler.additionalModules');
}

/**
 * Validate and register a bpmn-moddle extension plugin.
 *
 * @param {Object} descriptor
 *
 * @example
 * import {
 *   registerBpmnJSModdleExtension
 * } from 'camunda-modeler-plugin-helpers';
 *
 * var moddleDescriptor = {
 *   name: 'my descriptor',
 *   uri: 'http://example.my.company.localhost/schema/my-descriptor/1.0',
 *   prefix: 'mydesc',
 *
 *   ...
 * };
 *
 * registerBpmnJSModdleExtension(moddleDescriptor);
 */
function registerBpmnJSModdleExtension(descriptor) {
  registerClientPlugin(descriptor, 'bpmn.modeler.moddleExtension');
}

/**
 * Return the modeler directory, as a string.
 *
 * @deprecated Will be removed in future Camunda Modeler versions without replacement.
 *
 * @return {String}
 */
function getModelerDirectory() {
  return window.getModelerDirectory();
}

/**
 * Return the modeler plugin directory, as a string.
 *
 * @deprecated Will be removed in future Camunda Modeler versions without replacement.
 *
 * @return {String}
 */
function getPluginsDirectory() {
  return window.getPluginsDirectory();
}

/***/ },

/***/ "./node_modules/domify/index.js"
/*!**************************************!*\
  !*** ./node_modules/domify/index.js ***!
  \**************************************/
(module) {


/**
 * Expose `parse`.
 */

module.exports = parse;

/**
 * Tests for browser support.
 */

var innerHTMLBug = false;
var bugTestDiv;
if (typeof document !== 'undefined') {
  bugTestDiv = document.createElement('div');
  // Setup
  bugTestDiv.innerHTML = '  <link/><table></table><a href="/a">a</a><input type="checkbox"/>';
  // Make sure that link elements get serialized correctly by innerHTML
  // This requires a wrapper element in IE
  innerHTMLBug = !bugTestDiv.getElementsByTagName('link').length;
  bugTestDiv = undefined;
}

/**
 * Wrap map from jquery.
 */

var map = {
  legend: [1, '<fieldset>', '</fieldset>'],
  tr: [2, '<table><tbody>', '</tbody></table>'],
  col: [2, '<table><tbody></tbody><colgroup>', '</colgroup></table>'],
  // for script/link/style tags to work in IE6-8, you have to wrap
  // in a div with a non-whitespace character in front, ha!
  _default: innerHTMLBug ? [1, 'X<div>', '</div>'] : [0, '', '']
};

map.td =
map.th = [3, '<table><tbody><tr>', '</tr></tbody></table>'];

map.option =
map.optgroup = [1, '<select multiple="multiple">', '</select>'];

map.thead =
map.tbody =
map.colgroup =
map.caption =
map.tfoot = [1, '<table>', '</table>'];

map.polyline =
map.ellipse =
map.polygon =
map.circle =
map.text =
map.line =
map.path =
map.rect =
map.g = [1, '<svg xmlns="http://www.w3.org/2000/svg" version="1.1">','</svg>'];

/**
 * Parse `html` and return a DOM Node instance, which could be a TextNode,
 * HTML DOM Node of some kind (<div> for example), or a DocumentFragment
 * instance, depending on the contents of the `html` string.
 *
 * @param {String} html - HTML string to "domify"
 * @param {Document} doc - The `document` instance to create the Node for
 * @return {DOMNode} the TextNode, DOM Node, or DocumentFragment instance
 * @api private
 */

function parse(html, doc) {
  if ('string' != typeof html) throw new TypeError('String expected');

  // default to the global `document` object
  if (!doc) doc = document;

  // tag name
  var m = /<([\w:]+)/.exec(html);
  if (!m) return doc.createTextNode(html);

  html = html.replace(/^\s+|\s+$/g, ''); // Remove leading/trailing whitespace

  var tag = m[1];

  // body support
  if (tag == 'body') {
    var el = doc.createElement('html');
    el.innerHTML = html;
    return el.removeChild(el.lastChild);
  }

  // wrap map
  var wrap = Object.prototype.hasOwnProperty.call(map, tag) ? map[tag] : map._default;
  var depth = wrap[0];
  var prefix = wrap[1];
  var suffix = wrap[2];
  var el = doc.createElement('div');
  el.innerHTML = prefix + html + suffix;
  while (depth--) el = el.lastChild;

  // one element
  if (el.firstChild == el.lastChild) {
    return el.removeChild(el.firstChild);
  }

  // several elements
  var fragment = doc.createDocumentFragment();
  while (el.firstChild) {
    fragment.appendChild(el.removeChild(el.firstChild));
  }

  return fragment;
}


/***/ }

/******/ 	});
/************************************************************************/
/******/ 	// The module cache
/******/ 	var __webpack_module_cache__ = {};
/******/ 	
/******/ 	// The require function
/******/ 	function __webpack_require__(moduleId) {
/******/ 		// Check if module is in cache
/******/ 		var cachedModule = __webpack_module_cache__[moduleId];
/******/ 		if (cachedModule !== undefined) {
/******/ 			return cachedModule.exports;
/******/ 		}
/******/ 		// Check if module exists (development only)
/******/ 		if (__webpack_modules__[moduleId] === undefined) {
/******/ 			var e = new Error("Cannot find module '" + moduleId + "'");
/******/ 			e.code = 'MODULE_NOT_FOUND';
/******/ 			throw e;
/******/ 		}
/******/ 		// Create a new module (and put it into the cache)
/******/ 		var module = __webpack_module_cache__[moduleId] = {
/******/ 			// no module.id needed
/******/ 			// no module.loaded needed
/******/ 			exports: {}
/******/ 		};
/******/ 	
/******/ 		// Execute the module function
/******/ 		__webpack_modules__[moduleId](module, module.exports, __webpack_require__);
/******/ 	
/******/ 		// Return the exports of the module
/******/ 		return module.exports;
/******/ 	}
/******/ 	
/************************************************************************/
/******/ 	/* webpack/runtime/define property getters */
/******/ 	(() => {
/******/ 		// define getter functions for harmony exports
/******/ 		__webpack_require__.d = (exports, definition) => {
/******/ 			for(var key in definition) {
/******/ 				if(__webpack_require__.o(definition, key) && !__webpack_require__.o(exports, key)) {
/******/ 					Object.defineProperty(exports, key, { enumerable: true, get: definition[key] });
/******/ 				}
/******/ 			}
/******/ 		};
/******/ 	})();
/******/ 	
/******/ 	/* webpack/runtime/hasOwnProperty shorthand */
/******/ 	(() => {
/******/ 		__webpack_require__.o = (obj, prop) => (Object.prototype.hasOwnProperty.call(obj, prop))
/******/ 	})();
/******/ 	
/******/ 	/* webpack/runtime/make namespace object */
/******/ 	(() => {
/******/ 		// define __esModule on exports
/******/ 		__webpack_require__.r = (exports) => {
/******/ 			if(typeof Symbol !== 'undefined' && Symbol.toStringTag) {
/******/ 				Object.defineProperty(exports, Symbol.toStringTag, { value: 'Module' });
/******/ 			}
/******/ 			Object.defineProperty(exports, '__esModule', { value: true });
/******/ 		};
/******/ 	})();
/******/ 	
/************************************************************************/
var __webpack_exports__ = {};
// This entry needs to be wrapped in an IIFE because it needs to be in strict mode.
(() => {
"use strict";
/*!**************************!*\
  !*** ./client/client.js ***!
  \**************************/


var registerBpmnJSPlugin = (__webpack_require__(/*! camunda-modeler-plugin-helpers */ "./node_modules/camunda-modeler-plugin-helpers/index.js").registerBpmnJSPlugin);
var registerBpmnJSModdleExtension = (__webpack_require__(/*! camunda-modeler-plugin-helpers */ "./node_modules/camunda-modeler-plugin-helpers/index.js").registerBpmnJSModdleExtension);

var McpToolReplaceMenuProvider = __webpack_require__(/*! ./provider/McpToolReplaceMenuProvider */ "./client/provider/McpToolReplaceMenuProvider.js");
var McpToolOverlayProvider = __webpack_require__(/*! ./provider/McpToolOverlayProvider */ "./client/provider/McpToolOverlayProvider.js");
var McpToolPropertiesProvider = __webpack_require__(/*! ./provider/McpToolPropertiesProvider */ "./client/provider/McpToolPropertiesProvider.js");

console.log('[MCP Tool Plugin] client.js loaded');

// ============================================
// MODDLE EXTENSION
// ============================================
registerBpmnJSModdleExtension({
  name: 'mcp',
  prefix: 'mcp',
  uri: 'http://fluxnova.finos.org/schema/1.0/ai/mcp',
  xml: { tagAlias: 'lowerCase' },
  types: [
    {
      name: 'McpProperties',
      isAbstract: true,
      extends: ['bpmn:StartEvent'],
      properties: [
        { name: 'type', isAttr: true, type: 'String' },
        { name: 'toolName', isAttr: true, type: 'String' },
        { name: 'description', isAttr: true, type: 'String' },
        { name: 'propagateBusinessKey', isAttr: true, type: 'Boolean', default: true }
      ]
    },
    {
      name: 'Parameters',
      superClass: ['Element'],
      properties: [{ name: 'parameters', type: 'mcp:Parameter', isMany: true }]
    },
    {
      name: 'Parameter',
      superClass: ['Element'],
      properties: [
        { name: 'paramName', isAttr: true, type: 'String' },
        { name: 'paramType', isAttr: true, type: 'String' }
      ]
    }
  ]
});

// ============================================
// MODULE REGISTRATION
// ============================================
registerBpmnJSPlugin({
  __init__: ['mcpToolReplaceMenuProvider', 'mcpToolPropertiesProvider', 'mcpToolOverlayProvider'],
  mcpToolReplaceMenuProvider: ['type', McpToolReplaceMenuProvider],
  mcpToolPropertiesProvider: ['type', McpToolPropertiesProvider],
  mcpToolOverlayProvider: ['type', McpToolOverlayProvider]
});

console.log('[MCP Tool Plugin] Plugin registered');

})();

/******/ })()
;
//# sourceMappingURL=client.bundle.js.map