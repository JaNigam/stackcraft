package dev.stackcraft.api.progress.port;

/** The store already has this attemptId: the unique constraint won a race between two identical requests. */
public class DuplicateAttemptException extends RuntimeException {
    public DuplicateAttemptException(String attemptId) {
        super("Attempt " + attemptId + " already recorded");
    }
}
