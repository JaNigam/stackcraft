package dev.stackcraft.api.progress.application;

import dev.stackcraft.api.progress.domain.XpEvent;
import dev.stackcraft.api.progress.domain.XpKind;
import dev.stackcraft.api.progress.port.DuplicateAttemptException;
import dev.stackcraft.api.progress.port.ProgressReader;
import dev.stackcraft.api.progress.port.ProgressWriter;

import java.time.Clock;
import java.util.regex.Pattern;

/**
 * Awards XP exactly once per attempt, and at most the item's XP in total.
 *
 * <ol>
 *   <li><b>Idempotent retries.</b> Same {@code attemptId} again (a double click, a network retry) returns the first
 *       result with {@code duplicate=true} and awards nothing. Two identical requests racing are settled by the
 *       store's unique constraint ({@link DuplicateAttemptException}). Same idea as idempotency keys on payments.</li>
 *   <li><b>No farming.</b> A new attempt on an item only earns the difference between this score and what the item
 *       already paid out, so answering the same MCQ ten times is still worth one MCQ.</li>
 * </ol>
 */
public class AwardXpUseCase {

    private static final Pattern ATTEMPT_ID = Pattern.compile("[A-Za-z0-9-]{8,64}");

    private final ProgressReader reader;
    private final ProgressWriter writer;
    private final Clock clock;

    public AwardXpUseCase(ProgressReader reader, ProgressWriter writer, Clock clock) {
        this.reader = reader;
        this.writer = writer;
        this.clock = clock;
    }

    public XpAward award(XpClaim claim) {
        if (claim.attemptId() == null || !ATTEMPT_ID.matcher(claim.attemptId()).matches()) {
            throw new IllegalArgumentException("attemptId must be 8-64 characters of letters, digits or '-'");
        }
        var existing = reader.eventByAttempt(claim.attemptId());
        if (existing.isPresent()) return XpAward.duplicate(existing.get());

        int alreadyPaid = reader.events(claim.learnerId()).stream()
                .filter(e -> e.sameItem(claim.kind(), claim.chapterId(), claim.itemId()))
                .mapToInt(XpEvent::xp).sum();
        int earned = (int) Math.round(claim.maxXp() * claim.score());
        int awarded = Math.max(0, earned - alreadyPaid);

        XpEvent event = new XpEvent(claim.attemptId(), claim.learnerId(), claim.kind(), claim.chapterId(),
                claim.itemId(), awarded, claim.score(), clock.instant());
        try {
            writer.append(event);
        } catch (DuplicateAttemptException race) {
            return XpAward.duplicate(reader.eventByAttempt(claim.attemptId()).orElseThrow());
        }
        return new XpAward(awarded, false, event);
    }

    public record XpClaim(String attemptId, String learnerId, XpKind kind, String chapterId, String itemId,
                          int maxXp, double score) {}

    public record XpAward(int xpAwarded, boolean duplicate, XpEvent event) {
        static XpAward duplicate(XpEvent original) {
            return new XpAward(original.xp(), true, original);
        }
    }
}
