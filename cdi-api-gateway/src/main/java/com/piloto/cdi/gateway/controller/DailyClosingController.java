package com.piloto.cdi.gateway.controller;

import com.piloto.cdi.kernel.diagnostics.SystemDiagnostics;
import com.piloto.cdi.kernel.types.TenantID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * Controller for Daily Closing operations.
 * Captures system state snapshots for historical tracking.
 */
@RestController
@RequestMapping("/api/closing")
@CrossOrigin(origins = "*")
public class DailyClosingController {

    private static final Logger logger = LoggerFactory.getLogger(DailyClosingController.class);
    private final SystemDiagnostics diagnostics;

    public DailyClosingController(SystemDiagnostics diagnostics) {
        this.diagnostics = diagnostics;
    }

    @PostMapping("/daily")
    public ResponseEntity<Map<String, Object>> performDailyClosing(@RequestBody Map<String, String> request) {
        String tenantId = request.getOrDefault("tenantId", "default_tenant");
        logger.info("Initiating Daily Closing for tenant: {}", tenantId);

        // Capture current health snapshot
        double healthScore = diagnostics.getLatestHealthScore();
        Map<String, Double> metrics = diagnostics.getLatestMetrics();

        // In a persistent scenario, this would be saved to a 'DailyClosing' repository
        logger.info("Daily Closing COMPLETED. Health: {}, Date: {}", healthScore, LocalDate.now());

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "date", LocalDate.now().toString(),
                "health_at_closing", healthScore,
                "metrics_snapshot", metrics));
    }
}
