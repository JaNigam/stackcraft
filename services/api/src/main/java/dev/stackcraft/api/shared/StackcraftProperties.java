package dev.stackcraft.api.shared;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.List;

/**
 * 12-factor config. Every value has an env var: {@code STACKCRAFT_CONTENT_ROOT}, {@code STACKCRAFT_RUNNER_URL},
 * {@code STACKCRAFT_WEB_ORIGINS}, {@code STACKCRAFT_ZONE}...
 */
@ConfigurationProperties("stackcraft")
public record StackcraftProperties(
        @DefaultValue Content content,
        @DefaultValue Runner runner,
        @DefaultValue("http://localhost:5173") List<String> webOrigins,
        @DefaultValue("UTC") String zone) {

    public record Content(@DefaultValue("../../content") String root, @DefaultValue("false") boolean reload) {}

    public record Runner(@DefaultValue("http://127.0.0.1:8091") String url, @DefaultValue("15s") Duration readTimeout) {}
}
