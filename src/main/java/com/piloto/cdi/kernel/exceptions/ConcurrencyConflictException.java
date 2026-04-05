package com.piloto.cdi.kernel.exceptions;

/**
 * Exception thrown when a state persistence conflict is detected
 * during an Optimistic Concurrency Control (OCC) check.
 */
public class ConcurrencyConflictException extends RuntimeException {

    public ConcurrencyConflictException(String message) {
        super(message);
    }

    public ConcurrencyConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
