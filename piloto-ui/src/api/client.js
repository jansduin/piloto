/**
 * Decoupled API client for PILOTO UI.
 * All backend communication goes through this module.
 */
import { ENDPOINTS, DEFAULT_TENANT_ID } from './config.js';

/**
 * Internal fetch wrapper with common error handling.
 */
async function request(url, options = {}) {
    const config = {
        headers: { 'Content-Type': 'application/json', ...options.headers },
        ...options,
    };

    const response = await fetch(url, config);

    if (!response.ok) {
        const errorText = await response.text().catch(() => 'Unknown error');
        throw new Error(`HTTP ${response.status}: ${errorText}`);
    }

    return response.json();
}

// ─── Chat API ─────────────────────────────────────────────────────────────────

/**
 * Send a chat message to the deliberation engine.
 * @param {string} message - The user message
 * @param {string} sessionId - Current session ID
 * @param {object} options - Optional overrides: { role, domain, executionMode, tenantId }
 * @returns {Promise<object>} API response with { success, response, metadata, error }
 */
export async function sendChatMessage(message, sessionId, options = {}) {
    return request(ENDPOINTS.CHAT_MESSAGE, {
        method: 'POST',
        body: JSON.stringify({
            message,
            sessionId,
            tenantId: options.tenantId || DEFAULT_TENANT_ID,
            domain: options.domain,
            executionMode: options.executionMode,
            metadata: { role: options.role || 'chat_agent' },
        }),
    });
}

// ─── Governance API ───────────────────────────────────────────────────────────

/**
 * Fetch all registered prompts.
 */
export async function fetchPrompts() {
    return request(ENDPOINTS.GOV_PROMPTS);
}

/**
 * Update a prompt's content via Governance Command.
 * Follows CQRS Mandatory (No PUT) doctrine.
 */
export async function updatePrompt(id, content) {
    return request(`${ENDPOINTS.GOV_PROMPTS}/commands/update-content`, {
        method: 'POST',
        body: JSON.stringify({
            id,
            content,
            commandType: 'UPDATE_GOAL',
            timestamp: new Date().toISOString()
        }),
    });
}

/**
 * Register a new prompt via Governance Command.
 */
export async function registerPrompt(promptData) {
    return request(ENDPOINTS.GOV_PROMPTS, {
        method: 'POST',
        body: JSON.stringify(promptData),
    });
}

/**
 * Fetch all domain memory items.
 */
export async function fetchMemoryItems() {
    return request(ENDPOINTS.GOV_MEMORY);
}

/**
 * Update a memory item's content via Governance Command.
 * Follows CQRS Mandatory (No PUT) doctrine.
 */
export async function updateMemoryItem(id, content) {
    return request(`${ENDPOINTS.GOV_MEMORY}/commands/update-content`, {
        method: 'POST',
        body: JSON.stringify({
            id,
            content,
            commandType: 'UPDATE_STATE',
            timestamp: new Date().toISOString()
        }),
    });
}

/**
 * Fetch system telemetry events for Audit Log.
 */
export async function fetchTelemetry() {
    return request(ENDPOINTS.GOV_TELEMETRY);
}
/**
 * Fetch system health metrics.
 */
export async function fetchHealth() {
    return request(ENDPOINTS.GOV_HEALTH);
}
