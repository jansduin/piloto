/**
 * WebSocket client for real-time deliberation streaming.
 *
 * Connects to the PILOTO backend via native WebSocket (with STOMP framing).
 * Subscribes to /topic/deliberation/{sessionId} to receive stage events.
 *
 * Usage:
 *   import { DeliberationStream } from './deliberationStream.js';
 *
 *   const stream = new DeliberationStream('sess-123', (event) => {
 *     console.log(event.stage, event.status, event.content);
 *   });
 *   stream.connect();
 *   // later:
 *   stream.disconnect();
 */

import { API_BASE_URL } from './config.js';

// WS URL derived from HTTP base (http → ws, https → wss)
const WS_BASE = API_BASE_URL.replace(/^http/, 'ws');

const STOMP_CONNECT_FRAME = 'CONNECT\naccept-version:1.1,1.0\nheart-beat:0,0\n\n\0';

/**
 * Real-time deliberation stream client.
 * Wraps a native WebSocket with lightweight STOMP framing.
 */
export class DeliberationStream {
    /**
     * @param {string} sessionId - Current session ID to subscribe to
     * @param {function} onEvent - Callback called with each DeliberationStageMessage
     * @param {function} [onError] - Optional error callback
     */
    constructor(sessionId, onEvent, onError = null) {
        this.sessionId = sessionId;
        this.onEvent = onEvent;
        this.onError = onError;
        this._ws = null;
        this._connected = false;
        this._topic = `/topic/deliberation/${sessionId}`;
    }

    /**
     * Connect and subscribe to the deliberation topic.
     */
    connect() {
        if (this._ws) {
            this.disconnect();
        }

        const wsUrl = `${WS_BASE}/ws/websocket`;
        this._ws = new WebSocket(wsUrl);

        this._ws.onopen = () => {
            // Send STOMP CONNECT frame
            this._ws.send(STOMP_CONNECT_FRAME);
        };

        this._ws.onmessage = (event) => {
            this._handleFrame(event.data);
        };

        this._ws.onerror = (err) => {
            console.warn('[DeliberationStream] WebSocket error:', err);
            if (this.onError) this.onError(err);
        };

        this._ws.onclose = () => {
            this._connected = false;
        };
    }

    /**
     * Disconnect and clean up.
     */
    disconnect() {
        if (this._ws) {
            try {
                if (this._connected) {
                    this._ws.send('DISCONNECT\n\n\0');
                }
                this._ws.close();
            } catch (_) {
                // Ignore errors on close
            }
            this._ws = null;
            this._connected = false;
        }
    }

    /** Returns true if the stream is active. */
    get isConnected() {
        return this._connected;
    }

    // ─── Private ────────────────────────────────────────────────────────────

    _handleFrame(rawData) {
        const command = rawData.split('\n')[0];

        if (command === 'CONNECTED') {
            this._connected = true;
            // Subscribe to session deliberation topic
            const subFrame = `SUBSCRIBE\nid:sub-deliberation\ndestination:${this._topic}\n\n\0`;
            this._ws.send(subFrame);
            return;
        }

        if (command === 'MESSAGE') {
            try {
                // Extract JSON body after double-newline, strip null byte at end
                const bodyStart = rawData.indexOf('\n\n') + 2;
                const body = rawData.substring(bodyStart).replace(/\0$/, '');
                const payload = JSON.parse(body);
                this.onEvent(payload);
            } catch (e) {
                console.warn('[DeliberationStream] Could not parse MESSAGE frame:', e);
            }
        }
    }
}

/**
 * Convenience hook: creates and manages a DeliberationStream for a React
 * component lifecycle (or used directly in vanilla JS).
 *
 * @param {string} sessionId
 * @param {function} onStageEvent - callback per stage update
 * @returns {{ start: function, stop: function }}
 */
export function createDeliberationStream(sessionId, onStageEvent) {
    let stream = null;

    return {
        start() {
            stream = new DeliberationStream(sessionId, onStageEvent, (err) => {
                console.warn('[PILOTO WS] Connection error, streaming unavailable:', err);
            });
            stream.connect();
        },
        stop() {
            if (stream) {
                stream.disconnect();
                stream = null;
            }
        }
    };
}
