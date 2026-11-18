package dev.stackcraft.api.progress.adapter.web;

import dev.stackcraft.api.progress.domain.LearnerProgress;

import java.util.Map;
import java.util.Set;

/** Wire shape of {@link LearnerProgress}; ranks go out as id + label so the UI never hard-codes thresholds. */
public record ProgressView(String learnerId, int totalXp, String rank, String rankLabel, int rankMinXp,
                           String nextRank, String nextRankLabel, Integer nextRankMinXp, int xpToNextRank,
                           int streakDays, Set<String> completedChapters, Map<String, Double> bestScores) {

    public static ProgressView of(LearnerProgress p) {
        return new ProgressView(p.learnerId(), p.totalXp(), p.rank().name(), p.rank().label(), p.rank().minXp(),
                p.nextRank() == null ? null : p.nextRank().name(),
                p.nextRank() == null ? null : p.nextRank().label(),
                p.nextRank() == null ? null : p.nextRank().minXp(),
                p.xpToNextRank(), p.streakDays(), p.completedChapters(), p.bestScores());
    }
}
