package dev.stackcraft.runner.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;

import java.time.Duration;

/**
 * 12-factor config: every limit can be overridden by an environment variable, e.g. {@code RUNNER_TIMEOUT=5s},
 * {@code RUNNER_MAX_CONCURRENT=4}. Defaults are the documented limits from ADR 0003.
 */
@ConfigurationProperties("runner")
public record RunnerProperties(
        @DefaultValue("10s") Duration timeout,
        @DefaultValue("128") int maxHeapMb,
        @DefaultValue("64KB") DataSize outputCap,
        @DefaultValue("64KB") DataSize maxSource,
        @DefaultValue("2") int maxConcurrent,
        @DefaultValue("8") int queueCapacity,
        @DefaultValue("") String tempDir,
        @DefaultValue("") String javaBinary) {
}
