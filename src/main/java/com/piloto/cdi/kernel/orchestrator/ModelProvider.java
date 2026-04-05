package com.piloto.cdi.kernel.orchestrator;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public abstract class ModelProvider {
    
    public abstract CompletableFuture<String> generate(
        String systemPrompt,
        String userPrompt,
        Map<String, Object> context
    );
    
    public abstract String getProviderName();
    
    public abstract String getModelName();
}
