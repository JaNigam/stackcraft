package dev.stackcraft.api.quiz.grading;

import dev.stackcraft.api.quiz.domain.Answer;
import dev.stackcraft.api.quiz.domain.GradeResult;
import dev.stackcraft.api.quiz.domain.Question;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry: finds the grader for a question's runtime type. Spring passes in every {@link Grader} bean, so a new
 * question type only needs a new grader class (OCP). Fails at start-up, not at request time, if a type has no
 * grader or two graders.
 */
public class GraderRegistry {

    private final Map<Class<? extends Question>, Grader<? extends Question>> byType = new HashMap<>();

    public GraderRegistry(List<Grader<? extends Question>> graders) {
        for (Grader<? extends Question> g : graders) {
            Grader<? extends Question> previous = byType.put(g.supports(), g);
            if (previous != null) {
                throw new IllegalStateException("Two graders for " + g.supports().getSimpleName());
            }
        }
        for (Class<?> type : Question.class.getPermittedSubclasses()) {
            if (!byType.containsKey(type)) {
                throw new IllegalStateException("No grader for question type " + type.getSimpleName());
            }
        }
    }

    public GradeResult grade(Question question, Answer answer) {
        return gradeTyped(question, answer == null ? new Answer(null, null, null, null) : answer);
    }

    @SuppressWarnings("unchecked")
    private <T extends Question> GradeResult gradeTyped(T question, Answer answer) {
        Grader<T> grader = (Grader<T>) byType.get(question.getClass());
        return grader.grade(question, answer);
    }
}
