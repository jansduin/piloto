/**
 * Centralized configuration for the PILOTO UI.
 * All backend URLs and shared constants live here.
 * 
 * For production, set VITE_API_BASE_URL in .env or .env.production
 */

// API Base URL — resolved from environment or fallback to localhost
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

// Derived endpoints
export const ENDPOINTS = {
    CHAT_MESSAGE: `${API_BASE_URL}/api/chat/message`,
    GOV_PROMPTS: `${API_BASE_URL}/gov/prompts`,
    GOV_MEMORY: `${API_BASE_URL}/gov/memory`,
    GOV_TELEMETRY: `${API_BASE_URL}/api/governance/telemetry`,
    GOV_HEALTH: `${API_BASE_URL}/api/governance/health`,
};

// Agent definitions removed — PILOTO is a universal execution matrix, not restricted to fixed agents.

// Domain options — DEPRECATED: PILOTO is a universal matrix. 
// Use dynamic input instead of fixed lists to avoid "encajonamiento".
export const DOMAINS = [
    { value: 'general', label: 'General' },
    // Static domains removed to honor universal architecture vision
];

// Execution modes
export const EXECUTION_MODES = [
    { value: 'NORMAL', label: 'Normal' },
    { value: 'STRESS_TEST', label: 'Stress Test' },
    { value: 'HIGH_RISK_SIMULATION', label: 'High Risk Simulation' },
];

// Default session config
export const DEFAULT_TENANT_ID = 'tenant_001';
