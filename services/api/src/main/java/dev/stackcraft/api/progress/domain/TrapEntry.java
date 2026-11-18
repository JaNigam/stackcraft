package dev.stackcraft.api.progress.domain;

import java.time.Instant;

/**
 * A gotcha the learner fell for. It stays on the Trap Board (and comes back in quizzes) until answered correctly.
 * Immutable: {@link #fallenAgain} and {@link #markCleared} return new values.
 */
public record TrapEntry(String learnerId, String chapterId, String itemId, String kind, String prompt,
                        int timesFallen, Instant lastFallenAt, boolean cleared) {

    public static TrapEntry firstFall(String learnerId, String chapterId, String itemId, String kind, String prompt, Instant at) {
        return new TrapEntry(learnerId, chapterId, itemId, kind, prompt, 1, at, false);
    }

    public TrapEntry fallenAgain(Instant at) {
        return new TrapEntry(learnerId, chapterId, itemId, kind, prompt, timesFallen + 1, at, false);
    }

    public TrapEntry markCleared() {
        return new TrapEntry(learnerId, chapterId, itemId, kind, prompt, timesFallen, lastFallenAt, true);
    }
}
