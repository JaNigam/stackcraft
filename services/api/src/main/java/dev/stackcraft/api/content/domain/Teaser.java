package dev.stackcraft.api.content.domain;

import java.util.List;

/**
 * "What happens if...": the learner commits to an option before the reveal. A wrong commit goes to the Trap Board.
 */
public record Teaser(String id, String prompt, String code, List<String> options, int answer, String reveal, int xp) {
    public Teaser {
        options = List.copyOf(options);
        if (answer < 0 || answer >= options.size()) throw new IllegalArgumentException(id + ": teaser answer out of range");
    }

    public boolean isCorrect(int choice) {
        return choice == answer;
    }
}
