package dev.stackcraft.runner.config;

import dev.stackcraft.runner.adapter.compiler.JavacCompiler;
import dev.stackcraft.runner.adapter.sandbox.ProcessSandbox;
import dev.stackcraft.runner.application.RunCodeUseCase;
import dev.stackcraft.runner.domain.Limits;
import dev.stackcraft.runner.port.Compiler;
import dev.stackcraft.runner.port.Sandbox;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

/** Composition root: the only place that knows which adapter plugs into which port. */
@Configuration(proxyBeanMethods = false)
public class RunnerConfig {

    @Bean
    Compiler compiler() {
        return new JavacCompiler();
    }

    @Bean
    Sandbox sandbox(RunnerProperties props) {
        return new ProcessSandbox(props.javaBinary().isBlank() ? ProcessSandbox.currentJava() : props.javaBinary());
    }

    @Bean(destroyMethod = "close")
    RunCodeUseCase runCodeUseCase(Compiler compiler, Sandbox sandbox, RunnerProperties props) {
        Path tempRoot = props.tempDir().isBlank()
                ? Path.of(System.getProperty("java.io.tmpdir"), "stackcraft-runner")
                : Path.of(props.tempDir());
        Limits limits = new Limits(props.timeout(), props.maxHeapMb(), (int) props.outputCap().toBytes());
        return new RunCodeUseCase(compiler, sandbox, limits, (int) props.maxSource().toBytes(),
                props.maxConcurrent(), props.queueCapacity(), tempRoot);
    }
}
