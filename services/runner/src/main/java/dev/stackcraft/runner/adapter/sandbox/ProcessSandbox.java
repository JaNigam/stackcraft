package dev.stackcraft.runner.adapter.sandbox;

import dev.stackcraft.runner.domain.Limits;
import dev.stackcraft.runner.domain.RunResult;
import dev.stackcraft.runner.domain.RunStatus;
import dev.stackcraft.runner.port.Sandbox;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Adapter: runs the snippet in a child JVM with a wall clock, a heap cap, an output cap and an empty environment.
 *
 * <p>Honest limit (ADR 0003): a child process still shares the host's file system and network. That is fine for
 * local, single-learner use. Before public hosting this adapter is replaced by an ephemeral-container sandbox.
 */
public class ProcessSandbox implements Sandbox {

    private final String javaBinary;

    public ProcessSandbox(String javaBinary) {
        this.javaBinary = javaBinary;
    }

    /** The java binary of the JVM running this service, so child and parent always match. */
    public static String currentJava() {
        return ProcessHandle.current().info().command()
                .orElse(Path.of(System.getProperty("java.home"), "bin", "java").toString());
    }

    @Override
    public RunResult run(Path workDir, String className, String stdin, Limits limits) {
        List<String> cmd = new ArrayList<>(List.of(
                javaBinary,
                "-Xmx" + limits.maxHeapMb() + "m",
                "-Xss1m",
                "-XX:+UseSerialGC",          // one GC thread: small, predictable footprint
                "-XX:TieredStopAtLevel=1",   // C1 only: faster start for short snippets
                "-Xshare:auto",
                "-Dfile.encoding=UTF-8",
                "-Dstdout.encoding=UTF-8",
                "-Dstderr.encoding=UTF-8",
                "-cp", workDir.toString(),
                className));
        ProcessBuilder pb = new ProcessBuilder(cmd).directory(workDir.toFile());
        scrubEnvironment(pb.environment());

        long start = System.nanoTime();
        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not start child JVM", e);
        }

        AtomicBoolean overflow = new AtomicBoolean(false);
        CappedSink out = new CappedSink(limits.outputCapBytes(), overflow, process);
        CappedSink err = new CappedSink(limits.outputCapBytes(), overflow, process);
        Thread outPump = Thread.ofVirtual().start(() -> out.drain(process.getInputStream()));
        Thread errPump = Thread.ofVirtual().start(() -> err.drain(process.getErrorStream()));
        writeStdin(process, stdin);

        boolean finished;
        try {
            finished = process.waitFor(limits.wallClock().toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            finished = false;
        }
        if (!finished) kill(process);
        join(outPump);
        join(errPump);
        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        int exit = finished ? process.exitValue() : -1;
        RunStatus status;
        String stderr = err.text();
        if (!finished) {
            status = RunStatus.TIMEOUT;
            stderr = stderr + "\n[stackcraft] Killed: exceeded " + limits.wallClock().toSeconds() + " s wall-clock limit";
        } else if (overflow.get()) {
            status = RunStatus.OUTPUT_LIMIT;
            stderr = stderr + "\n[stackcraft] Killed: output exceeded " + limits.outputCapBytes() + " bytes";
        } else {
            status = exit == 0 ? RunStatus.OK : RunStatus.RUNTIME_ERROR;
        }
        return new RunResult(status, out.text(), stderr.strip(), exit, durationMs, overflow.get());
    }

    /** No inherited variables: the child must never see the parent's secrets. Windows needs SystemRoot to boot a JVM. */
    private static void scrubEnvironment(Map<String, String> env) {
        String systemRoot = env.get("SystemRoot");
        env.clear();
        if (systemRoot != null) env.put("SystemRoot", systemRoot);
    }

    private static void writeStdin(Process process, String stdin) {
        try (OutputStream in = process.getOutputStream()) {
            in.write(stdin.getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) {
            // the child may exit before reading stdin; that is not an error
        }
    }

    private static void kill(Process process) {
        process.descendants().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
        try {
            process.waitFor(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void join(Thread t) {
        try {
            t.join(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Keeps the first {@code cap} bytes; one byte more kills the child so a print loop cannot fill memory or disk. */
    static final class CappedSink {
        private final int cap;
        private final AtomicBoolean overflow;
        private final Process process;
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        CappedSink(int cap, AtomicBoolean overflow, Process process) {
            this.cap = cap;
            this.overflow = overflow;
            this.process = process;
        }

        void drain(InputStream in) {
            byte[] chunk = new byte[8192];
            try (in) {
                int n;
                while ((n = in.read(chunk)) != -1) {
                    int room = cap - buffer.size();
                    if (n > room) {
                        synchronized (this) { buffer.write(chunk, 0, Math.max(room, 0)); }
                        overflow.set(true);
                        kill(process);
                        return;
                    }
                    synchronized (this) { buffer.write(chunk, 0, n); }
                }
            } catch (IOException ignored) {
                // stream closes when the process is killed
            }
        }

        synchronized String text() {
            return buffer.toString(StandardCharsets.UTF_8);
        }
    }
}
