package dev.stackcraft.api.progress.domain;

import java.time.LocalDate;

/**
 * Leitner-box spaced repetition. Box 1 comes back tomorrow, box 5 in 16 days. Remembered moves a card up one box;
 * forgotten sends it back to box 1. Simple, explainable, and good enough (SM-2 can replace it behind the same API).
 */
public record FlashcardState(String learnerId, String cardKey, int box, LocalDate dueOn) {

    static final int[] INTERVAL_DAYS = {1, 2, 4, 8, 16};

    public FlashcardState {
        if (box < 1 || box > INTERVAL_DAYS.length) throw new IllegalArgumentException("box must be 1..5");
    }

    public static FlashcardState fresh(String learnerId, String cardKey, LocalDate today) {
        return new FlashcardState(learnerId, cardKey, 1, today);
    }

    public FlashcardState review(boolean remembered, LocalDate today) {
        int newBox = remembered ? Math.min(box + 1, INTERVAL_DAYS.length) : 1;
        return new FlashcardState(learnerId, cardKey, newBox, today.plusDays(INTERVAL_DAYS[newBox - 1]));
    }

    public boolean isDue(LocalDate today) {
        return !dueOn.isAfter(today);
    }
}
