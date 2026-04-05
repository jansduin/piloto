import React, { useState } from 'react';
import { Calendar, CheckCircle, AlertTriangle } from 'lucide-react';
import { executeDailyClosing } from '../api/client.js';

export default function DailyClosing() {
    const [status, setStatus] = useState('idle');
    const [result, setResult] = useState(null);

    const runClosing = async () => {
        setStatus('running');
        try {
            const data = await executeDailyClosing('tenant_001');
            setResult(data);
            setStatus('success');
        } catch (e) {
            setStatus('error');
            console.error(e);
        }
    };

    return (
        <div style={{ padding: '2rem', maxWidth: '600px', margin: '0 auto' }}>
            <h2 style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1.5rem' }}>
                <Calendar size={24} color="#00a3ff" /> Daily Closing
            </h2>

            <div className="glass-panel" style={{ padding: '2rem', textAlign: 'center' }}>
                {status === 'idle' && (
                    <>
                        <p style={{ color: '#9ea4b0', marginBottom: '1.5rem' }}>
                            Perform the daily closing to snapshot current system health and metrics for historical auditing.
                        </p>
                        <button className="btn-primary" onClick={runClosing} style={{ padding: '0.8rem 2rem', background: '#00a3ff', color: '#fff', border: 'none', borderRadius: '8px', cursor: 'pointer', fontWeight: 600 }}>
                            EXECUTE CLOSING
                        </button>
                    </>
                )}

                {status === 'running' && <div style={{ color: '#00a3ff' }}>Capturing system snapshot...</div>}

                {status === 'success' && (
                    <div style={{ animation: 'fadeIn 0.5s' }}>
                        <CheckCircle size={48} color="#4ade80" style={{ marginBottom: '1rem' }} />
                        <h3 style={{ color: '#4ade80' }}>Closing Captured Successfully</h3>
                        <div style={{ marginTop: '1.5rem', textAlign: 'left', background: 'rgba(0,0,0,0.2)', padding: '1rem', borderRadius: '8px', fontSize: '0.85rem' }}>
                            <p><strong>Date:</strong> {result.date}</p>
                            <p><strong>Health Score:</strong> {(result.health_at_closing * 100).toFixed(1)}%</p>
                            <p><strong>Status:</strong> DETERMINISTIC_SNAPSHOT_STORED</p>
                        </div>
                        <button className="btn-secondary" onClick={() => setStatus('idle')} style={{ marginTop: '1rem', padding: '0.5rem 1rem', background: 'transparent', color: '#9ea4b0', border: '1px solid currentColor', borderRadius: '6px', cursor: 'pointer' }}>
                            RESET
                        </button>
                    </div>
                )}

                {status === 'error' && (
                    <div>
                        <AlertTriangle size={48} color="#f87171" style={{ marginBottom: '1rem' }} />
                        <p style={{ color: '#f87171' }}>Failed to communicate with Gateway.</p>
                        <button onClick={() => setStatus('idle')} style={{ color: '#00a3ff', background: 'none', border: 'none', cursor: 'pointer', textDecoration: 'underline' }}>Try again</button>
                    </div>
                )}
            </div>
        </div>
    );
}
