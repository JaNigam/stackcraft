package dev.stackcraft.api.progress.application;

import dev.stackcraft.api.content.domain.Chapter;
import dev.stackcraft.api.content.port.ContentSource;
import dev.stackcraft.api.progress.application.AwardXpUseCase.XpAward;
import dev.stackcraft.api.progress.application.AwardXpUseCase.XpClaim;
import dev.stackcraft.api.progress.domain.FlashcardState;
import dev.stackcraft.api.progress.domain.TrapEntry;
import dev.stackcraft.api.progress.domain.XpKind;
import dev.stackcraft.api.progress.port.ProgressReader;
import dev.stackcraft.api.progress.port.ProgressWriter;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Brings a guest's browser progress into their new account, once.
 *
 * <p>Trust model (ADR 0004): the client's XP numbers are ignored. Every event is re-awarded through
 * {@link AwardXpUseCase}: max XP comes from the content, attempt ids dedupe (so a second import is a no-op), and the
 * no-farming rule still caps each item. Unknown items, future timestamps and anything older than 400 days are skipped.
 * The one thing we must trust is the browser-graded score, which is bounded by the content's XP.
 */
public class ImportGuestProgressUseCase {

    public static final int MAX_EVENTS = 2000;
    private static final Duration MAX_AGE = Duration.ofDays(400);
    private static final Duration CLOCK_SKEW = Duration.ofMinutes(5);
    private static final Pattern FLASHCARD_ITEM = Pattern.compile("^(.+)@(\\d{4}-\\d{2}-\\d{2})$");

    private final ContentSource content;
    private final AwardXpUseCase awardXp;
    private final ProgressReader reader;
    private final ProgressWriter writer;
    private final Clock clock;

    public ImportGuestProgressUseCase(ContentSource content, AwardXpUseCase awardXp, ProgressReader reader,
                                      ProgressWriter writer, Clock clock) {
        this.content = content;
        this.awardXp = awardXp;
        this.reader = reader;
        this.writer = writer;
        this.clock = clock;
    }

    public ImportResult importGuest(String learnerId, GuestProgress guest) {
        if (guest.events().size() > MAX_EVENTS) throw new IllegalArgumentException("At most " + MAX_EVENTS + " events per import");
        Instant now = clock.instant();
        int imported = 0;
        int skipped = 0;
        int xp = 0;

        for (GuestEvent e : guest.events().stream().sorted(Comparator.comparing(GuestEvent::at, Comparator.nullsLast(Comparator.naturalOrder()))).toList()) {
            Optional<Integer> maxXp = e.at() == null || e.at().isAfter(now.plus(CLOCK_SKEW)) || e.at().isBefore(now.minus(MAX_AGE))
                    ? Optional.empty() : maxXpFor(e);
            if (maxXp.isEmpty()) {
                skipped++;
                continue;
            }
            double score = Math.max(0, Math.min(1, e.score()));
            try {
                XpAward award = awardXp.award(new XpClaim(e.attemptId(), learnerId, e.kind(), e.chapterId(), e.itemId(), maxXp.get(), score), e.at());
                if (award.duplicate()) {
                    skipped++;
                } else {
                    imported++;
                    xp += award.xpAwarded();
                }
            } catch (IllegalArgumentException badAttemptId) {
                skipped++;
            }
        }

        for (GuestTrap t : guest.traps()) {
            Optional<Chapter> chapter = content.chapter(t.chapterId());
            boolean known = chapter.isPresent() && (chapter.get().question(t.itemId()).isPresent() || chapter.get().teaser(t.itemId()).isPresent());
            if (known && reader.trap(learnerId, t.chapterId(), t.itemId()).isEmpty() && t.lastFallenAt() != null) {
                String kind = "teaser".equals(t.kind()) ? "teaser" : "question";
                String prompt = t.prompt() == null ? "" : t.prompt().substring(0, Math.min(300, t.prompt().length()));
                writer.saveTrap(new TrapEntry(learnerId, t.chapterId(), t.itemId(), kind, prompt,
                        Math.max(1, Math.min(t.timesFallen(), 99)), t.lastFallenAt(), false));
            }
        }

        for (GuestCard c : guest.cards()) {
            int slash = c.key() == null ? -1 : c.key().indexOf('/');
            if (slash <= 0 || c.dueOn() == null) continue;
            String chapterId = c.key().substring(0, slash);
            String cardId = c.key().substring(slash + 1);
            boolean known = content.chapter(chapterId).map(ch -> ch.flashcards().stream().anyMatch(f -> f.id().equals(cardId))).orElse(false);
            if (known && reader.flashcard(learnerId, c.key()).isEmpty()) {
                writer.saveFlashcard(new FlashcardState(learnerId, c.key(), Math.max(1, Math.min(5, c.box())), c.dueOn()));
            }
        }
        return new ImportResult(imported, skipped, xp);
    }

    /** XP the content says the item is worth; empty if the item does not exist. */
    private Optional<Integer> maxXpFor(GuestEvent e) {
        if (e.kind() == null || e.chapterId() == null || e.itemId() == null) return Optional.empty();
        Optional<Chapter> chapter = content.chapter(e.chapterId());
        if (chapter.isEmpty()) return Optional.empty();
        Chapter c = chapter.get();
        return switch (e.kind()) {
            case ANSWER -> c.question(e.itemId()).map(q -> q.xp());
            case TEASER -> c.teaser(e.itemId()).map(t -> t.xp());
            case CHAPTER -> "complete".equals(e.itemId()) ? Optional.of(c.meta().completionXp()) : Optional.empty();
            case FLASHCARD -> {
                var m = FLASHCARD_ITEM.matcher(e.itemId());
                yield m.matches() && c.flashcards().stream().anyMatch(f -> f.id().equals(m.group(1))) ? Optional.of(1) : Optional.empty();
            }
        };
    }

    public record GuestProgress(List<GuestEvent> events, List<GuestTrap> traps, List<GuestCard> cards) {
        public GuestProgress {
            events = events == null ? List.of() : List.copyOf(events);
            traps = traps == null ? List.of() : List.copyOf(traps);
            cards = cards == null ? List.of() : List.copyOf(cards);
        }
    }

    public record GuestEvent(String attemptId, XpKind kind, String chapterId, String itemId, double score, Instant at) {}

    public record GuestTrap(String chapterId, String itemId, String kind, String prompt, int timesFallen, Instant lastFallenAt) {}

    public record GuestCard(String key, int box, LocalDate dueOn) {}

    public record ImportResult(int eventsImported, int eventsSkipped, int xpAwarded) {}
}
