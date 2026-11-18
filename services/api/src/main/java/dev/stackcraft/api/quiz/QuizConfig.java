package dev.stackcraft.api.quiz;

import dev.stackcraft.api.content.port.ContentSource;
import dev.stackcraft.api.progress.application.AwardXpUseCase;
import dev.stackcraft.api.progress.application.TrapBoardUseCase;
import dev.stackcraft.api.quiz.application.CommitTeaserUseCase;
import dev.stackcraft.api.quiz.application.SubmitAnswerUseCase;
import dev.stackcraft.api.quiz.domain.Question;
import dev.stackcraft.api.quiz.grading.Grader;
import dev.stackcraft.api.quiz.grading.GraderRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration(proxyBeanMethods = false)
public class QuizConfig {

    /** Spring collects every {@link Grader} bean; the registry checks that each question type has exactly one. */
    @Bean
    GraderRegistry graderRegistry(List<Grader<? extends Question>> graders) {
        return new GraderRegistry(graders);
    }

    @Bean
    SubmitAnswerUseCase submitAnswerUseCase(ContentSource content, GraderRegistry graders, AwardXpUseCase awardXp,
                                            TrapBoardUseCase traps) {
        return new SubmitAnswerUseCase(content, graders, awardXp, traps);
    }

    @Bean
    CommitTeaserUseCase commitTeaserUseCase(ContentSource content, AwardXpUseCase awardXp, TrapBoardUseCase traps) {
        return new CommitTeaserUseCase(content, awardXp, traps);
    }
}
