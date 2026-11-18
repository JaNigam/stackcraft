package dev.stackcraft.api.quiz.grading;

import dev.stackcraft.api.quiz.domain.Answer;
import dev.stackcraft.api.quiz.domain.GradeResult;
import dev.stackcraft.api.quiz.domain.Question;
import dev.stackcraft.api.quiz.domain.Tier;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * The Liskov contract, checked once for every grader. A new grader is added to {@link #graders()} and must pass
 * all of these unchanged; {@link #everyQuestionTypeIsCovered} fails if someone forgets.
 */
class GraderContractTest {

    /** grader, a sample question, an answer that is fully right for it. */
    static Stream<Arguments> graders() {
        return Stream.of(
                Arguments.of(new McqGrader(),
                        new Question.Mcq("m", Tier.WARMUP, "p", null, List.of("a", "b"), 1, "why", 2), Answer.choice(1)),
                Arguments.of(new PredictOutputGrader(),
                        new Question.PredictOutput("p", Tier.INTERVIEW, "p", "code", "33", "why", 4), Answer.text("33")),
                Arguments.of(new ShortAnswerGrader(),
                        new Question.ShortAnswer("s", Tier.INTERVIEW, "p", "model", List.of("k"), 4), Answer.selfScored(2, 0)),
                Arguments.of(new ProblemGrader(),
                        new Question.Problem("k", Tier.KILLER, "p", List.of("h1", "h2", "h3"), "model", 8), Answer.selfScored(2, 0)));
    }

    static List<Answer> hostileAnswers() {
        return List.of(new Answer(null, null, null, null), new Answer(-1, "", -5, -3), new Answer(99, "\u0000\n\r ", 99, 99),
                new Answer(Integer.MAX_VALUE, "x".repeat(10_000), Integer.MIN_VALUE, Integer.MAX_VALUE));
    }

    @ParameterizedTest
    @MethodSource("graders")
    <T extends Question> void neverThrowsAndScoreStaysInRange(Grader<T> grader, T question, Answer ignored) {
        for (Answer hostile : hostileAnswers()) {
            assertThatCode(() -> grader.grade(question, hostile)).doesNotThrowAnyException();
            GradeResult r = grader.grade(question, hostile);
            assertThat(r.score()).isBetween(0.0, 1.0);
            assertThat(r.feedback()).isNotNull();
        }
    }

    @ParameterizedTest
    @MethodSource("graders")
    <T extends Question> void rightAnswerIsCorrectWithFullScore(Grader<T> grader, T question, Answer right) {
        GradeResult r = grader.grade(question, right);
        assertThat(r.correct()).isTrue();
        assertThat(r.score()).isEqualTo(1.0);
    }

    @ParameterizedTest
    @MethodSource("graders")
    <T extends Question> void emptyAnswerIsWrongWithZeroScore(Grader<T> grader, T question, Answer ignored) {
        GradeResult r = grader.grade(question, new Answer(null, null, null, null));
        assertThat(r.correct()).isFalse();
        assertThat(r.score()).isZero();
    }

    @ParameterizedTest
    @MethodSource("graders")
    <T extends Question> void isPure(Grader<T> grader, T question, Answer right) {
        assertThat(grader.grade(question, right)).isEqualTo(grader.grade(question, right));
    }

    @ParameterizedTest
    @MethodSource("graders")
    <T extends Question> void supportsMatchesTheQuestionItGrades(Grader<T> grader, T question, Answer ignored) {
        assertThat(grader.supports()).isEqualTo(question.getClass());
    }

    @org.junit.jupiter.api.Test
    void everyQuestionTypeIsCovered() {
        List<String> covered = graders().map(a -> a.get()[1].getClass().getName()).toList();
        List<String> permitted = Arrays.stream(Question.class.getPermittedSubclasses()).map(Class::getName).toList();
        assertThat(covered).containsExactlyInAnyOrderElementsOf(permitted);
    }
}
