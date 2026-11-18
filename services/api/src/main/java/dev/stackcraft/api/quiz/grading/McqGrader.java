package dev.stackcraft.api.quiz.grading;

import dev.stackcraft.api.quiz.domain.Answer;
import dev.stackcraft.api.quiz.domain.GradeResult;
import dev.stackcraft.api.quiz.domain.Question.Mcq;
import org.springframework.stereotype.Component;

@Component
public class McqGrader implements Grader<Mcq> {

    @Override
    public Class<Mcq> supports() {
        return Mcq.class;
    }

    @Override
    public GradeResult grade(Mcq q, Answer a) {
        boolean correct = a.choice() != null && a.choice() == q.answer();
        String feedback = correct ? "Correct." : "Not quite. The answer is: " + q.options().get(q.answer());
        return new GradeResult(correct, correct ? 1.0 : 0.0, feedback, q.explanation());
    }
}
