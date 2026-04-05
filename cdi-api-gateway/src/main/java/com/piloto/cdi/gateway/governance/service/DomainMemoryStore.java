package com.piloto.cdi.gateway.governance.service;

import com.piloto.cdi.gateway.governance.repository.DomainMemoryRepository;
import com.piloto.cdi.kernel.governance.model.DomainMemoryItem;
import com.piloto.cdi.kernel.governance.type.MemoryCategory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service to manage and retrieve domain-specific memory items (Business Rules,
 * Policies).
 */
@Service
public class DomainMemoryStore {

    private final DomainMemoryRepository repository;

    @Autowired
    public DomainMemoryStore(DomainMemoryRepository repository) {
        this.repository = repository;
    }

    public void storeItem(DomainMemoryItem item) {
        repository.save(item);
    }

    /**
     * Retrieves relevant domain memory as a formatted string context.
     */
    public String retrieveContextMatches(String tenantId, String goalContext) {
        // In Phase 1, we simply inject ALL active rules for the tenant.
        // In future phases, we would use vector search against 'goalContext'.

        StringBuilder contextBuilder = new StringBuilder();

        List<DomainMemoryItem> policies = repository.findByTenantAndCategory(tenantId, MemoryCategory.POLICY);
        appendSection(contextBuilder, "POLICIES", policies);

        List<DomainMemoryItem> risks = repository.findByTenantAndCategory(tenantId, MemoryCategory.RISK_CONSTRAINT);
        appendSection(contextBuilder, "RISK CONSTRAINTS", risks);

        List<DomainMemoryItem> businessRules = repository.findByTenantAndCategory(tenantId,
                MemoryCategory.BUSINESS_RULE);
        appendSection(contextBuilder, "BUSINESS RULES", businessRules);

        return contextBuilder.toString();
    }

    /**
     * Specific method for the Verifier to check constraints.
     */
    public List<String> getRiskConstraints(String tenantId) {
        return repository.findByTenantAndCategory(tenantId, MemoryCategory.RISK_CONSTRAINT).stream()
                .map(DomainMemoryItem::getContent)
                .collect(Collectors.toList());
    }

    private void appendSection(StringBuilder builder, String header, List<DomainMemoryItem> items) {
        if (items.isEmpty())
            return;

        builder.append("\n--- ").append(header).append(" ---\n");
        for (DomainMemoryItem item : items) {
            builder.append("- [").append(item.getTitle()).append("]: ")
                    .append(item.getContent()).append("\n");
        }
    }

    public List<DomainMemoryItem> getAllMemoryItems() {
        return repository.findAll();
    }

    /**
     * Updates the content of an existing domain memory item.
     * CQRS-compliant: routes through the repository's dedicated update path.
     *
     * @param id         UUID of the item to update
     * @param newContent new content for the memory item
     * @return Optional containing the updated entity, or empty if not found
     */
    public java.util.Optional<DomainMemoryItem> updateMemoryContent(java.util.UUID id, String newContent) {
        if (newContent == null || newContent.isBlank()) {
            throw new IllegalArgumentException("Memory content must not be empty");
        }
        try {
            return java.util.Optional.of(repository.update(id, newContent));
        } catch (IllegalArgumentException e) {
            return java.util.Optional.empty();
        }
    }

    /**
     * Retrieves a single DomainMemoryItem by ID.
     *
     * @param id UUID of the memory item
     * @return Optional containing the entity, or empty if not found
     */
    public java.util.Optional<DomainMemoryItem> getMemoryById(java.util.UUID id) {
        return repository.findById(id);
    }
}
