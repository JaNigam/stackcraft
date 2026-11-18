package dev.stackcraft.runner.adapter.web;

import dev.stackcraft.runner.application.RunCodeUseCase;
import dev.stackcraft.runner.application.RunQueueFullException;
import dev.stackcraft.runner.domain.RunResult;
import dev.stackcraft.runner.domain.RunStatus;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@WebMvcTest(RunController.class)
class RunControllerTest {

    @Autowired
    MockMvcTester mvc;

    @MockitoBean
    RunCodeUseCase runCode;

    @TestConfiguration
    static class Meters {
        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }

    @Test
    void returnsTheRunResult() {
        given(runCode.run(any())).willReturn(new RunResult(RunStatus.OK, "hi\n", "", 0, 42, false));

        assertThat(mvc.post().uri("/internal/v1/runs").contentType(MediaType.APPLICATION_JSON)
                .content("{\"source\":\"class Main {}\"}"))
                .hasStatusOk()
                .bodyJson().extractingPath("$.status").isEqualTo("OK");
    }

    @Test
    void fullQueueIs429WithRetryAfter() {
        given(runCode.run(any())).willThrow(new RunQueueFullException());

        assertThat(mvc.post().uri("/internal/v1/runs").contentType(MediaType.APPLICATION_JSON)
                .content("{\"source\":\"class Main {}\"}"))
                .hasStatus(429)
                .hasHeader("Retry-After", "2")
                .bodyJson().extractingPath("$.title").isEqualTo("Runner busy");
    }

    @Test
    void blankSourceIs400() {
        assertThat(mvc.post().uri("/internal/v1/runs").contentType(MediaType.APPLICATION_JSON)
                .content("{\"source\":\"  \"}"))
                .hasStatus(400);
    }
}
