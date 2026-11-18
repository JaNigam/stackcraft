package dev.stackcraft.api.content.domain;

import dev.stackcraft.api.quiz.domain.Question;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * One lesson: metadata plus the three sections. Theory is MDX rendered by the web app; the api owns the parts that
 * are graded or scheduled (teasers, questions, flashcards). Answers are not secret: the content is in the public repo.
 */
public record Chapter(ChapterMeta meta, List<Teaser> teasers, List<Question> questions, List<Flashcard> flashcards) {

    public Chapter {
        teasers = List.copyOf(teasers);
        questions = List.copyOf(questions);
        flashcards = List.copyOf(flashcards);
        requireUniqueIds(meta.id(), teasers.stream().map(Teaser::id).toList(), "teaser");
        requireUniqueIds(meta.id(), questions.stream().map(Question::id).toList(), "question");
        requireUniqueIds(meta.id(), flashcards.stream().map(Flashcard::id).toList(), "flashcard");
    }

    public String id() {
        return meta.id();
    }

    public Optional<Question> question(String questionId) {
        return questions.stream().filter(q -> q.id().equals(questionId)).findFirst();
    }

    public Optional<Teaser> teaser(String teaserId) {
        return teasers.stream().filter(t -> t.id().equals(teaserId)).findFirst();
    }

    private static void requireUniqueIds(String chapterId, List<String> ids, String kind) {
        Set<String> seen = new HashSet<>();
        for (String id : ids) {
            if (!seen.add(id)) throw new IllegalArgumentException(chapterId + ": duplicate " + kind + " id '" + id + "'");
        }
    }
}
