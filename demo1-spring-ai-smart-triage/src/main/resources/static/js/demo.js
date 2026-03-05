// =====================================================
// E-Commerce Customer Intake — Demo UI
// Chat, SSE pipeline events, hotkey scenarios, auto-demo
// =====================================================

const chatMessages = document.getElementById('chat-messages');
const pipelineLog = document.getElementById('pipeline-log');
const chatInput = document.getElementById('chat-input');
const sendBtn = document.getElementById('send-btn');
const demoBtn = document.getElementById('demo-btn');

let isProcessing = false;

const SCENARIOS = [
    "My order ORD_A1B2C3 was supposed to arrive 5 days ago and still nothing! " +
    "This is the third time seller_42 has let me down.",
    "The product doesn't match the description at all. Seller seller_42 has terrible reviews " +
    "and I want answers. My customer ID is cust_1234.",
    "Order ORD_X7Y8Z9 is late, the product I already received from the same seller was broken, " +
    "AND I want a refund for both!"
];

// =====================================================
// HOTKEY SCENARIOS
// =====================================================

function runScenario(index) {
    if (isProcessing) return;
    var text = SCENARIOS[index];
    chatInput.value = '';
    sendMessage(text);
}

// Listen for keyboard shortcuts
document.addEventListener('keydown', function(e) {
    if (isProcessing) return;
    if (e.target.tagName === 'TEXTAREA' || e.target.tagName === 'INPUT') return;
    if (e.key === '1') runScenario(0);
    if (e.key === '2') runScenario(1);
    if (e.key === '3') runScenario(2);
    if (e.key === 'r' || e.key === 'R') runDemo();
});

// =====================================================
// CHAT FUNCTIONS
// =====================================================

function sendFromInput() {
    var text = chatInput.value.trim();
    if (!text || isProcessing) return;
    chatInput.value = '';
    autoResizeTextarea();
    sendMessage(text);
}

function sendMessage(text) {
    if (isProcessing) return;
    isProcessing = true;
    updateButtonStates();

    addMessage('user', text);
    showTypingIndicator();
    addQueryMarker(text);

    var url = '/api/chat?query=' + encodeURIComponent(text);
    var eventSource = new EventSource(url);

    eventSource.addEventListener('routing', function(e) {
        var data = JSON.parse(e.data);
        addRoutingEntry(data);
    });

    eventSource.addEventListener('handling', function(e) {
        var data = JSON.parse(e.data);
        addHandlingEntry(data);
    });

    eventSource.addEventListener('tool_call', function(e) {
        var data = JSON.parse(e.data);
        addToolCallEntry(data);
    });

    eventSource.addEventListener('draft', function(e) {
        var data = JSON.parse(e.data);
        addDraftEntry(data);
    });

    eventSource.addEventListener('evaluation', function(e) {
        var data = JSON.parse(e.data);
        addEvaluationEntry(data);
    });

    eventSource.addEventListener('refinement', function(e) {
        var data = JSON.parse(e.data);
        addRefinementEntry(data);
    });

    eventSource.addEventListener('status', function(e) {
        var data = JSON.parse(e.data);
        addStatusEntry(data);
    });

    eventSource.addEventListener('handoff', function(e) {
        var data = JSON.parse(e.data);
        addHandoffEntry(data);
    });

    eventSource.addEventListener('memory', function(e) {
        var data = JSON.parse(e.data);
        addMemoryEntry(data);
    });

    eventSource.addEventListener('complete', function(e) {
        var data = JSON.parse(e.data);
        hideTypingIndicator();
        addMessage('assistant', data.finalResponse);
        addCompleteEntry();
        eventSource.close();
        isProcessing = false;
        updateButtonStates();
    });

    eventSource.addEventListener('error', function(e) {
        if (e.data) {
            var data = JSON.parse(e.data);
            hideTypingIndicator();
            addMessage('system', 'Error: ' + data.message);
        }
        eventSource.close();
        isProcessing = false;
        updateButtonStates();
    });

    eventSource.onerror = function() {
        hideTypingIndicator();
        eventSource.close();
        if (isProcessing) {
            isProcessing = false;
            updateButtonStates();
        }
    };
}

function handleKeyDown(event) {
    if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault();
        sendFromInput();
    }
}

chatInput.addEventListener('input', autoResizeTextarea);

function autoResizeTextarea() {
    chatInput.style.height = 'auto';
    chatInput.style.height = Math.min(chatInput.scrollHeight, 120) + 'px';
}

// =====================================================
// MESSAGE RENDERING
// =====================================================

