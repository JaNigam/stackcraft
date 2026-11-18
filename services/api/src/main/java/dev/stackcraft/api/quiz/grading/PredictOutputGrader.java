package dev.stackcraft.api.quiz.grading;

import dev.stackcraft.api.quiz.domain.Answer;
import dev.stackcraft.api.quiz.domain.GradeResult;
import dev.stackcraft.api.quiz.domain.Question.PredictOutput;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Compares typed output to the verified output. Whitespace is normalised (line endings, trailing spaces, blank
 * edges) because nobody fails an interview for a trailing space; everything else must match exactly.
 */
@Component
public class PredictOutputGrader implements Grader<PredictOutput> {

    @Override
    public Class<PredictOutput> supports() {
        return PredictOutput.class;
    }

    @Override
    public GradeResult grade(PredictOutput q, Answer a) {
        boolean correct = a.text() != null && normalise(a.text()).equals(normalise(q.expected()));
        String feedback = correct ? "Exactly right." : "The program actually prints:\n" + q.expected().strip();
        return new GradeResult(correct, correct ? 1.0 : 0.0, feedback, q.explanation());
    }

    static String normalise(String s) {
        return Arrays.stream(s.replace("\r\n", "\n").replace('\r', '\n').split("\n"))
                .map(String::stripTrailing)
                .collect(Collectors.joining("\n"))
                .strip();
    }
}
