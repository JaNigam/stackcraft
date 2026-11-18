package dev.stackcraft.runner.domain;

/** Result of one run. {@code truncated} is true when stdout or stderr hit the output cap. */
public record RunResult(RunStatus status, String stdout, String stderr, int exitCode, long durationMs, boolean truncated) {

    public static RunResult compileError(String diagnostics, long durationMs) {
        return new RunResult(RunStatus.COMPILE_ERROR, "", diagnostics, -1, durationMs, false);
    }
}
