package dev.stackcraft.api.quiz.application;

import dev.stackcraft.api.content.domain.Chapter;
import dev.stackcraft.api.content.domain.Teaser;
import dev.stackcraft.api.content.port.ContentSource;
import dev.stackcraft.api.progress.application.AwardXpUseCase;
import dev.stackcraft.api.progress.application.AwardXpUseCase.XpAward;
import dev.stackcraft.api.progress.application.AwardXpUseCase.XpClaim;
import dev.stackcraft.api.progress.application.TrapBoardUseCase;
import dev.stackcraft.api.progress.domain.XpKind;
import dev.stackcraft.api.shared.NotFoundException;

/** "Commit before you peek": the learner locks in a guess, then sees the reveal. Wrong guesses become traps. */
public class CommitTeaserUseCase {

    private final ContentSource content;
    private final AwardXpUseCase awardXp;
    private final TrapBoardUseCase traps;

    public CommitTeaserUseCase(ContentSource content, AwardXpUseCase awardXp, TrapBoardUseCase traps) {
        this.content = content;
        this.awardXp = awardXp;
        this.traps = traps;
    }

    public TeaserOutcome commit(String learnerId, String chapterId, String teaserId, String attemptId, int choice) {
        Chapter chapter = content.chapter(chapterId).orElseThrow(() -> new NotFoundException("Chapter", chapterId));
        Teaser teaser = chapter.teaser(teaserId).orElseThrow(() -> new NotFoundException("Teaser", teaserId));
        if (choice < 0 || choice >= teaser.options().size()) {
            throw new IllegalArgumentException("choice must be between 0 and " + (teaser.options().size() - 1));
        }
        boolean correct = teaser.isCorrect(choice);
        XpAward award = awardXp.award(new XpClaim(attemptId, learnerId, XpKind.TEASER, chapterId, teaserId,
                teaser.xp(), correct ? 1.0 : 0.0));
        if (!award.duplicate()) {
            if (correct) traps.escaped(learnerId, chapterId, teaserId);
            else traps.fell(learnerId, chapterId, teaserId, "teaser", teaser.prompt());
        }
        return new TeaserOutcome(correct, teaser.answer(), teaser.reveal(), award.xpAwarded(), award.duplicate());
    }

    public record TeaserOutcome(boolean correct, int answer, String reveal, int xpAwarded, boolean duplicate) {}
}
