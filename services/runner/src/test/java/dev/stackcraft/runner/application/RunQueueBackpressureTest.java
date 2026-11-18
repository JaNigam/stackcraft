package dev.stackcraft.runner.application;

import dev.stackcraft.runner.domain.Limits;
import dev.stackcraft.runner.domain.RunRequest;
import dev.stackcraft.runner.domain.RunResult;
import dev.stackcraft.runner.domain.RunStatus;
import dev.stackcraft.runner.port.Compiler;
import dev.stackcraft.runner.port.Sandbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Uses fake ports (no javac, no JVM): this test is about the queue, not about Java. */
class RunQueueBackpressureTest {

    @TempDir
    Path temp;

    @Test
    @Timeout(10)
    void rejectsWhenWorkersAndQueueAreFull() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        Compiler ok = (dir, name, src) -> Compiler.CompileOutcome.ok();
        Sandbox blocking = (dir, name, stdin, limits) -> {
            await(release);
            return new RunResult(RunStatus.OK, "done", "", 0, 1, false);
        };
        // 1 worker + 1 waiting slot = room for exactly 2 runs.
        try (RunCodeUseCase useCase = new RunCodeUseCase(ok, blocking,
                new Limits(Duration.ofSeconds(5), 64, 1024), 1024, 1, 1, temp)) {
            RunRequest req = new RunRequest("class Main {}", null);

            CompletableFuture<RunResult> first = CompletableFuture.supplyAsync(() -> useCase.run(req));
            waitUntil(() -> useCase.activeRuns() == 1);
            CompletableFuture<RunResult> second = CompletableFuture.supplyAsync(() -> useCase.run(req));
            waitUntil(() -> useCase.queuedRuns() == 1);

            assertThatThrownBy(() -> useCase.run(req)).isInstanceOf(RunQueueFullException.class);

            release.countDown();
            assertThat(first.get(5, TimeUnit.SECONDS).stdout()).isEqualTo("done");
            assertThat(second.get(5, TimeUnit.SECONDS).stdout()).isEqualTo("done");
        }
    }

    @Test
    void rejectsOversizedSourceBeforeQueueing() {
        try (RunCodeUseCase useCase = new RunCodeUseCase((d, n, s) -> Compiler.CompileOutcome.ok(),
                (d, n, i, l) -> null, new Limits(Duration.ofSeconds(1), 64, 1024), 10, 1, 1, temp)) {
            assertThatThrownBy(() -> useCase.run(new RunRequest("class Main { /* way too long */ }", null)))
                    .isInstanceOf(SourceTooLargeException.class);
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void waitUntil(java.util.function.BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) throw new AssertionError("condition not reached");
            Thread.sleep(10);
        }
    }
}
