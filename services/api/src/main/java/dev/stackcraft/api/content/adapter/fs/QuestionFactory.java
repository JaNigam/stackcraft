package dev.stackcraft.api.content.adapter.fs;

import dev.stackcraft.api.content.adapter.fs.YamlModels.QuestionFile;
import dev.stackcraft.api.quiz.domain.Question;
import dev.stackcraft.api.quiz.domain.Tier;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * Factory: turns one YAML question into the right {@link Question} record. Each type has its own small builder in a
 * map; a new type adds one entry here and one grader, and nothing else.
 */
final class QuestionFactory {

    private static final Map<String, Function<QuestionFile, Question>> BUILDERS = Map.of(
            "mcq", q -> new Question.Mcq(q.id(), tier(q), required(q.prompt(), q, "prompt"), q.code(),
                    list(q.options()), required(q.answer(), q, "answer"), q.explanation(), xp(q)),
            "predict", q -> new Question.PredictOutput(q.id(), tier(q), orDefault(q.prompt(), "What does this print?"),
                    required(q.code(), q, "code"), required(q.expected(), q, "expected"), q.explanation(), xp(q)),
            "short", q -> new Question.ShortAnswer(q.id(), tier(q), required(q.prompt(), q, "prompt"),
                    required(q.modelAnswer(), q, "modelAnswer"), list(q.keyPoints()), xp(q)),
            "problem", q -> new Question.Problem(q.id(), tier(q), required(q.prompt(), q, "prompt"),
                    list(q.hints()), required(q.modelAnswer(), q, "modelAnswer"), xp(q)));

    private QuestionFactory() {}

    static Question create(QuestionFile q) {
        if (q.id() == null || q.id().isBlank()) throw new IllegalArgumentException("question without id");
        String type = q.type() == null ? "" : q.type().toLowerCase(Locale.ROOT);
        Function<QuestionFile, Question> builder = BUILDERS.get(type);
        if (builder == null) {
            throw new IllegalArgumentException(q.id() + ": unknown question type '" + q.type() + "', expected one of " + BUILDERS.keySet());
        }
        return builder.apply(q);
    }

    private static Tier tier(QuestionFile q) {
        if (q.tier() == null) throw new IllegalArgumentException(q.id() + ": missing tier");
        return Tier.valueOf(q.tier().toUpperCase(Locale.ROOT).replace("-", ""));
    }

    private static int xp(QuestionFile q) {
        return q.xp() != null ? q.xp() : tier(q).defaultXp();
    }

    private static <T> T required(T value, QuestionFile q, String field) {
        if (value == null) throw new IllegalArgumentException(q.id() + ": missing '" + field + "'");
        return value;
    }

    private static String orDefault(String value, String fallback) {
        return value == null ? fallback : value;
    }

    private static List<String> list(List<String> values) {
        return values == null ? List.of() : values;
    }
}
