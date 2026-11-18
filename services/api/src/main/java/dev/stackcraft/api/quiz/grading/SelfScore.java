package dev.stackcraft.api.quiz.grading;

/** Shared rules for self-scored answers. */
final class SelfScore {
    private SelfScore() {}

    static int clamp(Integer selfScore) {
        return selfScore == null ? 0 : Math.max(0, Math.min(2, selfScore));
    }

    static String feedback(int self) {
        return switch (self) {
            case 2 -> "Nailed it.";
            case 1 -> "Partly there: re-read the key points.";
            default -> "Missed it: this one goes on your Trap Board.";
        };
    }
}
