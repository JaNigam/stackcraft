package dev.stackcraft.api.progress;

import dev.stackcraft.api.content.port.ContentSource;
import dev.stackcraft.api.progress.application.AwardXpUseCase;
import dev.stackcraft.api.progress.application.CompleteChapterUseCase;
import dev.stackcraft.api.progress.application.FlashcardsUseCase;
import dev.stackcraft.api.progress.application.GetProgressUseCase;
import dev.stackcraft.api.progress.application.ImportGuestProgressUseCase;
import dev.stackcraft.api.progress.application.TrapBoardUseCase;
import dev.stackcraft.api.progress.port.ProgressStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
public class ProgressConfig {

    @Bean
    AwardXpUseCase awardXpUseCase(ProgressStore store, Clock clock) {
        return new AwardXpUseCase(store, store, clock);
    }

    @Bean
    TrapBoardUseCase trapBoardUseCase(ProgressStore store, Clock clock) {
        return new TrapBoardUseCase(store, store, clock);
    }

    @Bean
    GetProgressUseCase getProgressUseCase(ProgressStore store, Clock clock) {
        return new GetProgressUseCase(store, clock);
    }

    @Bean
    CompleteChapterUseCase completeChapterUseCase(ContentSource content, AwardXpUseCase awardXp) {
        return new CompleteChapterUseCase(content, awardXp);
    }

    @Bean
    ImportGuestProgressUseCase importGuestProgressUseCase(ContentSource content, AwardXpUseCase awardXp, ProgressStore store, Clock clock) {
        return new ImportGuestProgressUseCase(content, awardXp, store, store, clock);
    }

    @Bean
    FlashcardsUseCase flashcardsUseCase(ContentSource content, ProgressStore store, AwardXpUseCase awardXp, Clock clock) {
        return new FlashcardsUseCase(content, store, store, awardXp, clock);
    }
}
