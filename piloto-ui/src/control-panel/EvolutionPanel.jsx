import React, { useState, useEffect } from 'react'
import { Dna, RefreshCw, Plus, TrendingUp, Award, Target, Activity } from 'lucide-react'
import { fetchEvolutionGrid, registerEvolutionVariant, triggerEvolutionOptimization } from '../api/client.js'
import { DEFAULT_TENANT_ID } from '../api/config.js'

export default function EvolutionPanel() {
    const [grid, setGrid] = useState(null);
    const [loading, setLoading] = useState(true);
    const [feedback, setFeedback] = useState(null);
    const [showRegister, setShowRegister] = useState(false);
    const [regForm, setRegForm] = useState({ intentType: '', sessionStage: '', promptContent: '', layer: 'RUNTIME' });
    const [submitting, setSubmitting] = useState(false);

    const loadGrid = async () => {
        try {
            const data = await fetchEvolutionGrid();
            setGrid(data);
        } catch (e) {
            console.error('Evolution grid fetch failed', e);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadGrid();
        const timer = setInterval(loadGrid, 8000);
        return () => clearInterval(timer);
    }, []);

    const handleRegister = async () => {
        if (!regForm.intentType || !regForm.sessionStage || !regForm.promptContent) {
            setFeedback({ type: 'error', msg: 'All fields are required.' });
            return;
        }
        setSubmitting(true);
        try {
            const result = await registerEvolutionVariant({
                tenantId: DEFAULT_TENANT_ID,
                intentType: regForm.intentType,
                sessionStage: regForm.sessionStage,
                promptContent: regForm.promptContent,
                layer: regForm.layer,
            });
            setFeedback({ type: 'success', msg: `Variant registered: ${result.variantId?.substring(0, 8)}... in cell ${result.cell}` });
            setRegForm({ intentType: '', sessionStage: '', promptContent: '', layer: 'RUNTIME' });
            setShowRegister(false);
            loadGrid();
        } catch (e) {
            setFeedback({ type: 'error', msg: e.message });
        } finally {
            setSubmitting(false);
        }
    };

    const handleOptimize = async () => {
        try {
            await triggerEvolutionOptimization(DEFAULT_TENANT_ID);
            setFeedback({ type: 'success', msg: 'Optimization cycle triggered.' });
        } catch (e) {
            setFeedback({ type: 'error', msg: e.message });
        }
    };

    if (loading) return <div style={{ padding: '1.25rem', color: '#9ea4b0' }}>Loading evolution grid...</div>;

    return (
        <div style={{ padding: '1.25rem', height: '100%', overflowY: 'auto' }}>
            {/* Header */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                <h2 style={{ fontSize: '1rem', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <Dna size={18} color="#a78bfa" /> Evolution Grid
                </h2>
                <div style={{ display: 'flex', gap: '0.4rem', alignItems: 'center' }}>
                    {feedback && (
                        <span style={{ fontSize: '0.7rem', color: feedback.type === 'success' ? '#4ade80' : '#f87171', marginRight: '0.5rem' }}>
                            {feedback.msg}
                        </span>
                    )}
                    <ActionBtn color="#a78bfa" onClick={() => setShowRegister(!showRegister)} icon={<Plus size={12} />} label="Register Variant" />
                    <ActionBtn color="#f59e0b" onClick={handleOptimize} icon={<TrendingUp size={12} />} label="Optimize" />
                    <ActionBtn color="#9ea4b0" onClick={loadGrid} icon={<RefreshCw size={12} />} label="Refresh" />
                </div>
            </div>

            {/* Summary Metrics */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '0.75rem', marginBottom: '1.25rem' }}>
                <SummaryCard icon={<Activity size={16} />} label="Total Variants" value={grid?.totalVariants ?? 0} color="#60a5fa" />
                <SummaryCard icon={<Target size={16} />} label="Active Cells" value={grid?.totalCells ?? 0} color="#a78bfa" />
                <SummaryCard icon={<Award size={16} />} label="Champions" value={grid?.totalChampions ?? 0} color="#4ade80" />
            </div>

            {/* Register Form */}
            {showRegister && (
                <div style={{ background: 'rgba(167,139,250,0.05)', border: '1px dashed #a78bfa', borderRadius: '10px', padding: '1rem', marginBottom: '1.25rem' }}>
                    <h3 style={{ fontSize: '0.85rem', marginBottom: '0.75rem', color: '#a78bfa' }}>+ Register New Variant</h3>
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '0.5rem', marginBottom: '0.5rem' }}>
                        <input placeholder="Intent Type (e.g. price_inquiry)" value={regForm.intentType}
                            onChange={e => setRegForm(p => ({ ...p, intentType: e.target.value }))} style={inputStyle} />
                        <input placeholder="Session Stage (e.g. first_contact)" value={regForm.sessionStage}
                            onChange={e => setRegForm(p => ({ ...p, sessionStage: e.target.value }))} style={inputStyle} />
                        <select value={regForm.layer} onChange={e => setRegForm(p => ({ ...p, layer: e.target.value }))} style={inputStyle}>
                            <option value="RUNTIME">RUNTIME</option>
                            <option value="TENANT">TENANT</option>
                            <option value="DOMAIN">DOMAIN</option>
                            <option value="ROLE">ROLE</option>
                        </select>
                    </div>
                    <textarea placeholder="Prompt content for this variant..." rows={3} value={regForm.promptContent}
                        onChange={e => setRegForm(p => ({ ...p, promptContent: e.target.value }))}
                        style={{ ...inputStyle, width: '100%', marginBottom: '0.5rem', resize: 'vertical' }} />
                    <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.4rem' }}>
                        <ActionBtn color="#9ea4b0" onClick={() => setShowRegister(false)} label="Cancel" />
                        <ActionBtn color="#4ade80" onClick={handleRegister} label={submitting ? '...' : 'Register'} disabled={submitting} />
                    </div>
                </div>
            )}

            {/* Grid Cells */}
            {(!grid?.cells || grid.cells.length === 0) ? (
                <EmptyGrid />
            ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                    {grid.cells.map((cell, i) => (
                        <CellCard key={i} cell={cell} />
                    ))}
                </div>
            )}
        </div>
    );
}

