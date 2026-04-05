package com.piloto.cdi.gateway.config;

import com.piloto.cdi.kernel.controller.StateTransitionController;
import com.piloto.cdi.kernel.evaluation.EvaluationService;
import com.piloto.cdi.kernel.executive.ExecutiveController;
import com.piloto.cdi.kernel.interfaces.IEventStore;
import com.piloto.cdi.kernel.interfaces.ISnapshotStore;
import com.piloto.cdi.kernel.policy.BasicPolicyEngine;
import com.piloto.cdi.kernel.policy.PolicyEngine;
import com.piloto.cdi.kernel.service.EventApplicationService;
import com.piloto.cdi.kernel.store.JsonFileEventStore;
import com.piloto.cdi.kernel.store.JsonFileSnapshotStore;
import com.piloto.cdi.kernel.memory.*;
import com.piloto.cdi.kernel.orchestrator.*;
import com.piloto.cdi.gateway.embedding.GeminiEmbeddingProvider;
import com.piloto.cdi.gateway.llm.LLMRoleManager;
import com.piloto.cdi.gateway.llm.LLMRole;
import com.piloto.cdi.gateway.llm.bridge.LLMModelProvider;
import com.piloto.cdi.gateway.llm.bridge.CognitiveChatAgent;
import com.piloto.cdi.kernel.tools.ToolExecutionEngine;
import com.piloto.cdi.kernel.tools.ToolRegistry;
import com.piloto.cdi.kernel.tools.SandboxPolicy;
import com.piloto.cdi.kernel.tools.ApprovalGateway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.CompletableFuture;
import java.util.Collections;
import java.util.List;

/**
 * Spring configuration for CDI Cognitive Kernel integration.
 * 
 * This configuration instantiates all required kernel components as Spring
 * beans
 * for injection into the KernelBridgeService.
 * 
 * ARCHITECTURE:
 * - In-Memory stores for Phase 1 (development/testing)
 * - Stateless services (EvaluationService, BasicPolicyEngine)
 * - Thread-safe event application with lock striping
 * - ExecutiveController as central orchestrator
 * 
 * ENTERPRISE GRADE:
 * - Explicit dependency injection (no hidden coupling)
 * - Deterministic initialization order
 * - Production-ready for in-memory workloads
 * - Easy to swap implementations (e.g. PostgreSQL stores later)
 * 
 * @since API Gateway Phase 1
 */
@Configuration
public class KernelConfiguration {

    /**
     * Event store for persisting domain events.
     * 
     * Phase 1: In-memory implementation (non-durable)
     * Future: Replace with PostgreSQLEventStore for persistence
     * 
     * @return IEventStore bean (singleton)
     */
    @Bean
    public IEventStore eventStore() {
        return new JsonFileEventStore();
    }

    /**
     * Snapshot store for storing current state snapshots.
     * 
     * Phase 1: In-memory implementation (non-durable)
     * Future: Replace with PostgreSQLSnapshotStore for persistence
     * 
     * @return ISnapshotStore bean (singleton)
     */
    @Bean
    public ISnapshotStore snapshotStore() {
        return new JsonFileSnapshotStore();
    }

    /**
     * State transition controller for applying events to snapshots.
     * 
     * Stateless, pure function - safe for concurrent use.
     * 
     * @return StateTransitionController bean (singleton)
     */
    @Bean
    public StateTransitionController stateTransitionController() {
        return new StateTransitionController();
    }

    /**
     * Event application service coordinating event persistence and state
     * transitions.
     * 
     * Thread-safe with per-tenant lock striping (256 stripes).
     * 
     * @param eventStore           event store dependency
     * @param snapshotStore        snapshot store dependency
     * @param transitionController state transition controller dependency
     * @return EventApplicationService bean (singleton)
     */
    @Bean
    public EventApplicationService eventApplicationService(
            IEventStore eventStore,
            ISnapshotStore snapshotStore,
            StateTransitionController transitionController) {
        return new EventApplicationService(eventStore, snapshotStore, transitionController);
    }

    /**
     * Evaluation service for technical validation of commands.
     * 
     * Stateless, pure function - safe for concurrent use.
     * 
     * @return EvaluationService bean (singleton)
     */
    @Bean
    public EvaluationService evaluationService() {
        return new EvaluationService();
    }

