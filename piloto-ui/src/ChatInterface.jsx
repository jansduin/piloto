import React, { useState, useEffect, useRef } from 'react'
import { Send, ChevronDown, ChevronRight, CheckCircle, XCircle, AlertTriangle } from 'lucide-react'
import { sendChatMessage } from './api/client.js'
import { DEFAULT_TENANT_ID } from './api/config.js'

const SESSION_ID = 'session_' + Math.random().toString(36).substring(7);

export default function ChatInterface() {
    const [messages, setMessages] = useState([
        { role: 'system', content: 'Bienvenido al Centro de Control PILOTO. El núcleo cognitivo está listo para el despliegue.' }
    ]);
    const [inputValue, setInputValue] = useState('');
    const [isProcessing, setIsProcessing] = useState(false);
    const [role, setRole] = useState('chat_agent');
    const [metadata, setMetadata] = useState(null);
    const [trace, setTrace] = useState(null);
    const msgRef = useRef(null);

    useEffect(() => {
        if (msgRef.current) msgRef.current.scrollTop = msgRef.current.scrollHeight;
    }, [messages, isProcessing]);

    const handleSend = async () => {
        const text = inputValue.trim();
        if (!text) return;

        setMessages(prev => [...prev, { role: 'user', content: text }]);
        setInputValue('');
        setIsProcessing(true);
        setTrace(null);

        try {
            const data = await sendChatMessage(text, SESSION_ID, { role });
            setIsProcessing(false);

            if (data.success) {
                setMessages(prev => [...prev, { role: 'ai', content: data.response }]);
                setMetadata(data.metadata);
                setTrace(data.metadata?.deliberationTrace || null);
            } else {
                setMessages(prev => [...prev, { role: 'system', content: 'ERROR: ' + (data.error || 'Unknown') }]);
                if (data.metadata?.deliberationTrace) setTrace(data.metadata.deliberationTrace);
            }
        } catch (error) {
            setIsProcessing(false);
            setMessages(prev => [...prev, { role: 'system', content: 'ERROR DE CONEXIÓN: ' + error.message }]);
        }
    };

    const handleKey = (e) => {
        if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); handleSend(); }
    };

    return (
        <div className="chat-layout">
            {/* Sidebar */}
            <aside className="sidebar glass-panel">
                <section className="sidebar-section">
                    <h3>SESSION</h3>
                    <div className="meta-value" style={{ fontSize: '0.7rem', wordBreak: 'break-all' }}>{SESSION_ID}</div>
                </section>
            </aside>

            {/* Chat */}
            <section className="chat-area glass-panel">
                <div className="chat-content" ref={msgRef}>
                    {messages.map((msg, i) => (
                        <div key={i} className={`message ${msg.role}`}><p>{msg.content}</p></div>
                    ))}
                    {isProcessing && (
                        <div className="message system"><p>⚙️ Procesando deliberación...</p></div>
                    )}
                </div>
                <footer className="chat-input-area">
                    <div className="glass-input-wrapper">
                        <input
                            type="text" value={inputValue}
                            onChange={e => setInputValue(e.target.value)}
                            onKeyDown={handleKey}
                            placeholder="Escribe un comando o consulta al núcleo..."
                            autoComplete="off"
                        />
                        <button onClick={handleSend} className="send-button"><Send size={16} /></button>
                    </div>
                </footer>
            </section>

            {/* Metadata + Trace */}
            <aside className="metadata-panel glass-panel">
                <section className="metadata-section">
                    <h3>LLM EXECUTION</h3>
                    <MetaRow label="Provider" value={metadata?.llmProvider} />
                    <MetaRow label="Model" value={metadata?.llmModel} />
                    <MetaRow label="Latency" value={metadata?.executionTimeMs ? `${metadata.executionTimeMs}ms` : null} />
                    <MetaRow label="Mode" value={metadata?.mode} />
                </section>
                {trace && <DeliberationTracePanel trace={trace} />}
            </aside>
        </div>
    );
}

// ─── Helpers ──────────────────────────────────────────────────────────────────
function MetaRow({ label, value }) {
    return (
        <div className="metadata-item">
            <span className="meta-label">{label}:</span>
            <span className="meta-value">{value || '--'}</span>
        </div>
    );
}

