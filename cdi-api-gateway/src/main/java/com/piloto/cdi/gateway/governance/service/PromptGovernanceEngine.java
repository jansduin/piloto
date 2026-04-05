package com.piloto.cdi.gateway.governance.service;

import com.piloto.cdi.gateway.governance.repository.PromptRegistry;
import com.piloto.cdi.kernel.governance.model.PromptEntity;
import com.piloto.cdi.kernel.governance.type.PromptLayer;
import com.piloto.cdi.kernel.governance.type.PromptRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Engine responsible for composing and validating prompts based on the
 * hierarchy:
 * SYSTEM_CORE -> ROLE -> DOMAIN -> TENANT -> RUNTIME.
 */
@Service
public class PromptGovernanceEngine {

    private final PromptRegistry promptRegistry;

    @Autowired
    public PromptGovernanceEngine(PromptRegistry promptRegistry) {
        this.promptRegistry = promptRegistry;
    }

    /**
     * Composes the final system prompt for a specific role and context.
     *
     * @param role     The role of the model (PROPOSER, CRITIC, etc.)
     * @param domain   The business domain (optional)
     * @param tenantId The tenant ID (optional)
     * @return The composed prompt string.
     */
    public String composeSystemPrompt(PromptRole role, String domain, String tenantId) {
        StringBuilder finalPrompt = new StringBuilder();

        // 1. SYSTEM_CORE (Global Base)
        appendLayer(finalPrompt, PromptLayer.SYSTEM_CORE, null, null, null);

        // 2. ROLE (Specific to Proposer/Critic/etc)
        appendLayer(finalPrompt, PromptLayer.ROLE, role, null, null);

        // 3. DOMAIN (Industry specific)
        if (domain != null) {
            appendLayer(finalPrompt, PromptLayer.DOMAIN, role, domain, null);
        }

        // 4. TENANT (Customer specific)
        if (tenantId != null) {
            appendLayer(finalPrompt, PromptLayer.TENANT, role, domain, tenantId);
        }

        return finalPrompt.toString();
    }

    public void registerPrompt(PromptEntity prompt) {
        // Basic validation could go here
        promptRegistry.save(prompt);
    }

    public java.util.List<PromptEntity> getAllPrompts() {
        return promptRegistry.findAll();
    }

    /**
     * Updates the content of an existing prompt.
     * CQRS-compliant: routes through a dedicated update path.
     *
     * @param id         UUID of the prompt to update
     * @param newContent new prompt instructions
     * @return Optional containing the updated entity, or empty if not found
     */
    public java.util.Optional<PromptEntity> updatePromptContent(java.util.UUID id, String newContent) {
        if (newContent == null || newContent.isBlank()) {
            throw new IllegalArgumentException("Prompt content must not be empty");
        }
        try {
            return java.util.Optional.of(promptRegistry.update(id, newContent));
        } catch (IllegalArgumentException e) {
            return java.util.Optional.empty();
        }
    }

    /**
     * Retrieves a single PromptEntity by ID.
     *
     * @param id UUID of the prompt
     * @return Optional containing the entity, or empty if not found
     */
    public java.util.Optional<PromptEntity> getPromptById(java.util.UUID id) {
        return promptRegistry.findById(id);
    }

    private void appendLayer(StringBuilder builder, PromptLayer layer, PromptRole role, String domain,
            String tenantId) {
        Optional<PromptEntity> prompt = promptRegistry.findActive(layer, role, domain, tenantId);
        if (prompt.isPresent()) {
            if (builder.length() > 0) {
                builder.append("\n\n");
            }
            builder.append("### ").append(layer.name()).append(" INSTRUCTIONS ###\n");
            builder.append(prompt.get().getContent());
        }
    }
}
