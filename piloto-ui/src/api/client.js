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

// ─── Daily Closing API ───────────────────────────────────────────────────────

/**
 * Execute a daily closing snapshot.
 */
export async function executeDailyClosing(tenantId = 'tenant_001') {
    return request(ENDPOINTS.DAILY_CLOSING, {
        method: 'POST',
        body: JSON.stringify({ tenantId }),
    });
}

// ─── Cognitive Evolution Engine API ──────────────────────────────────────────

/**
 * Fetch the MAP-Elites grid state.
 */
export async function fetchEvolutionGrid() {
    return request(ENDPOINTS.EVOLUTION_GRID);
}

/**
 * Get optimal variant for a behavioral cell.
 */
export async function fetchEvolutionVariant(intentType, sessionStage, tenantId = 'default') {
    const params = new URLSearchParams({ intentType, sessionStage, tenantId });
    return request(`${ENDPOINTS.EVOLUTION_VARIANT}?${params}`);
}

/**
 * Register a new prompt variant (seed) in a behavioral cell.
 */
export async function registerEvolutionVariant(data) {
    return request(ENDPOINTS.EVOLUTION_REGISTER, {
        method: 'POST',
        body: JSON.stringify(data),
    });
}

/**
 * Report an outcome for a variant.
 */
export async function reportEvolutionOutcome(data) {
    return request(ENDPOINTS.EVOLUTION_OUTCOME, {
        method: 'POST',
        body: JSON.stringify(data),
    });
}

/**
 * Trigger OPRO + PromptBreeder optimization cycle.
 */
export async function triggerEvolutionOptimization(tenantId = 'default') {
    return request(`${ENDPOINTS.EVOLUTION_OPTIMIZE}?tenantId=${tenantId}`, {
        method: 'POST',
    });
}
