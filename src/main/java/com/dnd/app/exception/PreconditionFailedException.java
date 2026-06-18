package com.dnd.app.exception;

/**
 * Thrown when an If-Match precondition fails: the client's ETag does not match the
 * current server state (concurrent edit). Maps to HTTP 412.
 */
public class PreconditionFailedException extends RuntimeException {
    public PreconditionFailedException(String message) {
        super(message);
    }
}
