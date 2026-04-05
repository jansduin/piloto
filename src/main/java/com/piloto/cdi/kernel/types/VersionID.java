package com.piloto.cdi.kernel.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.UUID;

public final class VersionID {
    private final String value;

    private VersionID(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("VersionID value cannot be null or blank");
        }
        this.value = value;
    }

    @JsonCreator
    public static VersionID of(@JsonProperty("value") String value) {
        return new VersionID(value);
    }

    public static VersionID generate() {
        return new VersionID(UUID.randomUUID().toString());
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
        VersionID versionID = (VersionID) o;
        return Objects.equals(value, versionID.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "VersionID{" + value + '}';
    }
}
