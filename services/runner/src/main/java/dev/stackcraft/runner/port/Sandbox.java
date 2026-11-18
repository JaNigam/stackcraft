package dev.stackcraft.runner.port;

import dev.stackcraft.runner.domain.Limits;
import dev.stackcraft.runner.domain.RunResult;

import java.nio.file.Path;

/**
 * Port: executes compiled classes under {@link Limits}. Today a local child process; before public hosting an
 * ephemeral container or gVisor sandbox (ADR 0003). The use case never knows which.
 */
public interface Sandbox {
    RunResult run(Path workDir, String className, String stdin, Limits limits);
}
