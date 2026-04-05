package com.piloto.cdi.gateway.governance.controller.dto;

import com.piloto.cdi.kernel.governance.type.MemoryCategory;

import java.util.UUID;

public record DomainMemoryRequest(
        UUID id,
        String tenantId,
        MemoryCategory category,
        String title,
        String content,
        Integer priority,
        Integer version,
        Boolean isActive,
        String createdBy) {
}
