package dev.stackcraft.api.progress.domain;

import java.util.Optional;

/** Course ranks. Thresholds are the course's: Trainee 0, Developer 200, Senior 500, Staff 900, Principal 1300. */
public enum Rank {
    TRAINEE("Trainee", 0),
    DEVELOPER("Developer", 200),
    SENIOR("Senior", 500),
    STAFF("Staff", 900),
    PRINCIPAL("Principal", 1300);

    private final String label;
    private final int minXp;

    Rank(String label, int minXp) {
        this.label = label;
        this.minXp = minXp;
    }

    public String label() { return label; }

    public int minXp() { return minXp; }

    public static Rank of(int xp) {
        Rank result = TRAINEE;
        for (Rank r : values()) {
            if (xp >= r.minXp) result = r;
        }
        return result;
    }

    public Optional<Rank> next() {
        return ordinal() + 1 < values().length ? Optional.of(values()[ordinal() + 1]) : Optional.empty();
    }
}
