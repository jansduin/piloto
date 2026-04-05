package com.piloto.cdi.kernel.diagnostics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetricsEngine {

        public Map<String, Double> compute(List<TelemetryCollector.TelemetryEvent> events) {
                if (events == null || events.isEmpty()) {
                        return createEmptyMetrics();
                }

                Map<String, Double> metrics = new HashMap<>();
                long totalEvents = events.size();

                // 1. Failure Rate (ERROR, FAILURE, EXCEPTION in type or payload status)
                long failureCount = events.stream()
                                .filter(e -> isFailure(e))
                                .count();
                metrics.put("failure_rate", totalEvents > 0 ? (double) failureCount / totalEvents : 0.0);

                // 2. Average Execution Time (payload.execution_time_ms or duration_ms)
                double avgTime = events.stream()
                                .map(e -> e.getPayload().getOrDefault("execution_time_ms",
                                                e.getPayload().get("duration_ms")))
                                .filter(val -> val instanceof Number)
                                .mapToDouble(val -> ((Number) val).doubleValue())
                                .average()
                                .orElse(0.0);
                metrics.put("avg_execution_time", avgTime);

                // 3. Tool Timeout Rate
                long toolEvents = events.stream()
                                .filter(e -> "TOOL".equalsIgnoreCase(e.getComponent()))
                                .count();
                long timeoutCount = events.stream()
                                .filter(e -> "TOOL".equalsIgnoreCase(e.getComponent()) &&
                                                e.getEventType().toUpperCase().contains("TIMEOUT"))
                                .count();
                metrics.put("tool_timeout_rate", toolEvents > 0 ? (double) timeoutCount / toolEvents : 0.0);

                // 4. Approval Denial Rate
                long approvalEvents = events.stream()
                                .filter(e -> "APPROVAL".equalsIgnoreCase(e.getComponent()))
                                .count();
                long deniedCount = events.stream()
                                .filter(e -> "APPROVAL".equalsIgnoreCase(e.getComponent()) &&
                                                e.getEventType().toUpperCase().contains("DENIED"))
                                .count();
                metrics.put("approval_denial_rate", approvalEvents > 0 ? (double) deniedCount / approvalEvents : 0.0);

                // 5. Avg Reasoning Score
                double avgScore = events.stream()
                                .map(e -> e.getPayload().get("reasoning_score"))
                                .filter(val -> val instanceof Number)
                                .mapToDouble(val -> ((Number) val).doubleValue())
                                .average()
                                .orElse(0.5);
                metrics.put("avg_reasoning_score", avgScore);

                // 6. Iteration Average
                double avgIter = events.stream()
                                .map(e -> e.getPayload().get("iteration"))
                                .filter(val -> val instanceof Number)
                                .mapToDouble(val -> ((Number) val).doubleValue())
                                .average()
                                .orElse(0.0);
                metrics.put("iteration_average", avgIter);

                // Success Rate (Complement of Failure Rate)
                metrics.put("success_rate", 1.0 - metrics.get("failure_rate"));

                return metrics;
        }

        private boolean isFailure(TelemetryCollector.TelemetryEvent event) {
                String type = event.getEventType().toUpperCase();
                if (type.contains("ERROR") || type.contains("FAILURE") || type.contains("EXCEPTION")) {
                        return true;
                }
                Object status = event.getPayload().get("status");
                return status != null && status.toString().toUpperCase().matches(".*(ERROR|FAILURE|FAILED).*");
        }

        private Map<String, Double> createEmptyMetrics() {
                Map<String, Double> metrics = new HashMap<>();
                metrics.put("failure_rate", 0.0);
                metrics.put("success_rate", 1.0);
                metrics.put("avg_execution_time", 0.0);
                metrics.put("tool_timeout_rate", 0.0);
                metrics.put("approval_denial_rate", 0.0);
                return metrics;
        }
}
