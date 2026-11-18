package dev.stackcraft.runner.application;

/** Input size limit. Mapped to HTTP 413. */
public class SourceTooLargeException extends RuntimeException {
    public SourceTooLargeException(int max) {
        super("Source exceeds " + max + " bytes");
    }
}
