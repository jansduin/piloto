package com.piloto.cdi.gateway.governance.controller;

import com.piloto.cdi.gateway.governance.controller.dto.DomainMemoryRequest;
import com.piloto.cdi.gateway.governance.controller.dto.MemoryUpdateRequest;
import com.piloto.cdi.gateway.governance.controller.dto.PromptRegistrationRequest;
import com.piloto.cdi.gateway.governance.controller.dto.PromptUpdateRequest;
import com.piloto.cdi.gateway.governance.service.DomainMemoryStore;
import com.piloto.cdi.gateway.governance.service.PromptGovernanceEngine;
import com.piloto.cdi.kernel.executive.ExecutionResult;
import com.piloto.cdi.kernel.executive.ExecutiveController;
import com.piloto.cdi.kernel.command.BaseCommand;
import com.piloto.cdi.kernel.command.CommandType;
import com.piloto.cdi.kernel.governance.model.DomainMemoryItem;
import com.piloto.cdi.kernel.governance.model.PromptEntity;
import com.piloto.cdi.kernel.types.TenantID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * REST Controller for the Cognitive Governance Layer.
 * Allows runtime management of Prompts and Domain Memory.
 *
 * CDI Doctrine:
 * - No PUT endpoints (ZERO BYPASS)
 * - All mutations flow through POST command endpoints
 * - GET endpoints for query (read side of CQRS)
 *
 * Base path: /gov
 */
@RestController
@RequestMapping("/gov")
public class GovernanceController {

    private static final Logger logger = LoggerFactory.getLogger(GovernanceController.class);

    private final PromptGovernanceEngine promptEngine;
    private final DomainMemoryStore memoryStore;
    private final ExecutiveController executiveController;

    @Autowired
    public GovernanceController(PromptGovernanceEngine promptEngine,
            DomainMemoryStore memoryStore,
            ExecutiveController executiveController) {
        this.promptEngine = promptEngine;
        this.memoryStore = memoryStore;
        this.executiveController = executiveController;
    }

    // =================================================================================================
    // PROMPT — QUERY (Read Side)
    // =================================================================================================

    /**
     * GET /gov/prompts — Returns all registered prompts.
     */
    @GetMapping("/prompts")
    public ResponseEntity<List<PromptEntity>> getAllPrompts() {
        List<PromptEntity> prompts = promptEngine.getAllPrompts();
        logger.debug("Returning {} prompts", prompts.size());
        return ResponseEntity.ok(prompts);
    }

    // =================================================================================================
    // PROMPT — COMMAND (Write Side)
    // =================================================================================================

