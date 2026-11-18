package dev.stackcraft.api.quiz.grading;

import dev.stackcraft.api.quiz.domain.Answer;
import dev.stackcraft.api.quiz.domain.GradeResult;
import dev.stackcraft.api.quiz.domain.Question;

/**
 * Strategy: grades one question type. The contract every implementation must honour (checked by
 * {@code GraderContractTest}, so any grader can stand in for any other: LSP):
 * <ul>
 *   <li>never throws for a malformed or empty answer; it returns a zero score instead;</li>
 *   <li>score is always within [0, 1];</li>
 *   <li>pure: the same question and answer always give the same result.</li>
 * </ul>
 */
public interface Grader<T extends Question> {

    Class<T> supports();

    GradeResult grade(T question, Answer answer);
}
