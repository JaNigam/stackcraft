package dev.stackcraft.api.progress.adapter.web;

import dev.stackcraft.api.progress.application.AwardXpUseCase.XpAward;
import dev.stackcraft.api.progress.application.CompleteChapterUseCase;
import dev.stackcraft.api.progress.application.FlashcardsUseCase;
import dev.stackcraft.api.progress.application.FlashcardsUseCase.DueCard;
import dev.stackcraft.api.progress.application.GetProgressUseCase;
import dev.stackcraft.api.progress.application.TrapBoardUseCase;
import dev.stackcraft.api.progress.domain.TrapEntry;
import dev.stackcraft.api.shared.CurrentLearner;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ProgressController {

    private final CurrentLearner learner;
    private final GetProgressUseCase getProgress;
    private final CompleteChapterUseCase completeChapter;
    private final TrapBoardUseCase trapBoard;
    private final FlashcardsUseCase flashcards;

    public ProgressController(CurrentLearner learner, GetProgressUseCase getProgress, CompleteChapterUseCase completeChapter,
                              TrapBoardUseCase trapBoard, FlashcardsUseCase flashcards) {
        this.learner = learner;
        this.getProgress = getProgress;
        this.completeChapter = completeChapter;
        this.trapBoard = trapBoard;
        this.flashcards = flashcards;
    }

    @GetMapping("/progress")
    public ProgressView progress() {
        return ProgressView.of(getProgress.get(learner.id()));
    }

    @PostMapping("/chapters/{chapterId}/completion")
    public CompletionResponse complete(@PathVariable String chapterId, @Valid @RequestBody AttemptBody body) {
        XpAward award = completeChapter.complete(learner.id(), chapterId, body.attemptId());
        return new CompletionResponse(award.xpAwarded(), award.duplicate(), progress());
    }

    @GetMapping("/trap-board")
    public List<TrapView> traps() {
        return trapBoard.open(learner.id()).stream().map(TrapView::of).toList();
    }

    @GetMapping("/flashcards/due")
    public List<DueCard> dueFlashcards(@RequestParam(defaultValue = "5") int limit) {
        return flashcards.due(learner.id(), Math.max(1, Math.min(limit, 20)));
    }

    @PostMapping("/flashcards/reviews")
    public ReviewResponse review(@Valid @RequestBody ReviewBody body) {
        FlashcardsUseCase.Review r = flashcards.review(learner.id(), body.cardKey(), body.remembered(), body.attemptId());
        return new ReviewResponse(r.box(), r.dueOn(), r.xpAwarded(), r.duplicate(), progress());
    }

    public record AttemptBody(@NotBlank String attemptId) {}

    public record CompletionResponse(int xpAwarded, boolean duplicate, ProgressView progress) {}

    public record ReviewBody(@NotBlank String attemptId, @NotBlank String cardKey, @NotNull Boolean remembered) {}

    public record ReviewResponse(int box, LocalDate dueOn, int xpAwarded, boolean duplicate, ProgressView progress) {}

    public record TrapView(String chapterId, String itemId, String kind, String prompt, int timesFallen, Instant lastFallenAt) {
        static TrapView of(TrapEntry t) {
            return new TrapView(t.chapterId(), t.itemId(), t.kind(), t.prompt(), t.timesFallen(), t.lastFallenAt());
        }
    }
}
