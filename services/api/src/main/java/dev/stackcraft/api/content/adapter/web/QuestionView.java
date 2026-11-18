package dev.stackcraft.api.content.adapter.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.stackcraft.api.quiz.domain.Question;

import java.util.List;
import java.util.Locale;

/**
 * Wire shape of a question: one flat object with a {@code type} discriminator ({@code mcq}, {@code predict},
 * {@code short}, {@code problem}). The exhaustive switch below fails to compile if a new sealed variant is added
 * without a mapping, which is exactly the reminder we want.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record QuestionView(String id, String type, String tier, String prompt, String code, List<String> options,
                           Integer answer, String expected, String explanation, String modelAnswer,
                           List<String> keyPoints, List<String> hints, int xp) {

    static QuestionView of(Question q) {
        String tier = q.tier().name().toLowerCase(Locale.ROOT);
        return switch (q) {
            case Question.Mcq m -> new QuestionView(m.id(), "mcq", tier, m.prompt(), m.code(), m.options(), m.answer(),
                    null, m.explanation(), null, null, null, m.xp());
            case Question.PredictOutput p -> new QuestionView(p.id(), "predict", tier, p.prompt(), p.code(), null, null,
                    p.expected(), p.explanation(), null, null, null, p.xp());
            case Question.ShortAnswer s -> new QuestionView(s.id(), "short", tier, s.prompt(), null, null, null, null,
                    null, s.modelAnswer(), s.keyPoints(), null, s.xp());
            case Question.Problem p -> new QuestionView(p.id(), "problem", tier, p.prompt(), null, null, null, null,
                    null, p.modelAnswer(), null, p.hints(), p.xp());
        };
    }
}