function addMessage(role, content) {
    removePlaceholder(chatMessages);

    var wrapper = document.createElement('div');

    if (role === 'user') {
        wrapper.className = 'message user-message';
        wrapper.innerHTML =
            '<div class="message-avatar">U</div>' +
            '<div class="message-content">' + escapeHtml(content) + '</div>';
    } else if (role === 'assistant') {
        wrapper.className = 'message assistant-message';
        var rendered = renderMarkdown(content);
        wrapper.innerHTML =
            '<div class="message-avatar">AI</div>' +
            '<div class="message-content">' + rendered + '</div>';
    } else {
        wrapper.className = 'system-message';
        wrapper.innerHTML = '<div class="message-content">' + content + '</div>';
    }

    chatMessages.appendChild(wrapper);
    scrollToBottom(chatMessages);
}

function showTypingIndicator() {
    var existing = document.getElementById('typing-indicator');
    if (existing) return;

    var wrapper = document.createElement('div');
    wrapper.className = 'message assistant-message';
    wrapper.id = 'typing-indicator';
    wrapper.innerHTML =
        '<div class="message-avatar">AI</div>' +
        '<div class="message-content">' +
        '<div class="typing-indicator"><span></span><span></span><span></span></div>' +
        '</div>';

    chatMessages.appendChild(wrapper);
    scrollToBottom(chatMessages);
}

function hideTypingIndicator() {
    var indicator = document.getElementById('typing-indicator');
    if (indicator) indicator.remove();
}

// =====================================================
// PIPELINE LOG ENTRIES
// =====================================================

function addQueryMarker(text) {
    removePlaceholder(pipelineLog);

    var entry = document.createElement('div');
    entry.className = 'log-entry query-marker';
    var truncated = text.length > 80 ? text.substring(0, 80) + '...' : text;
    entry.innerHTML =
        '<div class="log-entry-header">' +
        '<span class="log-icon">&#9654;</span>' +
        '<span class="log-label">Customer Issue</span>' +
        '</div>' +
        '<div class="log-entry-body">' + escapeHtml(truncated) + '</div>';

    pipelineLog.appendChild(entry);
    scrollToBottom(pipelineLog);
}

function addRoutingEntry(data) {
    removePlaceholder(pipelineLog);

    var scoresHtml = '';
    if (data.scores && data.scores.length > 0) {
        scoresHtml = '<div class="score-list">';
        data.scores.forEach(function(s) {
            var isSelected = s.category === data.route;
            var pct = Math.round(s.confidence * 100);
            scoresHtml +=
                '<div class="score-item">' +
                '<span class="score-category">' + formatCategory(s.category) + '</span>' +
                '<div class="score-bar-container">' +
                '<div class="score-bar' + (isSelected ? ' selected' : '') + '" style="width:' + pct + '%"></div>' +
                '</div>' +
                '<span class="score-value">' + s.confidence.toFixed(2) + '</span>' +
                (isSelected ? '<span class="score-marker">&#9668;</span>' : '') +
                '</div>';
        });
        scoresHtml += '</div>';
    }

    var entry = document.createElement('div');
    entry.className = 'log-entry routing';
    entry.innerHTML =
        '<div class="log-entry-header">' +
        '<span class="log-icon">&#128268;</span>' +
        '<span class="log-label">Routing</span>' +
        '<span class="log-meta">' + formatCategory(data.route) + '</span>' +
        '</div>' +
        scoresHtml;

    pipelineLog.appendChild(entry);
    scrollToBottom(pipelineLog);
}

function addHandlingEntry(data) {
    var entry = document.createElement('div');
    entry.className = 'log-entry handling';
    entry.innerHTML =
        '<div class="log-entry-header">' +
        '<span class="log-icon">&#128295;</span>' +
        '<span class="log-label">Expert</span>' +
        '<span class="log-meta">' + escapeHtml(data.handler) + '</span>' +
        '</div>';

    pipelineLog.appendChild(entry);
    scrollToBottom(pipelineLog);
}

function addToolCallEntry(data) {
    var toolIcons = {
        'lookupOrder': '&#128230;',
        'checkDeliveryStatus': '&#128666;',
        'getCustomerHistory': '&#128100;',
        'getSellerRating': '&#11088;',
        'getProductInfo': '&#128722;'
    };

    var entry = document.createElement('div');
    entry.className = 'log-entry tool-call';
    var icon = toolIcons[data.tool] || '&#128295;';
    entry.innerHTML =
        '<div class="log-entry-header">' +
        '<span class="log-icon">' + icon + '</span>' +
        '<span class="log-label">@Tool</span>' +
        '<span class="log-meta">' + escapeHtml(data.tool) + '</span>' +
        '</div>';

    pipelineLog.appendChild(entry);
    scrollToBottom(pipelineLog);
}

