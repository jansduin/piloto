package com.piloto.cdi.kernel.command;

import java.util.Objects;
import java.util.UUID;

/**
 * Strong-typed identifier for Command instances.
 * 
 * <p>Provides type safety and semantic clarity over raw strings. Immutable value object.</p>
 * 
 * @since Phase 3
 */
public final class CommandID {
    
    private final String id;
    
    private CommandID(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("CommandID cannot be null or empty");
        }
        this.id = id;
    }
    
    /**
     * Creates a CommandID from an existing string identifier.
     * 
     * @param id non-null, non-empty string
     * @return CommandID instance
     * @throws IllegalArgumentException if id is null or empty
     */
    public static CommandID of(String id) {
        return new CommandID(id);
    }
    
    /**
     * Generates a new random CommandID using UUID.
     * 
     * @return new CommandID with unique value
     */
    public static CommandID generate() {
        return new CommandID(UUID.randomUUID().toString());
    }
    
    /**
     * Returns the raw string identifier.
     * 
     * @return non-null string
     */
    public String getValue() {
        return id;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommandID commandID = (CommandID) o;
        return id.equals(commandID.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "CommandID{" + id + '}';
    }
}
