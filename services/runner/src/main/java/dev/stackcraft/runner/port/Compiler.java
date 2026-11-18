package dev.stackcraft.runner.port;

import java.nio.file.Path;

/** Port: turns one source file into class files inside {@code workDir}. */
public interface Compiler {

    CompileOutcome compile(Path workDir, String className, String source);

    record CompileOutcome(boolean success, String diagnostics) {
        public static CompileOutcome ok() { return new CompileOutcome(true, ""); }
        public static CompileOutcome failed(String diagnostics) { return new CompileOutcome(false, diagnostics); }
    }
}
