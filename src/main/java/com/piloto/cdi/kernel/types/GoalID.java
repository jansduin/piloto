package com.piloto.cdi.kernel.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.UUID;

public final class GoalID {
    private final String value;

    private GoalID(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("GoalID value cannot be null or blank");
        }
        this.value = value;
    }

    @JsonCreator
    public static GoalID of(@JsonProperty("value") String value) {
        return new GoalID(value);
    }

    public static GoalID generate() {
        return new GoalID(UUID.randomUUID().toString());
    }

    public String getValue() {
        return value;
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        GoalID goalID = (GoalID) o;
        return Objects.equals(value, goalID.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "GoalID{" + value + '}';
    }
}
