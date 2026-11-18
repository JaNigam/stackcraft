package dev.stackcraft.api.execution.application;

import dev.stackcraft.api.execution.port.CodeExecutor;
import dev.stackcraft.api.execution.port.CodeExecutor.ExecutionResult;

import java.nio.charset.StandardCharsets;

/** Validates input size at the edge (cheap) before spending a runner slot (expensive). */
public class RunSnippetUseCase {

    public static final int MAX_SOURCE_BYTES = 64 * 1024;
    public static final int MAX_STDIN_BYTES = 8 * 1024;

    private final CodeExecutor executor;

    public RunSnippetUseCase(CodeExecutor executor) {
        this.executor = executor;
    }

    public ExecutionResult run(String source, String stdin) {
        if (source == null || source.isBlank()) throw new IllegalArgumentException("source must not be blank");
        if (source.getBytes(StandardCharsets.UTF_8).length > MAX_SOURCE_BYTES) {
            throw new IllegalArgumentException("source exceeds " + MAX_SOURCE_BYTES + " bytes");
        }
        if (stdin != null && stdin.getBytes(StandardCharsets.UTF_8).length > MAX_STDIN_BYTES) {
            throw new IllegalArgumentException("stdin exceeds " + MAX_STDIN_BYTES + " bytes");
        }
        return executor.execute(source, stdin == null ? "" : stdin);
    }

    public boolean runnerAvailable() {
        return executor.available();
    }
}
