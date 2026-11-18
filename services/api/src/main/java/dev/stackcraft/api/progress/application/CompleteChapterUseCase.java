package dev.stackcraft.api.progress.application;

import dev.stackcraft.api.content.domain.Chapter;
import dev.stackcraft.api.content.port.ContentSource;
import dev.stackcraft.api.progress.application.AwardXpUseCase.XpAward;
import dev.stackcraft.api.progress.application.AwardXpUseCase.XpClaim;
import dev.stackcraft.api.progress.domain.XpKind;
import dev.stackcraft.api.shared.NotFoundException;

/** Marks a lesson done (course rule: 10 XP, once). */
public class CompleteChapterUseCase {

    private final ContentSource content;
    private final AwardXpUseCase awardXp;

    public CompleteChapterUseCase(ContentSource content, AwardXpUseCase awardXp) {
        this.content = content;
        this.awardXp = awardXp;
    }

    public XpAward complete(String learnerId, String chapterId, String attemptId) {
        Chapter chapter = content.chapter(chapterId).orElseThrow(() -> new NotFoundException("Chapter", chapterId));
        return awardXp.award(new XpClaim(attemptId, learnerId, XpKind.CHAPTER, chapter.id(), "complete",
                chapter.meta().completionXp(), 1.0));
    }
}
