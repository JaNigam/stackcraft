package dev.stackcraft.runner.application;

/** Backpressure signal: every worker is busy and the waiting queue is full. Mapped to HTTP 429. */
public class RunQueueFullException extends RuntimeException {
    public RunQueueFullException() {
        super("Runner is busy, try again shortly");
    }
}
