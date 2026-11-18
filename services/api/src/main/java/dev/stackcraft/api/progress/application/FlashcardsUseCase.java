package dev.stackcraft.api.progress.application;

import dev.stackcraft.api.content.domain.Chapter;
import dev.stackcraft.api.content.domain.Flashcard;
import dev.stackcraft.api.content.port.ContentSource;
import dev.stackcraft.api.progress.application.AwardXpUseCase.XpAward;
import dev.stackcraft.api.progress.application.AwardXpUseCase.XpClaim;
import dev.stackcraft.api.progress.domain.FlashcardState;
import dev.stackcraft.api.progress.domain.XpKind;
import dev.stackcraft.api.progress.port.ProgressReader;
import dev.stackcraft.api.progress.port.ProgressWriter;
import dev.stackcraft.api.shared.NotFoundException;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The daily warm-up: due cards come only from chapters you have completed, at most {@code limit} at a time, oldest
 * due first. Reviewing moves the card between Leitner boxes; a remembered card earns 1 XP once per card per day.
 */
public class FlashcardsUseCase {

    private final ContentSource content;
    private final ProgressReader reader;
    private final ProgressWriter writer;
    private final AwardXpUseCase awardXp;
    private final Clock clock;

    public FlashcardsUseCase(ContentSource content, ProgressReader reader, ProgressWriter writer,
                             AwardXpUseCase awardXp, Clock clock) {
        this.content = content;
        this.reader = reader;
        this.writer = writer;
        this.awardXp = awardXp;
        this.clock = clock;
    }

    public List<DueCard> due(String learnerId, int limit) {
        LocalDate today = LocalDate.now(clock);
        Set<String> completed = reader.events(learnerId).stream()
                .filter(e -> e.kind() == XpKind.CHAPTER).map(e -> e.chapterId()).collect(Collectors.toSet());
        Map<String, FlashcardState> states = reader.flashcards(learnerId).stream()
                .collect(Collectors.toMap(FlashcardState::cardKey, Function.identity()));

        List<DueCard> due = new ArrayList<>();
        for (Chapter chapter : content.allChapters()) {
            if (!completed.contains(chapter.id())) continue;
            for (Flashcard card : chapter.flashcards()) {
                String key = key(chapter.id(), card.id());
                FlashcardState state = states.getOrDefault(key, FlashcardState.fresh(learnerId, key, today));
                if (state.isDue(today)) {
                    due.add(new DueCard(key, chapter.id(), chapter.meta().title(), card.front(), card.back(), state.box(), state.dueOn()));
                }
            }
        }
        return due.stream().sorted((a, b) -> a.dueOn().compareTo(b.dueOn())).limit(limit).toList();
    }

    public Review review(String learnerId, String cardKey, boolean remembered, String attemptId) {
        Flashcard card = find(cardKey);
        LocalDate today = LocalDate.now(clock);
        XpAward award = awardXp.award(new XpClaim(attemptId, learnerId, XpKind.FLASHCARD, chapterOf(cardKey),
                card.id() + "@" + today, 1, remembered ? 1.0 : 0.0));
        FlashcardState current = reader.flashcard(learnerId, cardKey).orElse(FlashcardState.fresh(learnerId, cardKey, today));
        if (award.duplicate()) {
            return new Review(current.box(), current.dueOn(), award.xpAwarded(), true);
        }
        FlashcardState next = current.review(remembered, today);
        writer.saveFlashcard(next);
        return new Review(next.box(), next.dueOn(), award.xpAwarded(), false);
    }

    private Flashcard find(String cardKey) {
        String chapterId = chapterOf(cardKey);
        String cardId = cardKey.substring(cardKey.indexOf('/') + 1);
        return content.chapter(chapterId).flatMap(c -> c.flashcards().stream().filter(f -> f.id().equals(cardId)).findFirst())
                .orElseThrow(() -> new NotFoundException("Flashcard", cardKey));
    }

    private static String chapterOf(String cardKey) {
        int slash = cardKey.indexOf('/');
        if (slash <= 0) throw new NotFoundException("Flashcard", cardKey);
        return cardKey.substring(0, slash);
    }

    public static String key(String chapterId, String cardId) {
        return chapterId + "/" + cardId;
    }

    public record DueCard(String key, String chapterId, String chapterTitle, String front, String back, int box, LocalDate dueOn) {}

    public record Review(int box, LocalDate dueOn, int xpAwarded, boolean duplicate) {}
}
