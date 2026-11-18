package dev.stackcraft.api.quiz.domain;

import java.util.List;

/**
 * A question in a chapter's bank. Sealed: the compiler knows every variant, so a {@code switch} over questions is
 * exhaustive and adding a type is a deliberate act (new record + new {@code Grader}; nothing else changes: OCP).
 */
public sealed interface Question permits Question.Mcq, Question.PredictOutput, Question.ShortAnswer, Question.Problem {

    String id();

    Tier tier();

    String prompt();

    /** XP for a perfect answer. Defaults to the tier's XP when the content file does not set it. */
    int xp();

    /** Multiple choice: exactly one correct option, auto-graded. */
    record Mcq(String id, Tier tier, String prompt, String code, List<String> options, int answer,
               String explanation, int xp) implements Question {
        public Mcq {
            options = List.copyOf(options);
            if (options.size() < 2) throw new IllegalArgumentException(id + ": an MCQ needs at least 2 options");
            if (answer < 0 || answer >= options.size()) throw new IllegalArgumentException(id + ": answer index out of range");
        }
    }

    /** "What does this print?": the learner types the output; compared after normalising whitespace. */
    record PredictOutput(String id, Tier tier, String prompt, String code, String expected, String explanation,
                         int xp) implements Question {}

    /** Open answer with a model answer and key points; the learner self-scores 0, 1 or 2. */
    record ShortAnswer(String id, Tier tier, String prompt, String modelAnswer, List<String> keyPoints,
                       int xp) implements Question {
        public ShortAnswer {
            keyPoints = List.copyOf(keyPoints);
        }
    }

    /** Killer problem: three hint levels and a model answer; every hint used lowers the XP. */
    record Problem(String id, Tier tier, String prompt, List<String> hints, String modelAnswer,
                   int xp) implements Question {
        public Problem {
            hints = List.copyOf(hints);
        }
    }
}
