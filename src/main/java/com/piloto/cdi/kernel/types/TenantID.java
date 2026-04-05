package com.piloto.cdi.kernel.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.UUID;

public final class TenantID {
    private final String value;

    private TenantID(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("TenantID value cannot be null or blank");
        }
        this.value = value;
    }

    @JsonCreator
    public static TenantID of(@JsonProperty("value") String value) {
        return new TenantID(value);
    }

    public static TenantID generate() {
        return new TenantID(UUID.randomUUID().toString());
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
        TenantID tenantID = (TenantID) o;
        return Objects.equals(value, tenantID.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "TenantID{" + value + '}';
    }
}