function addHandoffEntry(data) {
    var entry = document.createElement('div');
    entry.className = 'log-entry handoff';
    entry.innerHTML =
        '<div class="log-entry-header">' +
        '<span class="log-icon">&#128073;</span>' +
        '<span class="log-label">Handoff</span>' +
        '<span class="log-meta">' + escapeHtml(data.from) + ' &#x2192; ' + escapeHtml(data.to) + '</span>' +
        '</div>' +
        (data.context ?
            '<div class="log-detail">' + escapeHtml(data.context) + '</div>' : '');

    pipelineLog.appendChild(entry);
    scrollToBottom(pipelineLog);
}

function addMemoryEntry(data) {
    var isSearch = data.operation === 'search';
    var entry = document.createElement('div');
    entry.className = 'log-entry memory';
    entry.innerHTML =
        '<div class="log-entry-header">' +
        '<span class="log-icon">' + (isSearch ? '&#128270;' : '&#128190;') + '</span>' +
        '<span class="log-label">' + (isSearch ? 'Memory Search' : 'Memory Store') + '</span>' +
        '<span class="log-meta">' + escapeHtml(data.detail) + '</span>' +
        '</div>';

    pipelineLog.appendChild(entry);
    scrollToBottom(pipelineLog);
}

function addDraftEntry(data) {
    var entry = document.createElement('div');
    entry.className = 'log-entry draft';
    entry.innerHTML =
        '<div class="log-entry-header">' +
        '<span class="log-icon">&#128196;</span>' +
        '<span class="log-label">Draft Generated</span>' +
        '</div>';

    pipelineLog.appendChild(entry);
    scrollToBottom(pipelineLog);
}

function addEvaluationEntry(data) {
    var pass = data.score >= 3;
    var entry = document.createElement('div');
    entry.className = 'log-entry evaluation';
    entry.innerHTML =
        '<div class="log-entry-header">' +
        '<span class="log-icon">' + (pass ? '&#9989;' : '&#128260;') + '</span>' +
        '<span class="log-label">Evaluator-Optimizer</span>' +
        '<span class="log-meta">iteration ' + data.iteration + '</span>' +
        '</div>' +
        '<div class="eval-score">' +
        '<span class="eval-score-value ' + (pass ? 'pass' : 'fail') + '">' + data.score + '/4</span>' +
        '<span class="eval-score-label">quality score</span>' +
        '<span class="eval-status ' + (pass ? 'pass' : 'refining') + '">' +
        (pass ? 'PASS' : 'refining...') + '</span>' +
        '</div>' +
        (data.feedback ?
            '<details><summary>Feedback</summary>' +
            '<div class="detail-content">' + escapeHtml(data.feedback) + '</div></details>' : '');

    pipelineLog.appendChild(entry);
    scrollToBottom(pipelineLog);
}

function addRefinementEntry(data) {
    var entry = document.createElement('div');
    entry.className = 'log-entry evaluation';
    entry.innerHTML =
        '<div class="log-entry-header">' +
        '<span class="log-icon">&#128260;</span>' +
        '<span class="log-label">Refinement</span>' +
        '</div>';

    pipelineLog.appendChild(entry);
    scrollToBottom(pipelineLog);
}

function addStatusEntry(data) {
    var entry = document.createElement('div');
    entry.className = 'log-entry status';
    entry.innerHTML =
        '<div class="log-entry-header">' +
        '<span class="log-icon">&#128308;</span>' +
        '<span class="log-label">' + escapeHtml(data.phase) + '</span>' +
        '<span class="log-meta">' + escapeHtml(data.detail) + '</span>' +
        '</div>';

    pipelineLog.appendChild(entry);
    scrollToBottom(pipelineLog);
}

function addCompleteEntry() {
    var entry = document.createElement('div');
    entry.className = 'log-entry complete';
    entry.innerHTML =
        '<div class="log-entry-header">' +
        '<span class="log-icon">&#10004;</span>' +
        '<span class="log-label">Complete</span>' +
        '</div>';

    pipelineLog.appendChild(entry);
    scrollToBottom(pipelineLog);
}

// =====================================================
// AUTO-DEMO MODE
// =====================================================

