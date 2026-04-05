package com.piloto.cdi.kernel.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.UUID;

public final class EventID {
    private final String value;

    private EventID(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("EventID value cannot be null or blank");
        }
        this.value = value;
    }

    @JsonCreator
    public static EventID of(@JsonProperty("value") String value) {
        return new EventID(value);
    }

    public static EventID generate() {
        return new EventID(UUID.randomUUID().toString());
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
        EventID eventID = (EventID) o;
        return Objects.equals(value, eventID.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "EventID{" + value + '}';
    }
}
