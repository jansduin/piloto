package com.piloto.cdi.kernel.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.UUID;

public final class MemoryEntryID {
    private final String value;

    private MemoryEntryID(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("MemoryEntryID value cannot be null or blank");
        }
        this.value = value;
    }

    @JsonCreator
    public static MemoryEntryID of(@JsonProperty("value") String value) {
        return new MemoryEntryID(value);
    }

    public static MemoryEntryID generate() {
        return new MemoryEntryID(UUID.randomUUID().toString());
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
        MemoryEntryID that = (MemoryEntryID) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "MemoryEntryID{" + value + '}';
    }
}
