package com.piloto.cdi.gateway.governance.controller.dto;

import com.piloto.cdi.kernel.governance.type.MemoryCategory;
import com.piloto.cdi.kernel.governance.type.PromptLayer;
import com.piloto.cdi.kernel.governance.type.PromptRole;

import java.util.UUID;

public record PromptRegistrationRequest(
        UUID id,
        String name,
        PromptLayer layer,
        PromptRole role,
        String domain,
        String tenantId,
        String content,
        Integer version,
        Boolean isActive) {
}
