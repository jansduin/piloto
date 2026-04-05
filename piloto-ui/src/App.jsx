import React, { useState } from 'react'
import ChatInterface from './ChatInterface.jsx'
import ControlPanel from './control-panel/Dashboard.jsx'

function App() {
    const [mode, setMode] = useState('chat'); // 'chat' or 'admin'

    return (
        <div className="app-container">
            <header className="glass-header">
                <div className="logo">
                    <span className="logo-accent">PILOTO</span>
                    <span className="logo-subtitle">CDI KERNEL</span>
                </div>
                <div className="header-controls">
                    <button
                        className={`mode-toggle ${mode === 'admin' ? 'active' : ''}`}
                        onClick={() => setMode(mode === 'chat' ? 'admin' : 'chat')}
                    >
                        {mode === 'chat' ? '⚙️ CONTROL PANEL' : '💬 CHAT MODE'}
                    </button>
                    <div className="header-status">
                        <span className="status-dot"></span>
                        <span className="status-text">KERNEL ONLINE</span>
                    </div>
                </div>
            </header>

            <main className="main-content">
                {mode === 'chat' ? <ChatInterface /> : <ControlPanel />}
            </main>
        </div>
    )
}

export default App
