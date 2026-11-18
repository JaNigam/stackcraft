package dev.stackcraft.api.execution.adapter;

import dev.stackcraft.api.execution.application.RunSnippetUseCase;
import dev.stackcraft.api.execution.port.CodeExecutor.ExecutionResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/runs")
public class RunController {

    private final RunSnippetUseCase runSnippet;

    public RunController(RunSnippetUseCase runSnippet) {
        this.runSnippet = runSnippet;
    }

    @PostMapping
    public ExecutionResult run(@Valid @RequestBody RunRequest body) {
        return runSnippet.run(body.source(), body.stdin());
    }

    @GetMapping("/status")
    public Map<String, Boolean> status() {
        return Map.of("available", runSnippet.runnerAvailable());
    }

    public record RunRequest(@NotBlank @Size(max = RunSnippetUseCase.MAX_SOURCE_BYTES) String source,
                             @Size(max = RunSnippetUseCase.MAX_STDIN_BYTES) String stdin) {}
}