function DeliberationTracePanel({ trace }) {
    const [expanded, setExpanded] = useState({ proposer: false, critic: false, verifier: false });
    const toggle = (k) => setExpanded(p => ({ ...p, [k]: !p[k] }));

    const decisionColor = { APPROVED: '#4ade80', REJECTED: '#f87171', REVISION_REQUIRED: '#facc15' }[trace.arbiterDecision] || '#9ea4b0';
    const Icon = trace.arbiterDecision === 'APPROVED' ? CheckCircle : trace.arbiterDecision === 'REJECTED' ? XCircle : AlertTriangle;

    return (
        <section className="metadata-section">
            <h3>DELIBERATION TRACE</h3>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.75rem', padding: '0.4rem 0.6rem', background: `${decisionColor}15`, borderRadius: '8px', border: `1px solid ${decisionColor}33` }}>
                <Icon size={14} color={decisionColor} />
                <span style={{ color: decisionColor, fontWeight: 700, fontSize: '0.75rem' }}>{trace.arbiterDecision}</span>
                <span style={{ marginLeft: 'auto', fontSize: '0.7rem', color: '#9ea4b0' }}>Score: {trace.reasoningScore?.toFixed(2)}</span>
            </div>

            <TraceSection title="Proposer" color="#60a5fa" expanded={expanded.proposer} onToggle={() => toggle('proposer')}>
                {trace.proposerOutput && (<>
                    <TF label="Plan" value={trace.proposerOutput.plan} />
                    <TF label="Risk" value={trace.proposerOutput.riskLevel} />
                    {trace.proposerOutput.assumptions?.length > 0 && <TF label="Assumptions" value={trace.proposerOutput.assumptions.join(', ')} />}
                </>)}
            </TraceSection>

            <TraceSection title="Critic" color="#f472b6" expanded={expanded.critic} onToggle={() => toggle('critic')}>
                {trace.criticOutput && (<>
                    <TF label="Confidence" value={`${(trace.criticOutput.confidenceInPlan * 100).toFixed(0)}%`} />
                    <TF label="Risk Escalation" value={trace.criticOutput.riskEscalation ? 'YES' : 'NO'} />
                    {trace.criticOutput.logicalIssues?.length > 0 && <TF label="Issues" value={trace.criticOutput.logicalIssues.join(', ')} />}
                </>)}
            </TraceSection>

            <TraceSection title="Verifier" color="#34d399" expanded={expanded.verifier} onToggle={() => toggle('verifier')}>
                {trace.verifierOutput && (<>
                    <TF label="Grounding" value={`${(trace.verifierOutput.groundingScore * 100).toFixed(0)}%`} />
                    <TF label="Factual Risk" value={trace.verifierOutput.factualRisk} />
                    <TF label="Contradictions" value={trace.verifierOutput.contradictionsFound ? 'YES' : 'NO'} />
                </>)}
            </TraceSection>
        </section>
    );
}

function TraceSection({ title, color, expanded, onToggle, children }) {
    return (
        <div style={{ marginBottom: '0.4rem', border: `1px solid ${color}33`, borderRadius: '8px', overflow: 'hidden' }}>
            <button onClick={onToggle} style={{ width: '100%', display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0.4rem 0.6rem', background: `${color}15`, border: 'none', cursor: 'pointer', color, fontSize: '0.7rem', fontWeight: 600 }}>
                {title} {expanded ? <ChevronDown size={11} /> : <ChevronRight size={11} />}
            </button>
            {expanded && <div style={{ padding: '0.6rem', background: 'rgba(0,0,0,0.2)' }}>{children}</div>}
        </div>
    );
}

function TF({ label, value }) {
    return (
        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.3rem', fontSize: '0.68rem' }}>
            <span style={{ color: '#9ea4b0' }}>{label}:</span>
            <span style={{ color: '#e1e3e6', textAlign: 'right', maxWidth: '60%', wordBreak: 'break-word' }}>{value ?? '--'}</span>
        </div>
    );
}
