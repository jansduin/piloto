package com.piloto.cdi.kernel.context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HallucinationGuard {

    private static final double LOW_SCORE_THRESHOLD = 0.6;
    private static final double CRITICAL_SCORE_THRESHOLD = 0.4;

    public enum RiskLevel {
        LOW,
        MEDIUM,
        HIGH
    }

    public Map<String, Object> check(
        Map<String, Object> groundingResult,
        double evaluationScore
    ) {
        if (groundingResult == null) {
            throw new IllegalArgumentException("Grounding result cannot be null");
        }

        boolean grounded = (Boolean) groundingResult.getOrDefault("grounded", false);
        List<?> unsupportedClaims = (List<?>) groundingResult.getOrDefault("unsupported_claims", List.of());
        double confidenceScore = ((Number) groundingResult.getOrDefault("confidence_score", 0.5)).doubleValue();

        RiskLevel riskLevel = assessRiskLevel(
            grounded,
            unsupportedClaims.size(),
            evaluationScore,
            confidenceScore
        );

        boolean actionRequired = (riskLevel == RiskLevel.HIGH);

        Map<String, Object> result = new HashMap<>();
        result.put("risk_level", riskLevel.name());
        result.put("action_required", actionRequired);
        result.put("grounding_issues", unsupportedClaims.size());
        result.put("evaluation_score", evaluationScore);
        result.put("confidence_score", confidenceScore);
        result.put("recommendation", generateRecommendation(riskLevel));

        return Map.copyOf(result);
    }

    private RiskLevel assessRiskLevel(
        boolean grounded,
        int unsupportedClaimsCount,
        double evaluationScore,
        double confidenceScore
    ) {
        if (!grounded && unsupportedClaimsCount > 2) {
            return RiskLevel.HIGH;
        }

        if (evaluationScore < CRITICAL_SCORE_THRESHOLD) {
            return RiskLevel.HIGH;
        }

        if (!grounded || evaluationScore < LOW_SCORE_THRESHOLD) {
            return RiskLevel.MEDIUM;
        }

        if (confidenceScore < 0.7 || unsupportedClaimsCount > 0) {
            return RiskLevel.MEDIUM;
        }

        return RiskLevel.LOW;
    }

    private String generateRecommendation(RiskLevel riskLevel) {
        switch (riskLevel) {
            case HIGH:
                return "Block result. Require human review before proceeding. " +
                       "Re-generate response with stricter grounding constraints.";
            case MEDIUM:
                return "Flag for review. Consider regenerating with additional context " +
                       "or verification steps.";
            case LOW:
                return "Proceed with result. Continue monitoring for consistency.";
            default:
                return "Unknown risk level.";
        }
    }

    public boolean shouldBlock(Map<String, Object> checkResult) {
        String riskLevel = (String) checkResult.get("risk_level");
        return RiskLevel.HIGH.name().equals(riskLevel);
    }

    public Map<String, Object> analyzeRiskFactors(
        boolean grounded,
        int unsupportedClaimsCount,
        double evaluationScore,
        double confidenceScore
    ) {
        Map<String, Object> factors = new HashMap<>();
        
        factors.put("grounding_risk", !grounded ? "HIGH" : "LOW");
        factors.put("claims_risk", 
            unsupportedClaimsCount > 2 ? "HIGH" : 
            unsupportedClaimsCount > 0 ? "MEDIUM" : "LOW");
        factors.put("evaluation_risk",
            evaluationScore < CRITICAL_SCORE_THRESHOLD ? "HIGH" :
            evaluationScore < LOW_SCORE_THRESHOLD ? "MEDIUM" : "LOW");
        factors.put("confidence_risk",
            confidenceScore < 0.5 ? "HIGH" :
            confidenceScore < 0.7 ? "MEDIUM" : "LOW");
        
        return Map.copyOf(factors);
    }
}
