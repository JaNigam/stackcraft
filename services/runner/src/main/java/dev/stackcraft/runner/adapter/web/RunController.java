package dev.stackcraft.runner.adapter.web;

import dev.stackcraft.runner.application.RunCodeUseCase;
import dev.stackcraft.runner.domain.RunRequest;
import dev.stackcraft.runner.domain.RunResult;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** HTTP adapter. Maps JSON to the use case and back, records a latency metric, and does nothing else (SRP). */
@RestController
@RequestMapping("/internal/v1")
public class RunController {

    private final RunCodeUseCase runCode;
    private final MeterRegistry meters;

    public RunController(RunCodeUseCase runCode, MeterRegistry meters) {
        this.runCode = runCode;
        this.meters = meters;
    }

    @PostMapping("/runs")
    public RunResult run(@Valid @RequestBody RunBody body) {
        Timer.Sample sample = Timer.start(meters);
        RunResult result = runCode.run(new RunRequest(body.source(), body.stdin()));
        sample.stop(meters.timer("stackcraft.runner.run", "status", result.status().name()));
        return result;
    }

    public record RunBody(@NotBlank String source, String stdin) {}
}
