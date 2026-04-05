import React, { useState, useEffect } from 'react'
import ExecutionPanel from './ExecutionPanel.jsx'
import DailyClosing from './DailyClosing.jsx'
import { Zap, FileText, Brain, Shield, CheckCircle, XCircle, AlertTriangle, Edit3, Save, X, Calendar } from 'lucide-react'
import { fetchPrompts, updatePrompt, registerPrompt, fetchMemoryItems, updateMemoryItem, fetchTelemetry, fetchHealth } from '../api/client.js'

export default function Dashboard() {
    const [activeTab, setActiveTab] = useState('execution');
    const [healthData, setHealthData] = useState({ score: '--', metrics: {} });

    useEffect(() => {
        // Fetch real metrics from Diagnostic Phase 7 endpoint
        const getHealth = async () => {
            try {
                const data = await fetchHealth();
                setHealthData({
                    score: (data.health_score * 100).toFixed(0) + '%',
                    metrics: data.metrics || {}
                });
            } catch (e) { console.error("Health fetch failed", e); }
        };
        getHealth();
        const timer = setInterval(getHealth, 10000); // 10s refresh
        return () => clearInterval(timer);
    }, []);

    const tabs = [
        { id: 'execution', label: 'Execution Engine', icon: Zap },
        { id: 'closing', label: 'Daily Closing', icon: Calendar },
        { id: 'prompts', label: 'Prompt Governance', icon: FileText },
        { id: 'memory', label: 'Domain Memory', icon: Brain },
        { id: 'audit', label: 'Audit Log', icon: Shield },
    ];

    return (
        <div className="dashboard-container">
            {/* Metrics — Real CDI Phase 7 Data */}
            <div className="dashboard-metrics">
                <MetricCard label="Health Score" value={healthData.score} color={parseFloat(healthData.score) < 60 ? '#f87171' : '#4ade80'} />
                <MetricCard label="Failure Rate" value={healthData.metrics.failure_rate?.toFixed(2) || '0.00'} />
                <MetricCard label="Latency Score" value={healthData.metrics.avg_execution_time?.toFixed(2) || '1.00'} />
                <MetricCard label="Memory Health" value={healthData.metrics.memory_query_success_rate?.toFixed(2) || '1.00'} />
                <MetricCard label="System State" value={parseFloat(healthData.score) > 80 ? 'EXCELLENT' : 'GOOD'} />
            </div>

            <div className="dashboard-body">
                <aside className="glass-panel dashboard-nav">
                    <p style={{ fontSize: '0.65rem', letterSpacing: '2px', color: '#9ea4b0', marginBottom: '0.75rem' }}>NAVIGATION</p>
                    <ul style={{ listStyle: 'none', padding: 0, display: 'flex', flexDirection: 'column', gap: '0.2rem' }}>
                        {tabs.map(t => (
                            <NavItem key={t.id} label={t.label} Icon={t.icon} active={activeTab === t.id} onClick={() => setActiveTab(t.id)} />
                        ))}
                    </ul>
                </aside>

                <section className="glass-panel dashboard-content">
                    {activeTab === 'execution' && <ExecutionPanel />}
                    {activeTab === 'closing' && <DailyClosing />}
                    {activeTab === 'prompts' && <PromptView />}
                    {activeTab === 'memory' && <MemoryView />}
                    {activeTab === 'audit' && <AuditLog />}
                </section>
            </div>
        </div>
    );
}

// ─── Metric Card ──────────────────────────────────────────────────────────────
function MetricCard({ label, value, color = '#e1e3e6' }) {
    return (
        <div className="glass-panel" style={{ padding: '0.75rem', textAlign: 'center' }}>
            <div style={{ fontSize: '0.6rem', color: '#9ea4b0', marginBottom: '0.4rem', letterSpacing: '1px' }}>{label.toUpperCase()}</div>
            <div style={{ fontSize: '1.4rem', fontWeight: 700, color, fontFamily: "'JetBrains Mono', monospace" }}>{value}</div>
        </div>
    );
}

