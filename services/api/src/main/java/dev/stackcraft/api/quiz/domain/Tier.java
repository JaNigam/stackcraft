package dev.stackcraft.api.quiz.domain;

/** Practice tiers from the course: warm-up, interview, killer. Each has a default XP. */
public enum Tier {
    WARMUP(2),
    INTERVIEW(4),
    KILLER(8);

    private final int defaultXp;

    Tier(int defaultXp) {
        this.defaultXp = defaultXp;
    }

    public int defaultXp() {
        return defaultXp;
    }
}
