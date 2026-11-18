package dev.stackcraft.api.quiz.grading;

import dev.stackcraft.api.quiz.domain.Answer;
import dev.stackcraft.api.quiz.domain.GradeResult;
import dev.stackcraft.api.quiz.domain.Question.Problem;
import org.springframework.stereotype.Component;

/** Self-graded like a short answer, but each hint used takes 20% off (floor 40%), so hints are a real trade-off. */
@Component
public class ProblemGrader implements Grader<Problem> {

    @Override
    public Class<Problem> supports() {
        return Problem.class;
    }

    @Override
    public GradeResult grade(Problem q, Answer a) {
        int self = SelfScore.clamp(a.selfScore());
        int hints = a.hintsUsed() == null ? 0 : Math.max(0, Math.min(a.hintsUsed(), q.hints().size()));
        double hintFactor = Math.max(0.4, 1.0 - 0.2 * hints);
        double score = (self / 2.0) * hintFactor;
        String feedback = SelfScore.feedback(self) + (hints > 0 ? " (" + hints + " hint" + (hints > 1 ? "s" : "") + " used)" : "");
        return new GradeResult(self == 2, score, feedback, q.modelAnswer());
    }
}