function runDemo() {
    if (isProcessing) return;
    isProcessing = true;
    updateButtonStates();

    addMessage('system', 'Starting auto-demo: 3 scenarios demonstrating Routing, Tool Use (@Tool), and Evaluator-Optimizer');

    fetchSSE('/api/demo/stream', 'GET', null, {
        query: function(data) {
            typeText(chatInput, data.text, 25).then(function() {
                addMessage('user', data.text);
                chatInput.value = '';
                autoResizeTextarea();
                showTypingIndicator();
                addQueryMarker(data.text);
            });
        },
        routing: function(data) {
            addRoutingEntry(data);
        },
        handling: function(data) {
            addHandlingEntry(data);
        },
        tool_call: function(data) {
            addToolCallEntry(data);
        },
        draft: function(data) {
            addDraftEntry(data);
        },
        evaluation: function(data) {
            addEvaluationEntry(data);
        },
        refinement: function(data) {
            addRefinementEntry(data);
        },
        status: function(data) {
            addStatusEntry(data);
        },
        complete: function(data) {
            hideTypingIndicator();
            addMessage('assistant', data.finalResponse);
            addCompleteEntry();
        },
        error: function(data) {
            hideTypingIndicator();
            addMessage('system', 'Error: ' + escapeHtml(data.message));
        },
        _done: function() {
            hideTypingIndicator();
            addMessage('system', 'Demo complete! All 3 patterns demonstrated.');
            isProcessing = false;
            updateButtonStates();
        },
        _error: function() {
            hideTypingIndicator();
            isProcessing = false;
            updateButtonStates();
        }
    });
}

// =====================================================
// FETCH-BASED SSE (for POST and GET endpoints)
// =====================================================

function fetchSSE(url, method, body, handlers) {
    var options = {
        method: method,
        headers: {}
    };
    if (body) {
        options.headers['Content-Type'] = 'application/json';
        options.body = JSON.stringify(body);
    }

    fetch(url, options).then(function(response) {
        if (!response.ok) {
            throw new Error('HTTP ' + response.status);
        }
        var reader = response.body.getReader();
        var decoder = new TextDecoder();
        var buffer = '';

        function read() {
            reader.read().then(function(result) {
                if (result.done) {
                    if (handlers._done) handlers._done();
                    return;
                }

                buffer += decoder.decode(result.value, { stream: true });
                var lines = buffer.split('\n');
                buffer = lines.pop();

                var currentEvent = null;
                for (var i = 0; i < lines.length; i++) {
                    var line = lines[i];
                    if (line.startsWith('event:')) {
                        currentEvent = line.substring(6).trim();
                    } else if (line.startsWith('data:') && currentEvent) {
                        var dataStr = line.substring(5).trim();
                        try {
                            var data = JSON.parse(dataStr);
                            if (handlers[currentEvent]) {
                                handlers[currentEvent](data);
                            }
                        } catch (e) {
                            // ignore parse errors
                        }
                        currentEvent = null;
                    } else if (line === '') {
                        currentEvent = null;
                    }
                }

                read();
            }).catch(function(err) {
                if (handlers._error) handlers._error(err);
            });
        }

        read();
    }).catch(function(err) {
        if (handlers._error) handlers._error(err);
    });
}

// =====================================================
// TYPING ANIMATION
// =====================================================

function typeText(element, text, speed) {
    return new Promise(function(resolve) {
        element.value = '';
        var i = 0;
        function typeChar() {
            if (i < text.length) {
                element.value += text.charAt(i);
                autoResizeTextarea();
                i++;
                setTimeout(typeChar, speed);
            } else {
                resolve();
            }
        }
        typeChar();
    });
}

// =====================================================
// CLEAR FUNCTIONS
// =====================================================

function clearChat() {
    chatMessages.innerHTML =
        '<div class="system-message">' +
        '<div class="message-content">' +
        'Chat cleared. Use the hotkey buttons or type a customer complaint below.' +
        '</div></div>';
}

function clearLog() {
    pipelineLog.innerHTML = '<p class="log-placeholder">Pipeline events will appear here as queries are processed.</p>';
}

// =====================================================
// UTILITY FUNCTIONS
// =====================================================

function updateButtonStates() {
    sendBtn.disabled = isProcessing;
    demoBtn.disabled = isProcessing;

    var hotkeys = document.querySelectorAll('.btn-hotkey');
    hotkeys.forEach(function(btn) { btn.disabled = isProcessing; });

    if (isProcessing) {
        demoBtn.textContent = 'Running...';
    } else {
        demoBtn.textContent = 'Run Demo';
    }
}

function scrollToBottom(element) {
    requestAnimationFrame(function() {
        element.scrollTop = element.scrollHeight;
    });
}

function removePlaceholder(container) {
    var placeholder = container.querySelector('.log-placeholder');
    if (placeholder) placeholder.remove();
}

function escapeHtml(text) {
    if (!text) return '';
    var div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function renderMarkdown(text) {
    if (!text) return '';
    if (typeof marked !== 'undefined') {
        try {
            return marked.parse(text);
        } catch (e) {
            return escapeHtml(text);
        }
    }
    return escapeHtml(text);
}

function formatCategory(cat) {
    if (!cat) return '';
    return cat.replace(/_/g, ' ').replace(/\b\w/g, function(c) { return c.toUpperCase(); });
}