    /**
     * Policy engine for business rule evaluation.
     * 
     * Phase 1: BasicPolicyEngine (deterministic, no LLM)
     * Future: Can extend with LLM-based policy inference
     * 
     * @return PolicyEngine bean (singleton)
     */
    @Bean
    public PolicyEngine policyEngine() {
        return new BasicPolicyEngine();
    }

    /**
     * Executive controller - central command orchestrator.
     * 
     * Coordinates complete command-to-event-to-state pipeline:
     * 1. Technical validation (EvaluationService)
     * 2. Business rule checking (PolicyEngine)
     * 3. Event translation
     * 4. Event application (EventApplicationService)
     * 
     * @param evaluationService       validation service dependency
     * @param policyEngine            policy engine dependency
     * @param eventApplicationService event application service dependency
     * @param snapshotStore           snapshot store dependency
     * @return ExecutiveController bean (singleton)
     */
    @Bean
    public ExecutiveController executiveController(
            EvaluationService evaluationService,
            PolicyEngine policyEngine,
            EventApplicationService eventApplicationService,
            ISnapshotStore snapshotStore) {
        return new ExecutiveController(
                evaluationService,
                policyEngine,
                eventApplicationService,
                snapshotStore);
    }

    /**
     * Vector store for semantic memory.
     * Uses 768 dimensions to match Gemini text-embedding-004 output.
     */
    @Bean
    public VectorStore vectorStore() {
        return new JsonFileVectorStore(768);
    }

    /**
     * Embedding provider — uses GeminiEmbeddingProvider (backed by Gemini
     * text-embedding-004).
     * Falls back to deterministic mock vectors when GOOGLE_API_KEY is not set.
     * GeminiEmbeddingProvider is a @Component, Spring injects it here.
     */
    @Bean
    public EmbeddingProvider embeddingProvider(GeminiEmbeddingProvider geminiEmbeddingProvider) {
        return geminiEmbeddingProvider;
    }

    /**
     * Memory manager to coordinate hybrid memory storage.
     */
    @Bean
    public MemoryManager memoryManager(EmbeddingProvider embeddingProvider, VectorStore vectorStore) {
        return new MemoryManager(embeddingProvider, vectorStore);
    }

    /**
     * Model Provider using Gemini through RoleManager.
     */
    @Bean
    public ModelProvider cognitiveModelProvider(LLMRoleManager roleManager) {
        return new LLMModelProvider(roleManager, LLMRole.CHAT_AGENT, "google", "gemini-2.5");
    }

    /**
     * Agent Coordinator with cognitive agents.
     */
    @Bean
    public AgentCoordinator agentCoordinator(ModelProvider modelProvider,
            com.piloto.cdi.gateway.governance.service.PromptGovernanceEngine promptEngine,
            com.piloto.cdi.gateway.events.ConversionSignalEmitter conversionEmitter) {
        BaseAgent chatAgent = new CognitiveChatAgent("PilotoCore", "CHAT_AGENT", modelProvider, promptEngine, conversionEmitter);
        return new AgentCoordinator(java.util.List.of(chatAgent));
    }

    /**
     * Self-Evaluator for reasoning loops.
     */
    @Bean
    public SelfEvaluator selfEvaluator() {
        return new SelfEvaluator();
    }

    /**
     * Tool Registry and Engine (Phase 5 CDI).
     */
    @Bean
    public ToolRegistry toolRegistry() {
        ToolRegistry registry = new ToolRegistry();
        registry.register(new com.piloto.cdi.kernel.tools.impl.LogAnalyzerTool());
        return registry;
    }

    @Bean
    public ToolExecutionEngine toolExecutionEngine(ToolRegistry registry,
            com.piloto.cdi.kernel.diagnostics.TelemetryCollector telemetry) {
        // Basic sandbox and approval policies for Phase 1
        ApprovalGateway gateway = new ApprovalGateway();
        gateway.setApprovalCallback((tool, payload) -> CompletableFuture.completedFuture(
                com.piloto.cdi.kernel.tools.ApprovalDecision.create(
                        true,
                        "Auto-approved in Dev",
                        0.0,
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList())));

        return new ToolExecutionEngine(registry,
                new SandboxPolicy(Collections.emptyList(), 10000),
                gateway,
                telemetry);
    }

    /**
     * Reasoning Orchestrator - The "Brain" (Phase 6 CDI).
     */
    @Bean
    public ReasoningOrchestrator reasoningOrchestrator(
            MemoryManager memoryManager,
            ToolExecutionEngine toolEngine,
            AgentCoordinator coordinator,
            SelfEvaluator evaluator,
            com.piloto.cdi.kernel.diagnostics.TelemetryCollector telemetry) {
        return new ReasoningOrchestrator(memoryManager, toolEngine, coordinator, evaluator, telemetry);
    }

