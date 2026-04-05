import React, { useState } from 'react'
import { sendChatMessage } from '../api/client.js'
import { DOMAINS, EXECUTION_MODES } from '../api/config.js'

export default function ExecutionPanel() {
    const [goal, setGoal] = useState('');
    const [domain, setDomain] = useState('general');
    const [mode, setMode] = useState('NORMAL');
    const [result, setResult] = useState(null);
    const [loading, setLoading] = useState(false);

    const executeGoal = async () => {
        if (!goal.trim()) return;
        setLoading(true);
        setResult(null);

        try {
            const data = await sendChatMessage(goal, 'cp_session_' + Date.now(), {
                domain,
                executionMode: mode,
                tenantId: 'tenant_admin',
            });
            setResult(data);
        } catch (e) {
            setResult({ success: false, error: e.message });
        } finally {
            setLoading(false);
        }
    };

    const selectStyle = {
        width: '100%', padding: '0.5rem', marginTop: '0.2rem',
        background: 'rgba(0,0,0,0.3)', color: '#e1e3e6',
        border: '1px solid rgba(255,255,255,0.1)', borderRadius: '8px',
        outline: 'none', fontSize: '0.8rem',
    };

    return (
        <div style={{ padding: '1.25rem', height: '100%', overflowY: 'auto' }}>
            <h2 style={{ fontSize: '1rem', fontWeight: 600, marginBottom: '1rem' }}>Execution Engine</h2>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem', background: 'rgba(0,0,0,0.2)', padding: '1rem', borderRadius: '10px', border: '1px solid rgba(255,255,255,0.06)' }}>
                <div>
                    <label style={{ display: 'block', marginBottom: '0.4rem', fontSize: '0.8rem', color: '#9ea4b0' }}>Goal / Prompt</label>
                    <textarea
                        value={goal} onChange={e => setGoal(e.target.value)}
                        placeholder="Describe the objective for the agent..."
                        rows={4}
                        style={{ width: '100%', background: 'rgba(0,0,0,0.3)', border: '1px solid rgba(255,255,255,0.1)', color: '#e1e3e6', padding: '0.75rem', borderRadius: '8px', resize: 'vertical', outline: 'none', fontSize: '0.85rem' }}
                    />
                </div>

                <div style={{ display: 'flex', gap: '0.75rem' }}>
                    <div style={{ flex: 1 }}>
                        <label style={{ fontSize: '0.8rem', color: '#9ea4b0' }}>Domain</label>
                        <input
                            type="text"
                            value={domain}
                            onChange={e => setDomain(e.target.value)}
                            placeholder="e.g. Legal, Tech, Medical..."
                            style={selectStyle}
                        />
                    </div>
                    <div style={{ flex: 1 }}>
                        <label style={{ fontSize: '0.8rem', color: '#9ea4b0' }}>Execution Mode</label>
                        <select value={mode} onChange={e => setMode(e.target.value)} style={selectStyle}>
                            {EXECUTION_MODES.map(m => <option key={m.value} value={m.value}>{m.label}</option>)}
                        </select>
                    </div>
                </div>

                <button onClick={executeGoal} disabled={loading} style={{
                    padding: '0.75rem', borderRadius: '8px', border: 'none', cursor: loading ? 'not-allowed' : 'pointer',
                    fontWeight: 700, fontSize: '0.85rem', letterSpacing: '0.5px',
                    background: loading ? '#555' : '#00a3ff', color: '#fff',
                    transition: 'all 0.2s',
                }}>
                    {loading ? 'EXECUTING...' : 'RUN EXECUTION'}
                </button>
            </div>

            {result && (
                <div style={{ marginTop: '1.25rem', borderTop: '1px solid rgba(255,255,255,0.08)', paddingTop: '1rem' }}>
                    <h3 style={{ fontSize: '0.9rem', marginBottom: '0.75rem' }}>
                        Result: {result.success
                            ? <span style={{ color: '#4ade80' }}>SUCCESS</span>
                            : <span style={{ color: '#f87171' }}>FAILED</span>}
                    </h3>

                    {result.error && (
                        <div style={{ padding: '0.75rem', background: 'rgba(248,113,113,0.1)', border: '1px solid rgba(248,113,113,0.3)', borderRadius: '8px', fontSize: '0.8rem' }}>
                            <strong>Error:</strong> {result.error}
                        </div>
                    )}

                    {result.response && (
                        <div style={{ marginTop: '0.75rem' }}>
                            <h4 style={{ fontSize: '0.8rem', color: '#9ea4b0', marginBottom: '0.4rem' }}>Response:</h4>
                            <p style={{ background: 'rgba(0,0,0,0.3)', padding: '0.75rem', borderRadius: '8px', whiteSpace: 'pre-wrap', fontSize: '0.8rem', lineHeight: 1.5 }}>{result.response}</p>
                        </div>
                    )}

                    {result.metadata && (
                        <div style={{ marginTop: '0.75rem' }}>
                            <h4 style={{ fontSize: '0.8rem', color: '#9ea4b0', marginBottom: '0.4rem' }}>Metadata / Trace:</h4>
                            <pre style={{ background: 'rgba(0,0,0,0.4)', padding: '0.75rem', borderRadius: '8px', overflowX: 'auto', fontSize: '0.7rem', lineHeight: 1.4, color: '#9ea4b0' }}>
                                {JSON.stringify(result.metadata, null, 2)}
                            </pre>
                        </div>
                    )}
                </div>
            )}
        </div>
    );
}
