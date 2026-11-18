package dev.stackcraft.api.quiz.application;

import dev.stackcraft.api.content.domain.Chapter;
import dev.stackcraft.api.content.port.ContentSource;
import dev.stackcraft.api.progress.application.AwardXpUseCase;
import dev.stackcraft.api.progress.application.AwardXpUseCase.XpAward;
import dev.stackcraft.api.progress.application.AwardXpUseCase.XpClaim;
import dev.stackcraft.api.progress.application.TrapBoardUseCase;
import dev.stackcraft.api.progress.domain.XpKind;
import dev.stackcraft.api.quiz.domain.Answer;
import dev.stackcraft.api.quiz.domain.GradeResult;
import dev.stackcraft.api.quiz.domain.Question;
import dev.stackcraft.api.quiz.grading.GraderRegistry;
import dev.stackcraft.api.shared.NotFoundException;

/**
 * Grade, award XP, update the Trap Board. Grading is pure, so a duplicate attempt is simply re-graded (same result)
 * while XP and the Trap Board are left alone.
 */
public class SubmitAnswerUseCase {

    private final ContentSource content;
    private final GraderRegistry graders;
    private final AwardXpUseCase awardXp;
    private final TrapBoardUseCase traps;

    public SubmitAnswerUseCase(ContentSource content, GraderRegistry graders, AwardXpUseCase awardXp, TrapBoardUseCase traps) {
        this.content = content;
        this.graders = graders;
        this.awardXp = awardXp;
        this.traps = traps;
    }

    public AnswerOutcome submit(String learnerId, String chapterId, String questionId, String attemptId, Answer answer) {
        Chapter chapter = content.chapter(chapterId).orElseThrow(() -> new NotFoundException("Chapter", chapterId));
        Question question = chapter.question(questionId).orElseThrow(() -> new NotFoundException("Question", questionId));

        GradeResult grade = graders.grade(question, answer);
        XpAward award = awardXp.award(new XpClaim(attemptId, learnerId, XpKind.ANSWER, chapterId, questionId,
                question.xp(), grade.score()));
        if (!award.duplicate()) {
            if (grade.correct()) traps.escaped(learnerId, chapterId, questionId);
            else if (grade.score() == 0.0) traps.fell(learnerId, chapterId, questionId, "question", question.prompt());
        }
        return new AnswerOutcome(grade, award.xpAwarded(), award.duplicate());
    }

    public record AnswerOutcome(GradeResult grade, int xpAwarded, boolean duplicate) {}
}
