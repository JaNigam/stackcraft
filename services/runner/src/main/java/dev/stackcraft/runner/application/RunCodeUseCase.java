package dev.stackcraft.runner.application;

import dev.stackcraft.runner.domain.Limits;
import dev.stackcraft.runner.domain.MainClassName;
import dev.stackcraft.runner.domain.RunRequest;
import dev.stackcraft.runner.domain.RunResult;
import dev.stackcraft.runner.port.Compiler;
import dev.stackcraft.runner.port.Sandbox;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * The one thing this service does: compile and run a snippet, safely.
 *
 * <p>Concurrency is bounded on purpose: {@code maxConcurrent} workers plus a waiting queue of
 * {@code queueCapacity}. When both are full we reject at once ({@link RunQueueFullException}, HTTP 429)
 * instead of piling up threads and child JVMs until the machine falls over. That is backpressure.
 *
 * <p>Depends only on ports ({@link Compiler}, {@link Sandbox}), never on javac or ProcessBuilder (DIP).
 */
public class RunCodeUseCase implements AutoCloseable {

    private final Compiler compiler;
    private final Sandbox sandbox;
    private final Limits limits;
    private final int maxSourceBytes;
    private final Path tempRoot;
    private final ThreadPoolExecutor workers;

    public RunCodeUseCase(Compiler compiler, Sandbox sandbox, Limits limits, int maxSourceBytes,
                          int maxConcurrent, int queueCapacity, Path tempRoot) {
        this.compiler = compiler;
        this.sandbox = sandbox;
        this.limits = limits;
        this.maxSourceBytes = maxSourceBytes;
        this.tempRoot = tempRoot;
        // ArrayBlockingQueue(0) is illegal; a SynchronousQueue means "no waiting room, only free workers".
        BlockingQueue<Runnable> waiting = queueCapacity == 0 ? new SynchronousQueue<>() : new ArrayBlockingQueue<>(queueCapacity);
        this.workers = new ThreadPoolExecutor(maxConcurrent, maxConcurrent, 0, TimeUnit.MILLISECONDS, waiting,
                Thread.ofPlatform().name("run-worker-", 0).daemon(true).factory(),
                new ThreadPoolExecutor.AbortPolicy());
    }

    public RunResult run(RunRequest request) {
        if (request.source().getBytes(StandardCharsets.UTF_8).length > maxSourceBytes) {
            throw new SourceTooLargeException(maxSourceBytes);
        }
        Future<RunResult> future;
        try {
            future = workers.submit(() -> execute(request));
        } catch (RejectedExecutionException e) {
            throw new RunQueueFullException();
        }
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            future.cancel(true);
            throw new IllegalStateException("Interrupted while waiting for run", e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException re) throw re;
            throw new IllegalStateException(e.getCause());
        }
    }

    private RunResult execute(RunRequest request) {
        long start = System.nanoTime();
        Path workDir = createWorkDir();
        try {
            String className = MainClassName.of(request.source());
            Compiler.CompileOutcome compiled = compiler.compile(workDir, className, request.source());
            if (!compiled.success()) {
                return RunResult.compileError(cap(compiled.diagnostics()), elapsedMs(start));
            }
            return sandbox.run(workDir, className, request.stdin(), limits);
        } finally {
            deleteRecursively(workDir);
        }
    }

    private String cap(String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        return bytes.length <= limits.outputCapBytes() ? s : new String(bytes, 0, limits.outputCapBytes(), StandardCharsets.UTF_8);
    }

    private Path createWorkDir() {
        try {
            Files.createDirectories(tempRoot);
            return Files.createTempDirectory(tempRoot, "run-");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** The per-run folder is always removed, success or failure, so runs never see each other's files. */
    static void deleteRecursively(Path dir) {
        if (!Files.exists(dir)) return;
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // best effort: a locked file on Windows must not fail the run
                }
            });
        } catch (IOException ignored) {
            // best effort
        }
    }

    private static long elapsedMs(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }

    public int activeRuns() { return workers.getActiveCount(); }

    public int queuedRuns() { return workers.getQueue().size(); }

    @Override
    public void close() {
        workers.shutdownNow();
    }
}
