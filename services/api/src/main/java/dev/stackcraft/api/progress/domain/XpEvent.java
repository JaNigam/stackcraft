package dev.stackcraft.api.progress.domain;

import java.time.Instant;

/**
 * One XP-bearing attempt. An append-only log: progress, rank and streak are all derived from these events, so they
 * can never drift out of sync. {@code attemptId} is client-generated and unique, which makes retries idempotent.
 */
public record XpEvent(String attemptId, String learnerId, XpKind kind, String chapterId, String itemId, int xp,
                      double score, Instant occurredAt) {
    public XpEvent {
        if (xp < 0) throw new IllegalArgumentException("xp must be >= 0");
    }

    public boolean sameItem(XpKind kind, String chapterId, String itemId) {
        return this.kind == kind && this.chapterId.equals(chapterId) && this.itemId.equals(itemId);
    }
}
