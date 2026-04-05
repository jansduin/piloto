package com.piloto.cdi.kernel.interfaces;

import com.piloto.cdi.kernel.types.SeverityLevel;

import java.util.Map;
import java.util.Objects;

public final class EvaluationResult {
    private final boolean success;
    private final String message;
    private final SeverityLevel severity;
    private final Map<String, String> details;

    private EvaluationResult(
            boolean success,
            String message,
            SeverityLevel severity,
            Map<String, String> details) {
        
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message cannot be null or blank");
        }
        if (severity == null) {
            throw new IllegalArgumentException("severity cannot be null");
        }
        if (details == null) {
            throw new IllegalArgumentException("details cannot be null");
        }

        this.success = success;
        this.message = message;
        this.severity = severity;
        this.details = Map.copyOf(details);
    }

    public static EvaluationResult create(
            boolean success,
            String message,
            SeverityLevel severity,
            Map<String, String> details) {
        
        return new EvaluationResult(success, message, severity, details);
    }

    public boolean success() {
        return success;
    }

    public String message() {
        return message;
    }

    public SeverityLevel severity() {
        return severity;
    }

    public Map<String, String> details() {
        return details;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EvaluationResult that = (EvaluationResult) o;
        return success == that.success &&
                Objects.equals(message, that.message) &&
                severity == that.severity &&
                Objects.equals(details, that.details);
    }

    @Override
    public int hashCode() {
        return Objects.hash(success, message, severity, details);
    }

    @Override
    public String toString() {
        return "EvaluationResult{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", severity=" + severity +
                ", details=" + details +
                '}';
    }
}
