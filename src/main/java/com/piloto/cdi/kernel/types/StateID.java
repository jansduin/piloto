package com.piloto.cdi.kernel.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.UUID;

public final class StateID {
    private final String value;

    private StateID(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("StateID value cannot be null or blank");
        }
        this.value = value;
    }

    @JsonCreator
    public static StateID of(@JsonProperty("value") String value) {
        return new StateID(value);
    }

    public static StateID generate() {
        return new StateID(UUID.randomUUID().toString());
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
        StateID stateID = (StateID) o;
        return Objects.equals(value, stateID.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "StateID{" + value + '}';
    }
}
