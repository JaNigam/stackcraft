package dev.stackcraft.api.quiz.adapter;

import dev.stackcraft.api.progress.adapter.web.ProgressView;
import dev.stackcraft.api.progress.application.GetProgressUseCase;
import dev.stackcraft.api.quiz.application.CommitTeaserUseCase;
import dev.stackcraft.api.quiz.application.CommitTeaserUseCase.TeaserOutcome;
import dev.stackcraft.api.quiz.application.SubmitAnswerUseCase;
import dev.stackcraft.api.quiz.application.SubmitAnswerUseCase.AnswerOutcome;
import dev.stackcraft.api.quiz.domain.Answer;
import dev.stackcraft.api.shared.CurrentLearner;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chapters/{chapterId}")
public class QuizController {

    private final CurrentLearner learner;
    private final SubmitAnswerUseCase submitAnswer;
    private final CommitTeaserUseCase commitTeaser;
    private final GetProgressUseCase getProgress;

    public QuizController(CurrentLearner learner, SubmitAnswerUseCase submitAnswer, CommitTeaserUseCase commitTeaser,
                          GetProgressUseCase getProgress) {
        this.learner = learner;
        this.submitAnswer = submitAnswer;
        this.commitTeaser = commitTeaser;
        this.getProgress = getProgress;
    }

    @PostMapping("/questions/{questionId}/answers")
    public AnswerResponse answer(@PathVariable String chapterId, @PathVariable String questionId,
                                 @Valid @RequestBody AnswerBody body) {
        AnswerOutcome o = submitAnswer.submit(learner.id(), chapterId, questionId, body.attemptId(),
                new Answer(body.choice(), body.text(), body.selfScore(), body.hintsUsed()));
        return new AnswerResponse(o.grade().correct(), o.grade().score(), o.grade().feedback(), o.grade().explanation(),
                o.xpAwarded(), o.duplicate(), ProgressView.of(getProgress.get(learner.id())));
    }

    @PostMapping("/teasers/{teaserId}/commits")
    public TeaserResponse commit(@PathVariable String chapterId, @PathVariable String teaserId,
                                 @Valid @RequestBody CommitBody body) {
        TeaserOutcome o = commitTeaser.commit(learner.id(), chapterId, teaserId, body.attemptId(), body.choice());
        return new TeaserResponse(o.correct(), o.answer(), o.reveal(), o.xpAwarded(), o.duplicate(),
                ProgressView.of(getProgress.get(learner.id())));
    }

    public record AnswerBody(@NotBlank String attemptId, Integer choice, @Size(max = 8192) String text,
                             Integer selfScore, Integer hintsUsed) {}

    public record AnswerResponse(boolean correct, double score, String feedback, String explanation, int xpAwarded,
                                 boolean duplicate, ProgressView progress) {}

    public record CommitBody(@NotBlank String attemptId, @NotNull Integer choice) {}

    public record TeaserResponse(boolean correct, int answer, String reveal, int xpAwarded, boolean duplicate,
                                 ProgressView progress) {}
}
