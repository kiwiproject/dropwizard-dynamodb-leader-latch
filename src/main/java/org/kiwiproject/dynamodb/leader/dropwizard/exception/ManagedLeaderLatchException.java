package org.kiwiproject.dynamodb.leader.dropwizard.exception;

/**
 * Exception that is thrown when a managed leader latch cannot be started.
 * <p>
 * This is the only exception this library throws. Problems after startup, such as DynamoDB being unavailable,
 * are reported as values by the underlying leader latch.
 */
public class ManagedLeaderLatchException extends RuntimeException {

    public ManagedLeaderLatchException() {
    }

    public ManagedLeaderLatchException(String message) {
        super(message);
    }

    public ManagedLeaderLatchException(String message, Throwable cause) {
        super(message, cause);
    }

    public ManagedLeaderLatchException(Throwable cause) {
        super(cause);
    }
}
