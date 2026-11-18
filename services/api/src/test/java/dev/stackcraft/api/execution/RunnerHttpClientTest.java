package dev.stackcraft.api.execution;

import com.sun.net.httpserver.HttpServer;
import dev.stackcraft.api.execution.adapter.RunnerHttpClient;
import dev.stackcraft.api.execution.port.CodeExecutor.ExecutorBusyException;
import dev.stackcraft.api.execution.port.CodeExecutor.ExecutorUnavailableException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** A real HTTP stub (JDK HttpServer), so timeouts and connection failures are real too. */
class RunnerHttpClientTest {

    private HttpServer server;
    private final AtomicInteger status = new AtomicInteger(200);
    private final AtomicInteger delayMs = new AtomicInteger(0);
    private final AtomicInteger calls = new AtomicInteger();
    private final AtomicReference<String> seenRequestId = new AtomicReference<>();

    private String start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/internal/v1/runs", ex -> {
            calls.incrementAndGet();
            seenRequestId.set(ex.getRequestHeaders().getFirst("X-Request-Id"));
            try { Thread.sleep(delayMs.get()); } catch (InterruptedException ignored) { }
            byte[] body = ("{\"status\":\"OK\",\"stdout\":\"42\\n\",\"stderr\":\"\",\"exitCode\":0,\"durationMs\":5,\"truncated\":false}")
                    .getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(status.get(), body.length);
            ex.getResponseBody().write(body);
            ex.close();
        });
        server.start();
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stop() {
        if (server != null) server.stop(0);
        org.slf4j.MDC.clear();
    }

    @Test
    void returnsTheRunnerResultAndForwardsTheRequestId() throws IOException {
        var client = new RunnerHttpClient(start(), Duration.ofSeconds(2), RunnerHttpClient.defaultBreaker());
        org.slf4j.MDC.put("requestId", "req-777");

        var result = client.execute("class Main {}", "");

        assertThat(result.status()).isEqualTo("OK");
        assertThat(result.stdout()).isEqualTo("42\n");
        assertThat(seenRequestId.get()).isEqualTo("req-777");
    }

    @Test
    void runner429BecomesBusyAndDoesNotTripTheBreaker() throws IOException {
        var client = new RunnerHttpClient(start(), Duration.ofSeconds(2), RunnerHttpClient.defaultBreaker());
        status.set(429);

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> client.execute("class Main {}", "")).isInstanceOf(ExecutorBusyException.class);
        }
        assertThat(client.circuitState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    @Timeout(10)
    void slowRunnerTimesOutAsUnavailable() throws IOException {
        var client = new RunnerHttpClient(start(), Duration.ofMillis(300), RunnerHttpClient.defaultBreaker());
        delayMs.set(2000);

        assertThatThrownBy(() -> client.execute("class Main {}", "")).isInstanceOf(ExecutorUnavailableException.class);
    }

    @Test
    @Timeout(10)
    void deadRunnerOpensTheCircuitThenFailsFastWithoutCalling() throws IOException {
        String url = start();
        server.stop(0);
        server = null;
        var client = new RunnerHttpClient(url, Duration.ofSeconds(1), RunnerHttpClient.defaultBreaker());

        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> client.execute("class Main {}", "")).isInstanceOf(ExecutorUnavailableException.class);
        }
        assertThat(client.circuitState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(client.available()).isFalse();
        assertThatThrownBy(() -> client.execute("class Main {}", ""))
                .isInstanceOf(ExecutorUnavailableException.class).hasMessageContaining("circuit open");
    }

    @Test
    void server500CountsAsFailure() throws IOException {
        var client = new RunnerHttpClient(start(), Duration.ofSeconds(2), RunnerHttpClient.defaultBreaker());
        status.set(500);

        assertThatThrownBy(() -> client.execute("class Main {}", "")).isInstanceOf(ExecutorUnavailableException.class);
        assertThat(calls.get()).isEqualTo(1);
    }
}
