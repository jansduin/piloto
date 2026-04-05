package com.piloto.cdi.gateway.governance.controller;

import com.piloto.cdi.kernel.diagnostics.SystemDiagnostics;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/governance")
public class HealthController {

    private final SystemDiagnostics systemDiagnostics;

    public HealthController(SystemDiagnostics systemDiagnostics) {
        this.systemDiagnostics = systemDiagnostics;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getSystemHealth() {
        try {
            double score = systemDiagnostics.getLatestHealthScore();
            Map<String, Double> metrics = systemDiagnostics.getLatestMetrics();

            return ResponseEntity.ok(Map.of(
                    "status", "ONLINE",
                    "health_score", score,
                    "metrics", metrics,
                    "timestamp", System.currentTimeMillis()));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "status", "DEGRADED",
                    "health_score", 0.5,
                    "error", "Diagnostic internal failure: " + e.getMessage(),
                    "timestamp", System.currentTimeMillis()));
        }
    }

    @GetMapping("/diagnostics/run")
    public ResponseEntity<Map<String, Object>> runDiagnostics() {
        // In a real scenario, this might take a trace ID or run on latest events
        // For Phase 7, we run on collected telemetry buffer
        Map<String, Object> report = systemDiagnostics.runFullDiagnostic(null);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/telemetry")
    public ResponseEntity<Object> getTelemetry() {
        return ResponseEntity.ok(systemDiagnostics.getTelemetry().getAllEvents());
    }
}
