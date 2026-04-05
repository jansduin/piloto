package com.piloto.cdi.kernel.diagnostics;

import java.util.*;

public class DiagnosticReviewGateway {

    private final Map<String, String> approvedChanges = new HashMap<>();
    private final Map<String, String> rejectedChanges = new HashMap<>();
    private final List<Map<String, Object>> pendingRecommendations = new ArrayList<>();

    public Map<String, Object> review(List<Map<String, Object>> recommendations) {
        if (recommendations == null || recommendations.isEmpty()) {
            return Map.of(
                "approved_changes", List.of(),
                "rejected_changes", List.of()
            );
        }

        pendingRecommendations.clear();
        pendingRecommendations.addAll(recommendations);

        List<String> approved = new ArrayList<>();
        List<String> rejected = new ArrayList<>();

        for (Map<String, Object> rec : recommendations) {
            String action = (String) rec.get("action");
            double risk = ((Number) rec.getOrDefault("risk", 0.5)).doubleValue();

            ReviewDecision decision = simulateHumanReview(action, risk);

            if (decision == ReviewDecision.APPROVE) {
                approved.add(action);
                approvedChanges.put(action, (String) rec.get("component"));
            } else {
                rejected.add(action);
                rejectedChanges.put(action, decision.getRationale());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("approved_changes", List.copyOf(approved));
        result.put("rejected_changes", List.copyOf(rejected));
        result.put("total_reviewed", recommendations.size());
        result.put("approval_rate", recommendations.isEmpty() ? 0.0 : 
            (double) approved.size() / recommendations.size());

        return result;
    }

    private ReviewDecision simulateHumanReview(String action, double risk) {
        if (risk >= 0.7) {
            return ReviewDecision.REJECT_HIGH_RISK;
        }

        if (action.toLowerCase().contains("threshold") && risk < 0.4) {
            return ReviewDecision.APPROVE;
        }

        if (action.toLowerCase().contains("timeout") && risk < 0.5) {
            return ReviewDecision.APPROVE;
        }

        if (action.toLowerCase().contains("learning") || 
            action.toLowerCase().contains("heuristics")) {
            return risk < 0.35 ? ReviewDecision.APPROVE : ReviewDecision.REJECT_NEEDS_TESTING;
        }

        if (action.toLowerCase().contains("retry") || 
            action.toLowerCase().contains("validation")) {
            return risk < 0.4 ? ReviewDecision.APPROVE : ReviewDecision.REJECT_NEEDS_REVIEW;
        }

        return risk < 0.5 ? ReviewDecision.APPROVE : ReviewDecision.REJECT_UNCERTAIN;
    }

    public List<Map<String, Object>> getPendingRecommendations() {
        return List.copyOf(pendingRecommendations);
    }

    public Map<String, String> getApprovedChanges() {
        return Map.copyOf(approvedChanges);
    }

    public Map<String, String> getRejectedChanges() {
        return Map.copyOf(rejectedChanges);
    }

    private enum ReviewDecision {
        APPROVE("Approved for implementation"),
        REJECT_HIGH_RISK("Risk level too high for auto-approval"),
        REJECT_NEEDS_TESTING("Requires additional testing before approval"),
        REJECT_NEEDS_REVIEW("Needs deeper architectural review"),
        REJECT_UNCERTAIN("Benefit/risk ratio unclear");

        private final String rationale;

        ReviewDecision(String rationale) {
            this.rationale = rationale;
        }

        public String getRationale() {
            return rationale;
        }
    }
}
