package dev.stackcraft.api.execution;

import dev.stackcraft.api.execution.adapter.RunnerHttpClient;
import dev.stackcraft.api.execution.application.RunSnippetUseCase;
import dev.stackcraft.api.execution.port.CodeExecutor;
import dev.stackcraft.api.shared.StackcraftProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ExecutionConfig {

    @Bean
    CodeExecutor codeExecutor(StackcraftProperties props) {
        return new RunnerHttpClient(props.runner().url(), props.runner().readTimeout(), RunnerHttpClient.defaultBreaker());
    }

    @Bean
    RunSnippetUseCase runSnippetUseCase(CodeExecutor executor) {
        return new RunSnippetUseCase(executor);
    }
}
