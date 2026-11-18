package dev.stackcraft.runner.application;

import dev.stackcraft.runner.adapter.compiler.JavacCompiler;
import dev.stackcraft.runner.adapter.sandbox.ProcessSandbox;
import dev.stackcraft.runner.domain.Limits;
import dev.stackcraft.runner.domain.RunRequest;
import dev.stackcraft.runner.domain.RunResult;
import dev.stackcraft.runner.domain.RunStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/** Real javac + real child JVM. Short limits so the suite stays fast. */
class RunCodeUseCaseTest {

    @TempDir
    Path temp;

    private RunCodeUseCase useCase;

    @BeforeEach
    void setUp() {
        Limits limits = new Limits(Duration.ofSeconds(3), 64, 4096);
        useCase = new RunCodeUseCase(new JavacCompiler(), new ProcessSandbox(ProcessSandbox.currentJava()),
                limits, 64 * 1024, 2, 2, temp);
    }

    @AfterEach
    void tearDown() {
        useCase.close();
    }

    @Test
    @Timeout(30)
    void helloWorldRuns() {
        RunResult r = useCase.run(new RunRequest("""
                public class Hello {
                    public static void main(String[] args) {
                        System.out.println("Hello, Stackcraft");
                    }
                }
                """, null));

        assertThat(r.status()).isEqualTo(RunStatus.OK);
        assertThat(r.stdout()).isEqualToNormalizingNewlines("Hello, Stackcraft\n");
        assertThat(r.exitCode()).isZero();
        assertThat(r.truncated()).isFalse();
    }

    @Test
    @Timeout(30)
    void stdinIsPassedThrough() {
        RunResult r = useCase.run(new RunRequest("""
                import java.util.Scanner;
                public class Echo {
                    public static void main(String[] args) {
                        System.out.println(new Scanner(System.in).nextLine().toUpperCase());
                    }
                }
                """, "apple"));

        assertThat(r.stdout().strip()).isEqualTo("APPLE");
    }

    @Test
    @Timeout(30)
    void infiniteLoopIsKilledAtTheWallClock() {
        RunResult r = useCase.run(new RunRequest("""
                public class Spin {
                    public static void main(String[] args) {
                        while (true) { }
                    }
                }
                """, null));

        assertThat(r.status()).isEqualTo(RunStatus.TIMEOUT);
        assertThat(r.durationMs()).isBetween(3000L, 9000L);
        assertThat(r.stderr()).contains("wall-clock limit");
    }

    @Test
    @Timeout(30)
    void compileErrorIsReportedWithoutRunning() {
        RunResult r = useCase.run(new RunRequest("""
                public class Broken {
                    public static void main(String[] args) {
                        int x = "not a number";
                    }
                }
                """, null));

        assertThat(r.status()).isEqualTo(RunStatus.COMPILE_ERROR);
        assertThat(r.stderr()).contains("Broken.java:3: error:").contains("incompatible types");
        assertThat(r.stderr()).doesNotContain(temp.toString());
    }

    @Test
    @Timeout(30)
    void runtimeExceptionIsARuntimeError() {
        RunResult r = useCase.run(new RunRequest("""
                public class Boom {
                    public static void main(String[] args) {
                        Object o = null;
                        o.hashCode();
                    }
                }
                """, null));

        assertThat(r.status()).isEqualTo(RunStatus.RUNTIME_ERROR);
        assertThat(r.stderr()).contains("NullPointerException");
        assertThat(r.exitCode()).isNotZero();
    }

    @Test
    @Timeout(30)
    void printLoopIsKilledAtTheOutputCap() {
        RunResult r = useCase.run(new RunRequest("""
                public class Flood {
                    public static void main(String[] args) {
                        while (true) System.out.println("spam spam spam spam");
                    }
                }
                """, null));

        assertThat(r.status()).isEqualTo(RunStatus.OUTPUT_LIMIT);
        assertThat(r.truncated()).isTrue();
        assertThat(r.stdout().length()).isLessThanOrEqualTo(4096);
    }

    @Test
    @Timeout(30)
    void heapLimitIsEnforced() {
        RunResult r = useCase.run(new RunRequest("""
                public class Hog {
                    public static void main(String[] args) {
                        byte[] big = new byte[200 * 1024 * 1024];
                        System.out.println(big.length);
                    }
                }
                """, null));

        assertThat(r.status()).isEqualTo(RunStatus.RUNTIME_ERROR);
        assertThat(r.stderr()).contains("OutOfMemoryError");
    }

    @Test
    @Timeout(30)
    void perRunTempFoldersAreDeleted() throws IOException {
        useCase.run(new RunRequest("public class A { public static void main(String[] a) { } }", null));
        useCase.run(new RunRequest("public class B { public static void main(String[] a) { int x = ; } }", null));

        try (Stream<Path> left = Files.list(temp)) {
            assertThat(left).isEmpty();
        }
    }
}
