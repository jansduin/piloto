package com.piloto.cdi.kernel.tools;

import java.util.List;
import java.util.Objects;

public final class ApprovalDecision {
    private final boolean approved;
    private final String rationale;
    private final double riskScore;
    private final List<String> pros;
    private final List<String> cons;
    private final List<String> benefits;
    
    private ApprovalDecision(
            boolean approved,
            String rationale,
            double riskScore,
            List<String> pros,
            List<String> cons,
            List<String> benefits) {
        if (rationale == null || rationale.isBlank()) {
            throw new IllegalArgumentException("rationale must not be empty");
        }
        if (riskScore < 0.0 || riskScore > 1.0) {
            throw new IllegalArgumentException("riskScore must be between 0 and 1");
        }
        if (pros == null) {
            throw new IllegalArgumentException("pros must not be null");
        }
        if (cons == null) {
            throw new IllegalArgumentException("cons must not be null");
        }
        if (benefits == null) {
            throw new IllegalArgumentException("benefits must not be null");
        }
        
        this.approved = approved;
        this.rationale = rationale;
        this.riskScore = riskScore;
        this.pros = List.copyOf(pros);
        this.cons = List.copyOf(cons);
        this.benefits = List.copyOf(benefits);
    }
    
    public static ApprovalDecision create(
            boolean approved,
            String rationale,
            double riskScore,
            List<String> pros,
            List<String> cons,
            List<String> benefits) {
        return new ApprovalDecision(approved, rationale, riskScore, pros, cons, benefits);
    }
    
    public boolean isApproved() {
        return approved;
    }
    
    public String getRationale() {
        return rationale;
    }
    
    public double getRiskScore() {
        return riskScore;
    }
    
    public List<String> getPros() {
        return pros;
    }
    
    public List<String> getCons() {
        return cons;
    }
    
    public List<String> getBenefits() {
        return benefits;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ApprovalDecision)) return false;
        ApprovalDecision that = (ApprovalDecision) o;
        return approved == that.approved &&
                Double.compare(that.riskScore, riskScore) == 0 &&
                rationale.equals(that.rationale) &&
                pros.equals(that.pros) &&
                cons.equals(that.cons) &&
                benefits.equals(that.benefits);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(approved, rationale, riskScore, pros, cons, benefits);
    }
    
    @Override
    public String toString() {
        return "ApprovalDecision{" +
                "approved=" + approved +
                ", riskScore=" + riskScore +
                ", rationale='" + rationale + '\'' +
                ", pros=" + pros +
                ", cons=" + cons +
                ", benefits=" + benefits +
                '}';
    }
}
