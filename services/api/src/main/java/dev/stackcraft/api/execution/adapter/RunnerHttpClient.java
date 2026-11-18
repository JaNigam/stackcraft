package dev.stackcraft.api.execution.adapter;

import dev.stackcraft.api.execution.port.CodeExecutor;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Adapter: calls the internal runner over HTTP, with two safety nets.
 * <ul>
 *   <li><b>Timeouts</b>: connect 1 s; read = runner wall clock + slack. A hung runner can never hang the api.</li>
 *   <li><b>Circuit breaker</b> (Resilience4j): after repeated failures the circuit opens and calls fail fast for
 *       a while instead of piling up on a dead runner; then a few trial calls decide whether to close it again.
 *       "Busy" (429) is not counted as a failure: the runner is healthy, just full.</li>
 * </ul>
 * Either way the caller gets {@link ExecutorUnavailableException} and the UI falls back to the stored output.
 */
public class RunnerHttpClient implements CodeExecutor {

    private final RestClient http;
    private final CircuitBreaker breaker;

    public RunnerHttpClient(String baseUrl, Duration readTimeout, CircuitBreakerConfig breakerConfig) {
        HttpClient jdk = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(jdk);
        factory.setReadTimeout(readTimeout);
        this.http = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        this.breaker = CircuitBreaker.of("runner", breakerConfig);
    }

    public static CircuitBreakerConfig defaultBreaker() {
        return CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(3)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(15))
                // Leave OPEN on a timer, not only on the next call, so /runs/status recovers on its own.
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .permittedNumberOfCallsInHalfOpenState(2)
                .ignoreExceptions(ExecutorBusyException.class)
                .build();
    }

    @Override
    public ExecutionResult execute(String source, String stdin) {
        try {
            return breaker.executeSupplier(() -> call(source, stdin));
        } catch (CallNotPermittedException open) {
            throw new ExecutorUnavailableException("The code runner is unavailable (circuit open)", open);
        }
    }

    private ExecutionResult call(String source, String stdin) {
        try {
            var request = http.post().uri("/internal/v1/runs").contentType(MediaType.APPLICATION_JSON)
                    .body(new RunBody(source, stdin));
            String requestId = MDC.get("requestId");
            if (requestId != null) request = request.header("X-Request-Id", requestId);
            ExecutionResult result = request.retrieve().body(ExecutionResult.class);
            if (result == null) throw new ExecutorUnavailableException("Empty response from runner", null);
            return result;
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS)) throw new ExecutorBusyException();
            throw new IllegalArgumentException("Runner rejected the snippet: " + e.getStatusText(), e);
        } catch (ResourceAccessException e) {
            throw new ExecutorUnavailableException("The code runner did not answer", e);
        } catch (RestClientException e) {
            throw new ExecutorUnavailableException("The code runner failed", e);
        }
    }

    @Override
    public boolean available() {
        return breaker.getState() != CircuitBreaker.State.OPEN && breaker.getState() != CircuitBreaker.State.FORCED_OPEN;
    }

    public CircuitBreaker.State circuitState() {
        return breaker.getState();
    }

    record RunBody(String source, String stdin) {}
}
