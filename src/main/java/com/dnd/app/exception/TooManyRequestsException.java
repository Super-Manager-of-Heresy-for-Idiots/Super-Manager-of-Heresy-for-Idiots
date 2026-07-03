package com.dnd.app.exception;

/** Thrown when a per-user rate limit is exceeded; mapped to HTTP 429 by the global handler. */
public class TooManyRequestsException extends RuntimeException {
    public TooManyRequestsException(String message) {
        super(message);
    }
}