    /**
     * POST /gov/prompts — Registers a new prompt in the governance engine.
     */
    @PostMapping("/prompts")
    public ResponseEntity<PromptEntity> registerPrompt(
            @RequestBody PromptRegistrationRequest request) {
        logger.info("Registering prompt: {} (Layer: {})", request.name(), request.layer());

        PromptEntity entity = PromptEntity.builder()
                .id(request.id() != null ? request.id() : UUID.randomUUID())
                .name(request.name())
                .layer(request.layer())
                .role(request.role())
                .domain(request.domain())
                .tenantId(request.tenantId())
                .content(request.content())
                .version(request.version() != null ? request.version() : 1)
                .isActive(request.isActive() != null ? request.isActive() : true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        promptEngine.registerPrompt(entity);

        // Audit trail: emit UPDATE_GOAL event for EventStore tracking
        emitAuditEvent(request.tenantId(), "PROMPT_REGISTERED",
                Map.of("promptName", request.name(), "layer", String.valueOf(request.layer())));

        return ResponseEntity.ok(entity);
    }

    /**
     * POST /gov/prompts/commands/update-content
     * CQRS command to update prompt content. Increments version, persists to disk.
     *
     * Body: { "id": "uuid", "content": "new instructions..." }
     */
    @PostMapping("/prompts/commands/update-content")
    public ResponseEntity<?> handlePromptUpdateCommand(@RequestBody PromptUpdateRequest request) {
        if (request.id() == null || request.id().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "id is required"));
        }
        if (request.content() == null || request.content().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "content is required"));
        }

        UUID id;
        try {
            id = UUID.fromString(request.id());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "id must be a valid UUID"));
        }

        logger.info("Processing Prompt Update Command for ID: {}", id);

        Optional<PromptEntity> updated = promptEngine.updatePromptContent(id, request.content());

        if (updated.isEmpty()) {
            logger.warn("Prompt not found for update: {}", id);
            return ResponseEntity.notFound().build();
        }

        // Audit trail: record in EventStore
        PromptEntity entity = updated.get();
        emitAuditEvent(entity.getTenantId(), "PROMPT_CONTENT_UPDATED",
                Map.of("promptId", id.toString(), "newVersion", String.valueOf(entity.getVersion())));

        logger.info("Prompt updated successfully: id={}, version={}", id, entity.getVersion());
        return ResponseEntity.ok(entity);
    }

    // =================================================================================================
    // DOMAIN MEMORY — QUERY (Read Side)
    // =================================================================================================

    /**
     * GET /gov/memory — Returns all registered domain memory items.
     */
    @GetMapping("/memory")
    public ResponseEntity<List<DomainMemoryItem>> getAllMemoryItems() {
        List<DomainMemoryItem> items = memoryStore.getAllMemoryItems();
        logger.debug("Returning {} memory items", items.size());
        return ResponseEntity.ok(items);
    }

    // =================================================================================================
    // DOMAIN MEMORY — COMMAND (Write Side)
    // =================================================================================================

    /**
     * POST /gov/memory — Stores a new domain memory item.
     */
    @PostMapping("/memory")
    public ResponseEntity<DomainMemoryItem> addMemoryItem(
            @RequestBody DomainMemoryRequest request) {
        logger.info("Adding memory item: {} (Tenant: {})", request.title(), request.tenantId());

        DomainMemoryItem entity = DomainMemoryItem.builder()
                .id(request.id() != null ? request.id() : UUID.randomUUID())
                .tenantId(request.tenantId())
                .category(request.category())
                .title(request.title())
                .content(request.content())
                .priority(request.priority() != null ? request.priority() : 1)
                .version(request.version() != null ? request.version() : 1)
                .isActive(request.isActive() != null ? request.isActive() : true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .createdBy(request.createdBy() != null ? request.createdBy() : "API")
                .build();

        memoryStore.storeItem(entity);

        // Audit trail
        emitAuditEvent(request.tenantId(), "MEMORY_ITEM_REGISTERED",
                Map.of("title", request.title()));

        return ResponseEntity.ok(entity);
    }

    /**
     * POST /gov/memory/commands/update-content
     * CQRS command to update domain memory content. Increments version, persists to
     * disk.
     *
     * Body: { "id": "uuid", "content": "updated content..." }
     */
    @PostMapping("/memory/commands/update-content")
    public ResponseEntity<?> handleMemoryUpdateCommand(@RequestBody MemoryUpdateRequest request) {
        if (request.id() == null || request.id().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "id is required"));
        }
        if (request.content() == null || request.content().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "content is required"));
        }

        UUID id;
        try {
            id = UUID.fromString(request.id());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "id must be a valid UUID"));
        }

        logger.info("Processing Memory Update Command for ID: {}", id);

        Optional<DomainMemoryItem> updated = memoryStore.updateMemoryContent(id, request.content());

        if (updated.isEmpty()) {
            logger.warn("Memory item not found for update: {}", id);
            return ResponseEntity.notFound().build();
        }

        // Audit trail: record in EventStore
        DomainMemoryItem item = updated.get();
        emitAuditEvent(item.getTenantId(), "MEMORY_CONTENT_UPDATED",
                Map.of("itemId", id.toString(), "newVersion", String.valueOf(item.getVersion())));

        logger.info("Memory item updated successfully: id={}, version={}", id, item.getVersion());
        return ResponseEntity.ok(item);
    }

    // =================================================================================================
    // PRIVATE HELPERS
    // =================================================================================================

    /**
     * Emits an audit event to the CDI EventStore via ExecutiveController.
     * This provides traceability for governance mutations without storing
     * business logic in the Kernel command pipeline.
     */
    private void emitAuditEvent(String tenantId, String eventName, Map<String, Object> details) {
        try {
            String resolvedTenant = (tenantId != null && !tenantId.isBlank())
                    ? tenantId
                    : "default_tenant";

            Map<String, Object> payload = new java.util.HashMap<>(details);
            payload.put("governanceEvent", eventName);
            payload.put("timestamp", Instant.now().toString());

            BaseCommand auditCommand = BaseCommand.create(
                    TenantID.of(resolvedTenant),
                    CommandType.UPDATE_STATE,
                    payload);

            ExecutionResult result = executiveController.execute(auditCommand);
            if (!result.isSuccess()) {
                logger.warn("Governance audit event not recorded in EventStore: {}", result.getErrors());
            } else {
                logger.debug("Governance audit event recorded: {} for tenant={}", eventName, resolvedTenant);
            }
        } catch (Exception e) {
            // Audit failure must never block the governance operation itself
            logger.error("Non-critical: failed to record governance audit event '{}': {}", eventName, e.getMessage());
        }
    }
}
