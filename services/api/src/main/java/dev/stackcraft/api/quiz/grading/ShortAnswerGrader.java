package dev.stackcraft.api.quiz.grading;

import dev.stackcraft.api.quiz.domain.Answer;
import dev.stackcraft.api.quiz.domain.GradeResult;
import dev.stackcraft.api.quiz.domain.Question.ShortAnswer;
import org.springframework.stereotype.Component;

/** Self-graded: the learner compares with the model answer and scores 0 (missed), 1 (partly) or 2 (nailed it). */
@Component
public class ShortAnswerGrader implements Grader<ShortAnswer> {

    @Override
    public Class<ShortAnswer> supports() {
        return ShortAnswer.class;
    }

    @Override
    public GradeResult grade(ShortAnswer q, Answer a) {
        int self = SelfScore.clamp(a.selfScore());
        return new GradeResult(self == 2, self / 2.0, SelfScore.feedback(self), q.modelAnswer());
    }
}
