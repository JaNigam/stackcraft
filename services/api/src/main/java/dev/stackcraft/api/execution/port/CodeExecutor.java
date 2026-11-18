package dev.stackcraft.api.execution.port;

/**
 * Port: runs a Java snippet somewhere else. The api never runs untrusted code itself (ADR 0003).
 *
 * @throws ExecutorUnavailableException when the runner is down or the circuit is open
 * @throws ExecutorBusyException        when the runner's queue is full (backpressure)
 */
public interface CodeExecutor {

    ExecutionResult execute(String source, String stdin);

    /** True when calls are currently allowed through (circuit closed or half-open). */
    boolean available();

    record ExecutionResult(String status, String stdout, String stderr, int exitCode, long durationMs, boolean truncated) {}

    class ExecutorUnavailableException extends RuntimeException {
        public ExecutorUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    class ExecutorBusyException extends RuntimeException {
        public ExecutorBusyException() {
            super("The code runner is busy, try again in a few seconds");
        }
    }
}
