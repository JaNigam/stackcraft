package dev.stackcraft.runner;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/** Whole context with real adapters: proves the wiring, the JSON shape and the health probe. */
@SpringBootTest
@AutoConfigureMockMvc
class RunnerApplicationTest {

    @Autowired
    MockMvcTester mvc;

    @Test
    void runsHelloWorldEndToEnd() {
        assertThat(mvc.post().uri("/internal/v1/runs").contentType(MediaType.APPLICATION_JSON)
                .header("X-Request-Id", "test-123")
                .content("{\"source\":\"public class Hi { public static void main(String[] a) { System.out.print(7 * 6); } }\"}"))
                .hasStatusOk()
                .hasHeader("X-Request-Id", "test-123")
                .bodyJson().extractingPath("$.stdout").isEqualTo("42");
    }

    @Test
    void readinessProbeIsUp() {
        assertThat(mvc.get().uri("/actuator/health/readiness")).hasStatusOk();
    }
}
