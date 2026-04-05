package com.piloto.cdi.kernel.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

public class ApprovalGateway {
    private BiFunction<BaseTool, Map<String, Object>, CompletableFuture<ApprovalDecision>> approvalCallback;
    
    public ApprovalGateway() {
        this.approvalCallback = null;
    }
    
    public void setApprovalCallback(BiFunction<BaseTool, Map<String, Object>, CompletableFuture<ApprovalDecision>> callback) {
        this.approvalCallback = callback;
    }
    
    public CompletableFuture<ApprovalDecision> requestApproval(BaseTool tool, Map<String, Object> payload) {
        if (tool == null) {
            throw new IllegalArgumentException("tool must not be null");
        }
        if (payload == null) {
            throw new IllegalArgumentException("payload must not be null");
        }
        
        if (approvalCallback == null) {
            return CompletableFuture.completedFuture(generateDefaultDecision(tool, payload));
        }
        
        return approvalCallback.apply(tool, payload);
    }
    
    private ApprovalDecision generateDefaultDecision(BaseTool tool, Map<String, Object> payload) {
        ToolDefinition def = tool.getDefinition();
        
        List<String> pros = new ArrayList<>();
        List<String> cons = new ArrayList<>();
        List<String> benefits = new ArrayList<>();
        
        pros.add("Tool is registered in the system");
        pros.add("Tool version: " + def.getVersion());
        
        if (def.getCapabilities().contains(ToolCapability.WRITE)) {
            cons.add("Tool has WRITE capability - may modify data");
        }
        if (def.getCapabilities().contains(ToolCapability.SYSTEM)) {
            cons.add("Tool has SYSTEM capability - may affect system state");
        }
        if (def.getCapabilities().contains(ToolCapability.NETWORK)) {
            cons.add("Tool has NETWORK capability - may perform external calls");
        }
        if (def.getCapabilities().contains(ToolCapability.EXECUTE)) {
            cons.add("Tool has EXECUTE capability - may run arbitrary code");
        }
        
        benefits.add("Automated execution of: " + def.getDescription());
        benefits.add("Capability set: " + def.getCapabilities());
        
        double riskScore = calculateRiskScore(def);
        
        boolean approved = false;
        String rationale = "No approval callback configured - default to DENY for safety";
        
        return ApprovalDecision.create(approved, rationale, riskScore, pros, cons, benefits);
    }
    
    private double calculateRiskScore(ToolDefinition def) {
        double score = 0.1;
        
        if (def.getCapabilities().contains(ToolCapability.WRITE)) {
            score += 0.2;
        }
        if (def.getCapabilities().contains(ToolCapability.SYSTEM)) {
            score += 0.3;
        }
        if (def.getCapabilities().contains(ToolCapability.NETWORK)) {
            score += 0.15;
        }
        if (def.getCapabilities().contains(ToolCapability.EXECUTE)) {
            score += 0.4;
        }
        
        return Math.min(score, 1.0);
    }
}
