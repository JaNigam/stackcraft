package dev.stackcraft.api.quiz.grading;

import dev.stackcraft.api.quiz.domain.Answer;
import dev.stackcraft.api.quiz.domain.Question;
import dev.stackcraft.api.quiz.domain.Tier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GraderBehaviourTest {

    @Test
    void predictOutputIgnoresLineEndingsAndTrailingSpacesButNotContent() {
        var q = new Question.PredictOutput("p", Tier.INTERVIEW, "p", "c", "[a, b]\n{x=1}\n", "", 4);
        var g = new PredictOutputGrader();

        assertThat(g.grade(q, Answer.text("  [a, b]  \r\n{x=1}   \r\n\r\n")).correct()).isTrue(); // blank edges ignored
        assertThat(g.grade(q, Answer.text("[a, b]\n  {x=1}")).correct()).isFalse();             // inner indentation counts
        assertThat(g.grade(q, Answer.text("[b, a]\n{x=1}")).correct()).isFalse();
    }

    @Test
    void eachProblemHintCostsTwentyPercentWithAFloor() {
        var q = new Question.Problem("k", Tier.KILLER, "p", List.of("1", "2", "3"), "m", 8);
        var g = new ProblemGrader();

        assertThat(g.grade(q, Answer.selfScored(2, 0)).score()).isEqualTo(1.0);
        assertThat(g.grade(q, Answer.selfScored(2, 1)).score()).isEqualTo(0.8);
        assertThat(g.grade(q, Answer.selfScored(2, 3)).score()).isEqualTo(0.4, org.assertj.core.data.Offset.offset(1e-9));
        assertThat(g.grade(q, Answer.selfScored(1, 0)).score()).isEqualTo(0.5);
    }

    @Test
    void registryRejectsAMissingGrader() {
        assertThatThrownBy(() -> new GraderRegistry(List.of(new McqGrader())))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("No grader");
    }

    @Test
    void registryRejectsTwoGradersForOneType() {
        assertThatThrownBy(() -> new GraderRegistry(List.of(new McqGrader(), new McqGrader())))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Two graders");
    }

    @Test
    void registryDispatchesByRuntimeType() {
        var registry = new GraderRegistry(List.of(new McqGrader(), new PredictOutputGrader(), new ShortAnswerGrader(), new ProblemGrader()));
        Question q = new Question.Mcq("m", Tier.WARMUP, "p", null, List.of("a", "b"), 0, "", 2);

        assertThat(registry.grade(q, Answer.choice(0)).correct()).isTrue();
        assertThat(registry.grade(q, null).correct()).isFalse();
    }
}
