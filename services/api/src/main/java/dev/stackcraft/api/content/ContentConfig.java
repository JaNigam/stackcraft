package dev.stackcraft.api.content;

import dev.stackcraft.api.content.adapter.fs.FileSystemContentSource;
import dev.stackcraft.api.content.application.GetChapterUseCase;
import dev.stackcraft.api.content.application.ListSeriesUseCase;
import dev.stackcraft.api.content.port.ContentSource;
import dev.stackcraft.api.shared.StackcraftProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.time.Clock;

/** Composition root for the content feature: picks the file-system adapter for the {@link ContentSource} port. */
@Configuration(proxyBeanMethods = false)
public class ContentConfig {

    @Bean
    ContentSource contentSource(StackcraftProperties props, Clock clock) {
        return new FileSystemContentSource(Path.of(props.content().root()), props.content().reload(), clock);
    }

    @Bean
    ListSeriesUseCase listSeriesUseCase(ContentSource content) {
        return new ListSeriesUseCase(content);
    }

    @Bean
    GetChapterUseCase getChapterUseCase(ContentSource content) {
        return new GetChapterUseCase(content);
    }
}