// ─── Nav Item ─────────────────────────────────────────────────────────────────
function NavItem({ label, Icon, active, onClick }) {
    return (
        <li onClick={onClick} style={{
            display: 'flex', alignItems: 'center', gap: '0.6rem',
            padding: '0.6rem 0.75rem', borderRadius: '8px', cursor: 'pointer',
            background: active ? 'rgba(0,163,255,0.15)' : 'transparent',
            borderLeft: active ? '3px solid #00a3ff' : '3px solid transparent',
            color: active ? '#00a3ff' : '#9ea4b0',
            transition: 'all 0.2s', fontSize: '0.8rem', fontWeight: active ? 600 : 400
        }}>
            <Icon size={14} />{label}
        </li>
    );
}

// ─── Prompt View ──────────────────────────────────────────────────────────────
function PromptView() {
    const [prompts, setPrompts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [editingId, setEditingId] = useState(null);
    const [editContent, setEditContent] = useState('');
    const [saving, setSaving] = useState(false);
    const [feedback, setFeedback] = useState(null);

    useEffect(() => {
        fetchPrompts()
            .then(data => { setPrompts(data); setLoading(false); })
            .catch(() => setLoading(false));
    }, []);

    const startEdit = (p) => { setEditingId(p.id); setEditContent(p.content); setFeedback(null); };
    const cancelEdit = () => { setEditingId(null); setEditContent(''); };

    const saveEdit = async (id) => {
        setSaving(true);
        try {
            await updatePrompt(id, editContent);
            setPrompts(prev => prev.map(p => p.id === id ? { ...p, content: editContent } : p));
            setFeedback({ type: 'success', msg: 'Prompt updated.' });
            setEditingId(null);
        } catch (e) {
            setFeedback({ type: 'error', msg: e.message });
        } finally { setSaving(false); }
    };

    return (
        <div style={{ padding: '1.25rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                <h2 style={{ fontSize: '1rem', fontWeight: 600 }}>Prompt Governance</h2>
                {feedback && <FeedbackBadge feedback={feedback} />}
            </div>
            {loading ? <LoadingState /> : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                    {/* Add Prompt Form */}
                    <div style={{ background: 'rgba(0,163,255,0.05)', border: '1px dashed #00a3ff', borderRadius: '10px', padding: '1rem' }}>
                        <h3 style={{ fontSize: '0.85rem', marginBottom: '0.75rem', color: '#00a3ff' }}>+ Register New Prompt</h3>
                        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '0.5rem', marginBottom: '0.5rem' }}>
                            <input id="new-layer" placeholder="Layer (e.g. reasoning)" style={inputStyle} />
                            <input id="new-role" placeholder="Role (e.g. critic)" style={inputStyle} />
                            <input id="new-domain" placeholder="Domain (e.g. legal)" style={inputStyle} />
                        </div>
                        <textarea id="new-content" placeholder="Enter prompt instructions..." rows={3} style={{ ...inputStyle, width: '100%', marginBottom: '0.5rem' }} />
                        <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
                            <Btn color="#00a3ff" onClick={async () => {
                                const l = document.getElementById('new-layer').value;
                                const r = document.getElementById('new-role').value;
                                const d = document.getElementById('new-domain').value;
                                const c = document.getElementById('new-content').value;
                                if (!l || !c) return;
                                setSaving(true);
                                try {
                                    await registerPrompt({
                                        id: crypto.randomUUID(),
                                        name: `Prompt-${Date.now()}`,
                                        layer: l,
                                        role: r,
                                        domain: d,
                                        content: c,
                                        version: 1,
                                        tenantId: 'default_tenant'
                                    });
                                    setFeedback({ type: 'success', msg: 'Prompt registered successfully.' });
                                    fetchPrompts().then(setPrompts);
                                    document.getElementById('new-layer').value = '';
                                    document.getElementById('new-role').value = '';
                                    document.getElementById('new-domain').value = '';
                                    document.getElementById('new-content').value = '';
                                } catch (e) { setFeedback({ type: 'error', msg: e.message }); }
                                finally { setSaving(false); }
                            }} disabled={saving}>Register Prompt</Btn>
                        </div>
                    </div>

                    {prompts.length === 0 ? <EmptyState msg="No prompts registered." /> : prompts.map(p => (
                        <div key={p.id} style={{ background: 'rgba(255,255,255,0.04)', border: '1px solid rgba(255,255,255,0.08)', borderRadius: '10px', padding: '0.75rem' }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.5rem' }}>
                                <div style={{ display: 'flex', gap: '0.4rem', flexWrap: 'wrap' }}>
                                    <Badge label={p.layer} color="#00a3ff" />
                                    {p.role && <Badge label={p.role} color="#a78bfa" />}
                                    {p.domain && <Badge label={p.domain} color="#34d399" />}
                                    <Badge label={`v${p.version}`} color="#9ea4b0" />
                                </div>
                                {editingId !== p.id && <Btn color="#00a3ff" onClick={() => startEdit(p)}><Edit3 size={12} /> Edit</Btn>}
                            </div>
                            {editingId === p.id ? (
                                <div>
                                    <textarea value={editContent} onChange={e => setEditContent(e.target.value)} rows={5}
                                        style={{ width: '100%', background: 'rgba(0,0,0,0.4)', border: '1px solid #00a3ff', borderRadius: '8px', color: '#e1e3e6', padding: '0.6rem', fontSize: '0.8rem', fontFamily: "'JetBrains Mono', monospace", resize: 'vertical', outline: 'none' }}
                                    />
                                    <div style={{ display: 'flex', gap: '0.4rem', marginTop: '0.4rem', justifyContent: 'flex-end' }}>
                                        <Btn color="#9ea4b0" onClick={cancelEdit}><X size={12} /> Cancel</Btn>
                                        <Btn color="#4ade80" onClick={() => saveEdit(p.id)} disabled={saving}><Save size={12} /> {saving ? '...' : 'Save'}</Btn>
                                    </div>
                                </div>
                            ) : (
                                <p style={{ fontSize: '0.78rem', color: '#9ea4b0', fontFamily: "'JetBrains Mono', monospace", whiteSpace: 'pre-wrap', lineHeight: 1.5 }}>
                                    {p.content?.length > 200 ? p.content.substring(0, 200) + '…' : p.content}
                                </p>
                            )}
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}

// ─── Memory View ──────────────────────────────────────────────────────────────
function MemoryView() {
    const [items, setItems] = useState([]);
    const [loading, setLoading] = useState(true);
    const [editingId, setEditingId] = useState(null);
    const [editContent, setEditContent] = useState('');
    const [saving, setSaving] = useState(false);
    const [feedback, setFeedback] = useState(null);

    useEffect(() => {
        fetchMemoryItems()
            .then(data => { setItems(data); setLoading(false); })
            .catch(() => setLoading(false));
    }, []);

    const startEdit = (item) => { setEditingId(item.id); setEditContent(item.content); setFeedback(null); };
    const cancelEdit = () => { setEditingId(null); setEditContent(''); };

    const saveEdit = async (id) => {
        setSaving(true);
        try {
            await updateMemoryItem(id, editContent);
            setItems(prev => prev.map(i => i.id === id ? { ...i, content: editContent } : i));
            setFeedback({ type: 'success', msg: 'Memory updated.' });
            setEditingId(null);
        } catch (e) {
            setFeedback({ type: 'error', msg: e.message });
        } finally { setSaving(false); }
    };

    return (
        <div style={{ padding: '1.25rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                <h2 style={{ fontSize: '1rem', fontWeight: 600 }}>Domain Memory</h2>
                {feedback && <FeedbackBadge feedback={feedback} />}
            </div>
            {loading ? <LoadingState /> : items.length === 0 ? <EmptyState msg="No memory items registered." /> : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                    {items.map(item => (
                        <div key={item.id} style={{ background: 'rgba(255,255,255,0.04)', border: '1px solid rgba(255,255,255,0.08)', borderRadius: '10px', padding: '0.75rem' }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.5rem' }}>
                                <div>
                                    <div style={{ fontWeight: 600, fontSize: '0.85rem', marginBottom: '0.2rem' }}>{item.title}</div>
                                    <div style={{ display: 'flex', gap: '0.4rem' }}>
                                        {item.category && <Badge label={item.category} color="#34d399" />}
                                        {item.domain && <Badge label={item.domain} color="#00a3ff" />}
                                        {item.priority && <Badge label={`P${item.priority}`} color="#facc15" />}
                                    </div>
                                </div>
                                {editingId !== item.id && <Btn color="#00a3ff" onClick={() => startEdit(item)}><Edit3 size={12} /> Edit</Btn>}
                            </div>
                            {editingId === item.id ? (
                                <div>
                                    <textarea value={editContent} onChange={e => setEditContent(e.target.value)} rows={3}
                                        style={{ width: '100%', background: 'rgba(0,0,0,0.4)', border: '1px solid #00a3ff', borderRadius: '8px', color: '#e1e3e6', padding: '0.6rem', fontSize: '0.8rem', resize: 'vertical', outline: 'none' }}
                                    />
                                    <div style={{ display: 'flex', gap: '0.4rem', marginTop: '0.4rem', justifyContent: 'flex-end' }}>
                                        <Btn color="#9ea4b0" onClick={cancelEdit}><X size={12} /> Cancel</Btn>
                                        <Btn color="#4ade80" onClick={() => saveEdit(item.id)} disabled={saving}><Save size={12} /> {saving ? '...' : 'Save'}</Btn>
                                    </div>
                                </div>
                            ) : (
                                <p style={{ fontSize: '0.8rem', color: '#9ea4b0', lineHeight: 1.5 }}>{item.content}</p>
                            )}
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}

// ─── Audit Log ────────────────────────────────────────────────────────────────
function AuditLog() {
    const [events, setEvents] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchEvents = async () => {
            try {
                const data = await fetchTelemetry();
                setEvents(Array.isArray(data) ? [...data].reverse() : []);
                setLoading(false);
            } catch (e) { console.error("Audit fetch failed", e); setLoading(false); }
        };
        fetchEvents();
        const timer = setInterval(fetchEvents, 5000);
        return () => clearInterval(timer);
    }, []);

    return (
        <div style={{ padding: '1.25rem', height: '100%', display: 'flex', flexDirection: 'column' }}>
            <h2 style={{ fontSize: '1rem', fontWeight: 600, marginBottom: '1rem' }}>System Audit Log</h2>
            {loading ? <LoadingState /> : events.length === 0 ? <EmptyState msg="No events recorded yet." /> : (
                <div style={{ flex: 1, overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: '0.4rem' }}>
                    {events.map((e, i) => (
                        <div key={i} style={{ background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.05)', borderRadius: '6px', padding: '0.5rem 0.75rem', fontSize: '0.75rem' }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.2rem' }}>
                                <span style={{ color: '#00a3ff', fontWeight: 600 }}>{e.eventType}</span>
                                <span style={{ color: '#9ea4b0', fontSize: '0.65rem' }}>{new Date(e.timestamp).toLocaleTimeString()}</span>
                            </div>
                            <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                                <Badge label={e.component} color="#9ea4b0" />
                                <span style={{ color: '#e1e3e6', opacity: 0.8 }}>{JSON.stringify(e.payload).substring(0, 80)}...</span>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}

const inputStyle = {
    background: 'rgba(0,0,0,0.3)', border: '1px solid rgba(255,255,255,0.1)',
    borderRadius: '6px', color: '#e1e3e6', padding: '0.4rem 0.6rem',
    fontSize: '0.8rem', outline: 'none'
};

// ─── Shared UI Primitives ─────────────────────────────────────────────────────
function Badge({ label, color }) {
    return <span style={{ fontSize: '0.65rem', padding: '0.15rem 0.4rem', borderRadius: '4px', background: `${color}22`, color, border: `1px solid ${color}44`, fontWeight: 600, letterSpacing: '0.3px' }}>{label}</span>;
}

function Btn({ color, onClick, disabled, children }) {
    return <button onClick={onClick} disabled={disabled} style={{ display: 'flex', alignItems: 'center', gap: '0.25rem', padding: '0.3rem 0.6rem', borderRadius: '6px', border: `1px solid ${color}`, background: `${color}22`, color, cursor: disabled ? 'not-allowed' : 'pointer', fontSize: '0.7rem', fontWeight: 600 }}>{children}</button>;
}

function FeedbackBadge({ feedback }) {
    return (
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.75rem', color: feedback.type === 'success' ? '#4ade80' : '#f87171' }}>
            {feedback.type === 'success' ? <CheckCircle size={13} /> : <XCircle size={13} />}{feedback.msg}
        </div>
    );
}

function LoadingState() { return <div style={{ color: '#9ea4b0', fontSize: '0.8rem', padding: '0.75rem' }}>Loading...</div>; }

function EmptyState({ msg }) {
    return <div style={{ color: '#9ea4b0', fontSize: '0.8rem', padding: '1.5rem', textAlign: 'center' }}><AlertTriangle size={28} strokeWidth={1} style={{ marginBottom: '0.4rem' }} /><p>{msg}</p></div>;
}