    /**
     * Telemetry Collector - Centralized event ingestion.
     */
    @Bean
    public com.piloto.cdi.kernel.diagnostics.TelemetryCollector telemetryCollector() {
        return new com.piloto.cdi.kernel.diagnostics.TelemetryCollector();
    }

    /**
     * System Diagnostics - Self-Awareness Engine (Phase 7 CDI).
     */
    @Bean
    public com.piloto.cdi.kernel.diagnostics.SystemDiagnostics systemDiagnostics(
            com.piloto.cdi.kernel.diagnostics.TelemetryCollector telemetry) {
        return new com.piloto.cdi.kernel.diagnostics.SystemDiagnostics(
                telemetry,
                new com.piloto.cdi.kernel.diagnostics.MetricsEngine(),
                new com.piloto.cdi.kernel.diagnostics.AnomalyDetector(),
                new com.piloto.cdi.kernel.diagnostics.PerformanceAnalyzer(),
                new com.piloto.cdi.kernel.diagnostics.ConsistencyValidator(),
                new com.piloto.cdi.kernel.diagnostics.OptimizationRecommender(),
                new com.piloto.cdi.kernel.diagnostics.SystemHealthEngine(),
                new com.piloto.cdi.kernel.diagnostics.DiagnosticReviewGateway());
    }

    // ═══════════════════════════════════════════════════════════════════
    // COGNITIVE EVOLUTION ENGINE (CEE) — Phase 8 CDI
    // MAP-Elites + OPRO + PromptBreeder
    // ═══════════════════════════════════════════════════════════════════

    /**
     * MAP-Elites Quality-Diversity grid.
     * Thread-safe, persists via EvolutionStore, promotion via Wilson Score.
     */
    @Bean
    public com.piloto.cdi.gateway.governance.evolution.engine.MapElitesGrid mapElitesGrid(
            com.piloto.cdi.gateway.governance.evolution.store.EvolutionStore evolutionStore,
            com.piloto.cdi.gateway.governance.evolution.config.EvolutionProperties props) {
        return new com.piloto.cdi.gateway.governance.evolution.engine.MapElitesGrid(
                evolutionStore, props.getMinSamplesToChallenge());
    }

    /**
     * Epsilon-greedy traffic router for challenger evaluation.
     */
    @Bean
    public com.piloto.cdi.gateway.governance.evolution.engine.TrafficRouter trafficRouter(
            com.piloto.cdi.gateway.governance.evolution.engine.MapElitesGrid grid,
            com.piloto.cdi.gateway.governance.evolution.config.EvolutionProperties props) {
        return new com.piloto.cdi.gateway.governance.evolution.engine.TrafficRouter(
                grid, props.getEpsilonGreedy());
    }

    /**
     * OPRO + PromptBreeder meta-optimizer.
     * Uses LLMRoleManager for all LLM calls via OPTIMIZER role.
     */
    @Bean
    public com.piloto.cdi.gateway.governance.evolution.engine.OptimizerService optimizerService(
            com.piloto.cdi.gateway.governance.evolution.engine.MapElitesGrid grid,
            com.piloto.cdi.gateway.governance.evolution.store.EvolutionStore store,
            com.piloto.cdi.gateway.llm.LLMRoleManager roleManager,
            com.piloto.cdi.gateway.governance.evolution.config.EvolutionProperties props) {
        return new com.piloto.cdi.gateway.governance.evolution.engine.OptimizerService(
                grid, store, roleManager,
                props.getOptimizer().getMinVariants(),
                props.getOptimizer().getMinSamplesPerVariant());
    }

    /**
     * Per-tool statistics registry for UCB1 selection.
     */
    @Bean
    public com.piloto.cdi.kernel.tools.selection.PerToolStatsRegistry perToolStatsRegistry() {
        return new com.piloto.cdi.kernel.tools.selection.PerToolStatsRegistry();
    }

    /**
     * UCB1 tool selection policy (multi-armed bandit).
     */
    @Bean
    public com.piloto.cdi.kernel.tools.selection.UCB1SelectionPolicy ucb1SelectionPolicy() {
        return new com.piloto.cdi.kernel.tools.selection.UCB1SelectionPolicy();
    }
}