// ─── Cell Card ───────────────────────────────────────────────────────────────
function CellCard({ cell }) {
    const dims = cell.cellKey.split('::').map(d => {
        const [k, v] = d.split('=');
        return { key: k, value: v };
    });

    const hasChampion = !!cell.championId;
    const borderColor = hasChampion ? '#4ade80' : '#a78bfa';

    return (
        <div style={{
            background: 'rgba(255,255,255,0.03)', border: `1px solid ${borderColor}33`,
            borderRadius: '10px', padding: '0.85rem', borderLeft: `3px solid ${borderColor}`
        }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.5rem' }}>
                <div style={{ display: 'flex', gap: '0.4rem', flexWrap: 'wrap' }}>
                    {dims.map((d, i) => (
                        <Badge key={i} label={`${d.key}: ${d.value}`} color="#a78bfa" />
                    ))}
                    <Badge label={`${cell.variantCount} variants`} color="#60a5fa" />
                </div>
                {hasChampion && <Badge label="CHAMPION" color="#4ade80" />}
            </div>

            {hasChampion && (
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '0.5rem', marginTop: '0.5rem' }}>
                    <MiniStat label="Champion ID" value={cell.championId?.substring(0, 8) + '...'} />
                    <MiniStat label="Wilson Score" value={cell.championFitness?.toFixed(4)} color="#4ade80" />
                    <MiniStat label="Samples" value={cell.championSamples} />
                    <MiniStat label="Raw Rate" value={cell.championRawRate} color="#f59e0b" />
                </div>
            )}

            {!hasChampion && (
                <div style={{ fontSize: '0.72rem', color: '#9ea4b0', fontStyle: 'italic', marginTop: '0.3rem' }}>
                    No champion promoted yet — report outcomes to promote via Wilson Score.
                </div>
            )}
        </div>
    );
}

// ─── Primitives ──────────────────────────────────────────────────────────────
function SummaryCard({ icon, label, value, color }) {
    return (
        <div style={{
            background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.08)',
            borderRadius: '10px', padding: '0.85rem', textAlign: 'center'
        }}>
            <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '0.4rem', color }}>{icon}</div>
            <div style={{ fontSize: '1.5rem', fontWeight: 700, color, fontFamily: "'JetBrains Mono', monospace" }}>{value}</div>
            <div style={{ fontSize: '0.6rem', color: '#9ea4b0', letterSpacing: '1px', marginTop: '0.2rem' }}>{label.toUpperCase()}</div>
        </div>
    );
}

function MiniStat({ label, value, color = '#e1e3e6' }) {
    return (
        <div style={{ background: 'rgba(0,0,0,0.2)', borderRadius: '6px', padding: '0.4rem', textAlign: 'center' }}>
            <div style={{ fontSize: '0.6rem', color: '#9ea4b0', marginBottom: '0.15rem' }}>{label}</div>
            <div style={{ fontSize: '0.78rem', fontWeight: 600, color, fontFamily: "'JetBrains Mono', monospace" }}>{value ?? '--'}</div>
        </div>
    );
}

function Badge({ label, color }) {
    return (
        <span style={{
            fontSize: '0.65rem', padding: '0.15rem 0.4rem', borderRadius: '4px',
            background: `${color}22`, color, border: `1px solid ${color}44`,
            fontWeight: 600, letterSpacing: '0.3px'
        }}>{label}</span>
    );
}

function ActionBtn({ color, onClick, icon, label, disabled }) {
    return (
        <button onClick={onClick} disabled={disabled} style={{
            display: 'flex', alignItems: 'center', gap: '0.25rem',
            padding: '0.3rem 0.6rem', borderRadius: '6px',
            border: `1px solid ${color}`, background: `${color}22`, color,
            cursor: disabled ? 'not-allowed' : 'pointer',
            fontSize: '0.7rem', fontWeight: 600
        }}>{icon}{label}</button>
    );
}

function EmptyGrid() {
    return (
        <div style={{
            textAlign: 'center', padding: '3rem', color: '#9ea4b0',
            border: '1px dashed rgba(255,255,255,0.1)', borderRadius: '12px'
        }}>
            <Dna size={36} strokeWidth={1} style={{ marginBottom: '0.75rem', opacity: 0.5 }} />
            <p style={{ fontSize: '0.85rem', marginBottom: '0.3rem' }}>Evolution grid is empty.</p>
            <p style={{ fontSize: '0.72rem' }}>Register prompt variants to begin evolutionary optimization.</p>
        </div>
    );
}

const inputStyle = {
    background: 'rgba(0,0,0,0.3)', border: '1px solid rgba(255,255,255,0.1)',
    borderRadius: '6px', color: '#e1e3e6', padding: '0.4rem 0.6rem',
    fontSize: '0.8rem', outline: 'none'
};
